/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.insight.data.upgrade;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.ShellUtils;
import io.kylin.mdx.insight.core.dao.MdxInfoMapper;
import io.kylin.mdx.insight.core.dao.UserInfoMapper;
import io.kylin.mdx.insight.core.entity.MdxInfo;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.data.command.CommandLineExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import tk.mybatis.spring.annotation.MapperScan;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {"io.kylin.mdx.insight.data.upgrade"}
)
@MapperScan("io.kylin.mdx.insight.core.dao")
public class ConfAndMetadataUpgradeCommand implements CommandLineExecutor, ApplicationRunner {

    private static final String SP = File.separator;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MdxInfoMapper mdxInfoMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    private static final String VERSION_FILE_NAME = "VERSION";

    private static final String PROPERTIES_FILE = "insight.properties";

    private static final String OVERRIDE_PROPERTIES_FILE = "insight.override.properties";

    private static final String COPY_LOG_FLAG = "copyLog";

    private static final String LOG_DIRECTORY = "logs";

    private static final String SEMANTIC_DIRECTORY = "semantic-mdx";

    private static final String STARTUP_FILE = "startup.sh";

    private static final String SET_JVM_FILE = "set-jvm.sh";

    private static final String UNKNOWN = "UNKNOWN";

    private static final String ENABLE_VERSION = "1.3.0.1061-GA";

    private static final int ARGS_NAME = 4;

    private static final int MDX_VERSION_ID = 1;

    private static final int MDX_VERSION_LENGTH = 3;

    private static final String SHELL_SCRIPT_PATH = SEMANTIC_DIRECTORY + SP + "scripts" + SP + "upgrade_mdx.sh";

    private String[] versionArray;

    public ConfAndMetadataUpgradeCommand() {
    }

    public ConfAndMetadataUpgradeCommand(MdxInfoMapper mdxInfoMapper, UserInfoMapper userInfoMapper) {
        this.mdxInfoMapper = mdxInfoMapper;
        this.userInfoMapper = userInfoMapper;
    }

    @Override
    public void execute(String[] args) {
        new SpringApplicationBuilder()
                .web(WebApplicationType.NONE)
                .sources(ConfAndMetadataUpgradeCommand.class)
                .run(args);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.getSourceArgs().length < ARGS_NAME) {
            log.error("Please input old MDX directory.");
            return;
        }

        // update old config to current MDX directory
        String oldMdxPath = args.getSourceArgs()[1].replace(" ", "");
        String curMdxPath = args.getSourceArgs()[2];
        String copyLogFlag = args.getSourceArgs()[3];
        String databaseType = args.getSourceArgs()[4];

        String oldVersion = readOldVersion();

        Map<String, String> propertiesMap = getProperties(oldMdxPath);
        updateProperties(propertiesMap, curMdxPath, oldVersion);
        log.info("------------------------ update old config to current MDX directory success!---------------------");

        // copy old log file to current MDX directory
        if (COPY_LOG_FLAG.equals(copyLogFlag)) {
            copyLogDir(oldMdxPath, curMdxPath);
        }
        log.info("------------------------ copy old log file to current MDX directory success! ---------------------");

        // delete duplicate user
        if (clearDuplicateUsers()) {
            log.info("------------------------ delete duplicate user success! ---------------------");
        }

        /*
         * execute upgrade sql
         */
        initVersionArray(curMdxPath);
        String curVersion = getMdxVersion(curMdxPath);
        log.info("old MDX version is {}, current MDX version is {}", oldVersion, curVersion);
        upgradeSql(oldVersion, curMdxPath, databaseType);

        /*
         * upgrade mdx version in database
         */
        MdxInfo upgradeMdxInfo = mdxInfoMapper.selectByPrimaryKey(MDX_VERSION_ID);
        upgradeMdxInfo.setMdxVersion(curVersion);
        mdxInfoMapper.updateByPrimaryKey(upgradeMdxInfo);
        log.info("------------------------ execute upgrade sql success! ---------------------");

        // copy old jvm parameters to current MDX directory
        copyJvmParameters(oldMdxPath, curMdxPath);
        log.info("------------------------ copy old jvm parameters to current MDX directory success! ---------------------");

        /*
         * compress old MDX directory and delete it
         */
        if (oldMdxPath.endsWith("/")) {
            oldMdxPath = oldMdxPath.substring(0, oldMdxPath.length() - 1);
        }
        if (curMdxPath.endsWith("/")) {
            curMdxPath = curMdxPath.substring(0, curMdxPath.length() - 1);
        }
        tarOldMdxDirectory(oldMdxPath, curMdxPath);
        log.info("------------------------ upgrade MDX finished! ---------------------");
    }

    private String readOldVersion() {
        String oldVersion;
        MdxInfo mdxInfo = mdxInfoMapper.selectByPrimaryKey(MDX_VERSION_ID);
        oldVersion = mdxInfo.getMdxVersion();
        String[] versionArray = oldVersion.split("\\.");
        if (versionArray.length < MDX_VERSION_LENGTH) {
            log.error(" MDX version is error, please check it!");
        }
        if (versionArray[2].contains("-")) {
            versionArray[2] = versionArray[2].split("-")[0];
        }
        oldVersion = versionArray[0] + "." + versionArray[1] + "." + versionArray[2];
        oldVersion = oldVersion.trim()
                .replace("\n", "")
                .replace("\r", "");
        return oldVersion;
    }

    public Map<String, String> getProperties(String mdxPath) {
        String confPath = mdxPath + SP + "conf";
        Map<String, String> propertiesMap = new HashMap<>(128);
        File file = getPropertiesFile(confPath);
        try (InputStreamReader read = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(read);) {
            String lineText;
            while ((lineText = br.readLine()) != null) {
                if (lineText.startsWith("#") || !lineText.startsWith("insight") || !lineText.contains("=")) {
                    continue;
                }
                String[] confArray = lineText.split("=");
                if (confArray.length < 2) {
                    continue;
                }
                propertiesMap.put(confArray[0], confArray[1]);
            }
        } catch (IOException e) {
            throw new SemanticException(e);
        }
        return propertiesMap;
    }

    public void updateProperties(Map<String, String> propertiesMap, String curMdxPath, String oldVersion) {
        File file = getPropertiesFile(curMdxPath + SP + "conf");
        try {
            String content;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                content = generateProperties(propertiesMap, br, oldVersion);
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(content);
            }
        } catch (IOException e) {
            throw new SemanticException(e);
        }
    }

    public String generateProperties(Map<String, String> propertiesMap, BufferedReader br, String oldVersion)
            throws IOException {
        // 如果旧版本小于 1.3.0 GA，那么将 "insight.mdx.xmla.support-rowset-visibilities" 默认设置为 false
        boolean isDisableVersion = lessVersion(ENABLE_VERSION, oldVersion);
        String lineText;
        StringBuilder sb = new StringBuilder();
        while ((lineText = br.readLine()) != null) {
            if (!lineText.contains("insight") || lineText.startsWith("##") || !lineText.contains("=")) {
                sb.append(lineText).append("\n");
                continue;
            }
            String property = lineText;
            if (lineText.startsWith("#")) {
                property = lineText.substring(1);
            }
            String[] confArray = property.split("=");
            String propertyKey = confArray[0];
            if (propertiesMap.containsKey(propertyKey)) {
                lineText = propertyKey + "=" + propertiesMap.get(propertyKey);
                propertiesMap.remove(propertyKey);
            } else {
                if ("insight.mdx.xmla.support-rowset-visibilities".equals(propertyKey) && isDisableVersion) {
                    lineText = propertyKey + "=" + "false";
                }
            }
            sb.append(lineText).append("\n");
        }

        // 将旧版本中配置的未公开配置项复制到新版本中
        if (propertiesMap.size() > 0) {
            for (String propertiesKey : propertiesMap.keySet()) {
                lineText = propertiesKey + "=" + propertiesMap.get(propertiesKey);
                sb.append(lineText).append("\n");
            }
        }

        return sb.toString();
    }

    private void copyLogDir(String oldMdxPath, String curMdxPath) {
        String oldMdxLogDir = oldMdxPath + SP + LOG_DIRECTORY;
        File oldDir = new File(oldMdxLogDir);
        if (!oldDir.exists()) {
            return;
        }
        String copyLogCmd = "cp -r " + oldMdxLogDir + " " + curMdxPath;
        try {
            Runtime.getRuntime().exec(copyLogCmd);
        } catch (IOException e) {
            log.error("copy old config to current MDX directory failed!" + e);
        }
    }

    private void copyJvmParameters(String oldMdxPath, String curMdxPath) {
        String oldMdxSemanticDir = oldMdxPath + SP + SEMANTIC_DIRECTORY;
        String newMdxSemanticDir = curMdxPath + SP + SEMANTIC_DIRECTORY;
        File oldDir = new File(oldMdxSemanticDir);
        if (!oldDir.exists()) {
            log.error("Please check old MDX semantic path is normal");
            return;
        }
        if (!new File(newMdxSemanticDir).exists()) {
            throw new SemanticException("Please check current MDX semantic path is normal");
        }
        String jvmXms = "";
        String jvmXmx = "";
        // search jvm properties from startup.sh
        File startupFile = new File(oldMdxSemanticDir, STARTUP_FILE);
        try (InputStreamReader read = new InputStreamReader(
                new FileInputStream(startupFile), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(read);) {
            String lineText = null;
            while ((lineText = br.readLine()) != null) {
                if (lineText.contains("-Xms") && lineText.contains("-Xmx")) {
                    String[] parameters = lineText.split("-X");
                    if (parameters.length < 3) {
                        continue;
                    }
                    jvmXms = parameters[1].trim();
                    jvmXmx = parameters[2].trim();
                    break;
                }
            }
        } catch (IOException e) {
            throw new SemanticException(e);
        }
        if (!jvmXms.isEmpty()) {
            jvmXms = "-X" + jvmXms;
            jvmXmx = "-X" + jvmXmx;
        } else {
            File setJvmFile = new File(oldMdxSemanticDir, SET_JVM_FILE);
            try (InputStreamReader read = new InputStreamReader(
                    new FileInputStream(setJvmFile), StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(read);) {
                String lineText;
                while ((lineText = br.readLine()) != null) {
                    if (lineText.contains("-Xms")) {
                        jvmXms = lineText.split("=")[1];
                    }
                    if (lineText.contains("-Xmx")) {
                        jvmXmx = lineText.split("=")[1];
                    }
                }
            } catch (IOException e) {
                throw new SemanticException(e);
            }
        }

        File setJvmFile = new File(newMdxSemanticDir, SET_JVM_FILE);
        try (InputStreamReader read = new InputStreamReader(
                new FileInputStream(setJvmFile), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(read);) {
            String lineText;
            StringBuilder sb = new StringBuilder();
            while ((lineText = br.readLine()) != null) {
                if (lineText.contains("jvm_xms")) {
                    lineText = "jvm_xms" + "=" + jvmXms;
                    sb.append(lineText + "\n");
                    continue;
                }
                if (lineText.contains("jvm_xmx")) {
                    lineText = "jvm_xmx" + "=" + jvmXmx;
                    sb.append(lineText + "\n");
                    continue;
                }
                sb.append(lineText + "\n");
            }
            try (FileWriter fw = new FileWriter(setJvmFile.getPath());
                 BufferedWriter bw = new BufferedWriter(fw);) {
                bw.write(sb.toString());
            }
        } catch (IOException e) {
            throw new SemanticException(e);
        }
    }

    private void tarOldMdxDirectory(String oldMdxDirectory, String curMdxPath) {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dataTimeString = dateTime.format(formatter).trim().replace(" ", "_").replace(":", "_");
        String destFile = oldMdxDirectory + "_" + dataTimeString + ".tar.gz";
        String singleDirectory = oldMdxDirectory.split(SP)[oldMdxDirectory.split(SP).length - 1];
        destFile = destFile.substring(destFile.lastIndexOf('/') + 1);
        String scripts = "bash" + " " + curMdxPath + SP + SHELL_SCRIPT_PATH + " " + destFile + " " + oldMdxDirectory + " " + singleDirectory + " " + curMdxPath;
        Process pro = null;
        try {
            pro = ShellUtils.executeShell(scripts);
        } catch (IOException e) {
            log.error("compress old MDX directory and delete it failed!" + e);
        }
        int status = -1;
        try {
            status = ShellUtils.getPidStatus(pro);
        } catch (InterruptedException e) {
            log.error("------------------------ compress old MDX directory and delete it failed! ---------------------" + e);
        }
        if (status != 0) {
            log.error("------------------------ compress old MDX directory and delete it failed! ---------------------");
        } else {
            log.info("------------------------ compress old MDX directory and delete it success! ---------------------");
        }
    }

    private void upgradeSql(String oldVersion, String curMdxPath, String databaseType) {
        String sqlPath = curMdxPath + SP + SEMANTIC_DIRECTORY + SP + "sql" + SP + "alter_sql";
        int location = getVersionLocation(oldVersion);
        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
        for (int i = location + 1; i < versionArray.length; i++) {
            String sqlFile = "";
            if ("postgresql".equals(databaseType)) {
                sqlFile = sqlPath + SP + versionArray[i] + SP + "kylin_semantic_pg_alter-" + versionArray[i] + ".sql";
            } else {
                sqlFile = sqlPath + SP + versionArray[i] + SP + "kylin_semantic_mysql_alter-" + versionArray[i] + ".sql";
            }
            FileSystemResource sqlResource = new FileSystemResource(sqlFile);
            resourceDatabasePopulator.addScripts(sqlResource);
        }
        try {
            if (dataSource == null) {
                return;
            }
            resourceDatabasePopulator.execute(dataSource);
        } catch (Exception e) {
            log.error("******************* if you execute upgrade sql scripts repeatedly, this is normal! *****************  ", e);
        }
    }

    public String getMdxVersion(String mdxPath) {
        if (StringUtils.isBlank(mdxPath)) {
            return UNKNOWN;
        }

        File versionFile = new File(mdxPath, VERSION_FILE_NAME);
        if (!versionFile.exists()) {
            return UNKNOWN;
        }

        try (InputStream input = new FileInputStream(versionFile)) {
            String version = IOUtils.toString(input);
            String[] versionArray = version.split("\\.");
            if (versionArray.length < 3) {
                return UNKNOWN;
            }
            if (versionArray[2].contains("-")) {
                versionArray[2] = versionArray[2].split("-")[0];
            }
            version = versionArray[0] + "." + versionArray[1] + "." + versionArray[2];
            return version;
        } catch (IOException e) {
            return UNKNOWN;
        }
    }

    private File getPropertiesFile(String path) {
        File overrideFile = new File(path, OVERRIDE_PROPERTIES_FILE);
        if (overrideFile.exists()) {
            return overrideFile;
        } else {
            return new File(path, PROPERTIES_FILE);
        }
    }

    private int getVersionLocation(String version) {
        if (!validateVersion(version)) {
            return versionArray.length;
        }
        for (int i = 0; i < versionArray.length; i++) {
            if (version.equals(versionArray[i])) {
                return i;
            }
            if (lessVersion(versionArray[i], version)) {
                return i - 1;
            }
        }
        return versionArray.length;
    }

    private boolean validateVersion(String version) {
        String regex = "^[0-9]+\\.[0-9]+\\.[0-9]+$";
        return Pattern.matches(regex, version);
    }

    public static boolean lessVersion(String baseVersion, String compareVersion) {
        String[] baseNumArray = baseVersion.split("\\.");
        String[] compareNumArray = compareVersion.split("\\.");
        int len = Math.min(baseNumArray.length, compareNumArray.length);
        for (int i = 0; i < len; i++) {
            if (Integer.parseInt(baseNumArray[i]) < Integer.parseInt(compareNumArray[i])) {
                return false;
            }
            if (Integer.parseInt(baseNumArray[i]) > Integer.parseInt(compareNumArray[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * 初始化 MDX 版本列表
     *
     * @param curMdxPath
     */
    private void initVersionArray(String curMdxPath) {
        String sqlPath = curMdxPath + SP + SEMANTIC_DIRECTORY + SP + "sql" + SP + "alter_sql";
        File sqlDirectory = new File(sqlPath);
        File[] lists = sqlDirectory.listFiles();
        versionArray = new String[lists.length];
        for (int i = 0; i < lists.length; i++) {
            String fileName = lists[i].getName();
            versionArray[i] = fileName;
        }
        Arrays.sort(versionArray, ConfAndMetadataUpgradeCommand::versionCompare);
    }

    public static int versionCompare(String o1, String o2) {
        String[] baseNumArray = o1.split("\\.");
        String[] compareNumArray = o2.split("\\.");
        int len = Math.min(baseNumArray.length, compareNumArray.length);
        for (int i = 0; i < len; i++) {
            if (Integer.parseInt(baseNumArray[i]) < Integer.parseInt(compareNumArray[i])) {
                return -1;
            }
            if (Integer.parseInt(baseNumArray[i]) > Integer.parseInt(compareNumArray[i])) {
                return 1;
            }
        }
        return -1;
    }

    private boolean clearDuplicateUsers() {
        List<UserInfo> userInfos = userInfoMapper.selectAll();
        Set<String> usernames = new HashSet<>(userInfos.size());
        List<Integer> duplicateUserId = new LinkedList<>();
        for (UserInfo userInfo : userInfos) {
            if (usernames.contains(userInfo.getUsername().toUpperCase())) {
                duplicateUserId.add(userInfo.getId());
            } else {
                usernames.add(userInfo.getUsername().toUpperCase());
            }
        }
        if (!duplicateUserId.isEmpty()) {
            userInfoMapper.deleteUsersById(duplicateUserId);
            return true;
        } else {
            return false;
        }
    }

}
