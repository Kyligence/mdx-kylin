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


package io.kylin.mdx.insight.core.support;

import io.kylin.mdx.insight.common.MdxContext;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.ShellUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.dao.MdxInfoMapper;
import io.kylin.mdx.insight.core.entity.*;
import io.kylin.mdx.insight.core.manager.LicenseManager;
import io.kylin.mdx.insight.core.service.DiagnosisService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class PackageThread extends Thread {

    private final SimpleDateFormat conditionDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String startAt;

    private String endAt;

    private String shell;

    private String scriptPath;

    private String queryTime;

    private String ip;

    private String port;

    private String semanticMdxPath;

    private LicenseManager licenseManager;

    private Map<String, String> mapDatasetName2Json;

    public String packageName;

    public PackageState state;

    public PackagePhase phase;

    public String detail;

    private DiagnosisContext intermediaDirs;

    private MdxInfoMapper mdxInfoMapper;

    private static final String[] sourceDirs = {
            "conf",
            "logs",
            "dataset",
            Paths.PUBLIC_PATH + File.separator + "WEB-INF" + File.separator + "schema",
            Paths.PUBLIC_PATH + File.separator + "WEB-INF"
    };

    public PackageThread(Long startTimeStamp, Long endTimeStamp, String semanticMDXPath, String queryTime, String ip,
                         String port, LicenseManager licenseManager, Map<String, String> mapDatasetName2Json, DiagnosisContext intermediaDirs, MdxInfoMapper mdxInfoMapper) {
        this.startAt = conditionDateFormat.format(new Date(startTimeStamp * 1000));
        this.endAt = conditionDateFormat.format(new Date(endTimeStamp * 1000));
        this.semanticMdxPath = semanticMDXPath;
        this.queryTime = queryTime;
        this.ip = ip;
        this.port = port;
        this.licenseManager = licenseManager;
        this.mapDatasetName2Json = mapDatasetName2Json;
        semanticMDXPath = semanticMDXPath + File.separator + Paths.SHELL_SCRIPT_PATH;
        if (!startAt.isEmpty()) {
            //give arguments if there is
            semanticMDXPath = semanticMDXPath + " " +
                    startAt.split(" ")[0] + " " +
                    endAt.split(" ")[0] + " " +
                    queryTime + " " +
                    ip + " " +
                    port;
        }
        shell = "bash";
        this.scriptPath = shell + " " + semanticMDXPath;
        this.intermediaDirs = intermediaDirs;
        this.mdxInfoMapper = mdxInfoMapper;
    }

    @SneakyThrows
    @Override
    public void run() {
        phase = PackagePhase.EXTRACT_PACKAGE_INFO;
        state = PackageState.RUNNING;
        detail = "Start extracting package information";
        log.info(detail);

        //logs, schema and properties
        if (!extractData()) {
            return;
        }
        //extract json data of datasets
        if (!writeDatasetJson()) {
            return;
        }
        //env info
        if (!writeMdxEnv()) {
            return;
        }
        //sync info
        if (!writeSyncStatus()) {
            return;
        }
        //copy scripts
        if (!copyScripts()) {
            return;
        }

        //generate package
        phase = PackagePhase.START_COMPRESSION;
        detail = "Start compression";
        log.info(detail);
        int status = executeShellScript();
        if (status != 0) {
            packageError("Task error with exit code " + status + " while compression on " + scriptPath, null);
            return;
        }
        String packagePath = semanticMdxPath + File.separator + Paths.PACKAGE_DIR_PATH;
        renamePackage(packagePath);
    }

    private void renamePackage(String packagePath) {
        File generatedPackage = new File(packagePath + File.separator + packageName);
        try {
            packageName = ip + "_" + port + "_full_" +
                    packageName.replace(".tar.gz", Utils.genMD5Checksum(generatedPackage).substring(0, 7).toUpperCase() + ".tar.gz");
        } catch (Exception e) {
            packageError("Task error while rename file", e);
            return;
        }
        String standardName = packagePath + File.separator + packageName;
        //rename the file
        if (!generatedPackage.renameTo(new File(standardName))) {
            log.error("rename to {} fail", standardName);
        }
        state = PackageState.SUCCESS;
        phase = PackagePhase.PACKAGE_COMPLETE;
        if (startAt.isEmpty()) {
            detail = queryTime + "_diagnose_package.tar.gz";
        }
        detail = packageName;
        log.info("Package name is {}", detail);
    }

    private int executeShellScript() {
        int status = -1;
        Process pro;
        try {
            pro = ShellUtils.executeShell(scriptPath);
            status = ShellUtils.getPidStatus(pro);
        } catch (IOException e) {
            state = PackageState.ERROR;
            detail = "Task error while executing " + scriptPath;
            return status;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            state = PackageState.ERROR;
            detail = "Task error while executing " + scriptPath;
            return status;
        }
        try {
            packageName = ShellUtils.getPidName(pro);
        } catch (FileNotFoundException e) {
            packageError("Generated package is missing", e);
        } catch (IOException e) {
            packageError("IOException occurred while compression", e);
        }
        return status;
    }

    private boolean isEndsWithNl(String s) {
        if (s == null) {
            return false;
        }
        return s.endsWith("\n") || s.endsWith("\r\n") || s.endsWith("\r");
    }

    private String getEnvInfo() {
        final String nl = System.getProperty("line.separator");

        StringBuilder buf = new StringBuilder();
        String mdxHome;
        File f = new File(System.getProperty("user.dir") + File.separator + "..");
        try {
            mdxHome = f.getCanonicalPath();
        } catch (IOException e) {
            mdxHome = f.getAbsolutePath();
        }
        buf.append("mdx.home").append(": ").append(mdxHome).append(nl);

        String mdxVersion = licenseManager.getKiVersion();
        if (mdxVersion == null || "".equals(mdxVersion)) {
            mdxVersion = DiagnosisService.UNKNOWN;
        }

        buf.append("mdx.version").append(": ").append(mdxVersion);
        if (!isEndsWithNl(mdxVersion)) {
            buf.append(nl);
        }

        String commit = licenseManager.getCommitId();
        if (commit == null || "".equals(commit)) {
            commit = DiagnosisService.UNKNOWN;
        }

        buf.append("commit").append(": ").append(commit);
        if (!isEndsWithNl(commit)) {
            buf.append(nl);
        }
        buf.append(nl);

        buf.append("------------ The information of database ---------------").append(nl);
        String databaseVersion = mdxInfoMapper.getDatabaseVersion();
        buf.append("database.type: ").append(SemanticConfig.getInstance().getDatabaseType()).append(nl);
        buf.append("database.version: ").append(databaseVersion).append(nl);
        buf.append("--------------------------------------------------------").append(nl).append(nl);

        buf.append("----------------- The config of MDX --------------------").append(nl);
        Properties mdxConfigs = SemanticConfig.getInstance().getProperties();
        Enumeration<?> enumeration = mdxConfigs.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            String value = mdxConfigs.getProperty(key);
            buf.append(key).append(": ").append(value).append(nl);
        }
        buf.append("--------------------------------------------------------").append(nl).append(nl);

        buf.append("---------------- The system properties -----------------").append(nl);
        Properties systemProperties = System.getProperties();
        Enumeration<?> propertyEnumeration = systemProperties.propertyNames();
        while (propertyEnumeration.hasMoreElements()) {
            String key = (String) propertyEnumeration.nextElement();
            String value = systemProperties.getProperty(key);
            buf.append(key).append(": ").append(value).append(nl);
        }
        buf.append("--------------------------------------------------------").append(nl).append(nl);

        buf.append("---------------- The system environment ----------------").append(nl);
        Map<String, String> sysEnvironment = System.getenv();
        for (String mapKey : sysEnvironment.keySet()) {
            buf.append(mapKey).append(": ").append(sysEnvironment.get(mapKey)).append(nl);
        }
        buf.append("--------------------------------------------------------").append(nl).append(nl);

        buf.append("---------------- The startup parameters ----------------").append(nl);
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> args = runtime.getInputArguments();
        if (args != null && !args.isEmpty()) {
            for (String arg : args) {
                buf.append(arg).append(nl);
            }
        }
        buf.append("--------------------------------------------------------").append(nl).append(nl);
        return buf.toString();
    }

    public boolean writeMdxEnv() {
        detail = "Now writing environmental information file";
        log.info(detail);
        File mdxEnv = new File(semanticMdxPath + File.separator + intermediaDirs.confDir + File.separator + "mdx_env");
        try {
            if (!mdxEnv.getParentFile().exists()) {
                mdxEnv.getParentFile().mkdirs();
            } else if (!mdxEnv.exists()) {
                if (!mdxEnv.createNewFile() && !mdxEnv.exists()) {
                    return packageError("Error when creating mdx_env file", null);
                }
            }
        } catch (IOException e) {
            return packageError("Error when creating mdx_env file", e);
        }

        String env = getEnvInfo();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mdxEnv)))) {
            bw.write(env);
        } catch (IOException e) {
            return packageError("Error while writing to mdx_env file.", e);
        }

        return true;
    }

    public boolean writeSyncStatus() {
        detail = "Now writing sync task information file";
        log.info(detail);
        File syncInfoFile = new File(semanticMdxPath + File.separator + intermediaDirs.confDir + File.separator + "sync_info");
        try {
            if (!syncInfoFile.getParentFile().exists()) {
                syncInfoFile.getParentFile().mkdirs();
            } else if (!syncInfoFile.exists() && !syncInfoFile.createNewFile()) {
                packageError("Error when creating sync task information file", null);
                return false;
            }
        } catch (IOException e) {
            return packageError("Error when creating sync task information file", e);
        }

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(syncInfoFile)))) {
            String syncInfoString = getSyncInfo();
            bw.write(syncInfoString);
        } catch (IOException e) {
            return packageError("Error while writing to sync task information file.", e);
        }

        return true;
    }

    private String getSyncInfo() {
        String syncUser = SemanticConfig.getInstance().getKylinUser();
        StringBuilder sb = new StringBuilder();
        if (syncUser == null) {
            sb.append("Sync task user is not specified!");
        } else {
            sb.append("Current sync user is: ").append(syncUser);
        }
        sb.append("\nCurrent sync task status is: ").append(MdxContext.getSyncStatus()).append("\n");
        return sb.toString();
    }

    private boolean copyScripts() {
        try {
            String oldSetJvmFile = semanticMdxPath + File.separator + "semantic-mdx" + File.separator + "set-jvm.sh";
            File setJvmFile = new File(semanticMdxPath + File.separator + intermediaDirs.confDir + File.separator + "set-jvm.sh");
            FileUtils.copyFile(new File(oldSetJvmFile), setJvmFile);
            String oldStartupFile = semanticMdxPath + File.separator + "semantic-mdx" + File.separator + "startup.sh";
            File startupFile = new File(semanticMdxPath + File.separator + intermediaDirs.confDir + File.separator + "startup.sh");
            FileUtils.copyFile(new File(oldStartupFile), startupFile);
        } catch (Exception e) {
            return packageError("Error when copying jvm and start up scripts.", e);
        }
        return true;
    }

    private boolean writeDatasetJson() {
        //get json of datasets in specified time periods
        for (Map.Entry<String, String> entry : mapDatasetName2Json.entrySet()) {
            detail = "Now writing json file for " + entry.getKey();
            log.info(detail);
            File currentDatasetFile = new File(semanticMdxPath + File.separator + intermediaDirs.datasetDir + File.separator + entry.getKey() + ".json");
            try {
                if (!currentDatasetFile.getParentFile().exists()) {
                    currentDatasetFile.getParentFile().mkdirs();
                } else if (!currentDatasetFile.exists() && !currentDatasetFile.createNewFile()) {
                    return packageError("Error when creating json file for " + entry.getKey(), null);
                }
            } catch (IOException e) {
                return packageError("Error when creating json file for " + entry.getKey(), e);
            }
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentDatasetFile)))) {
                bw.write(entry.getValue());
            } catch (IOException e) {
                return packageError("Error while writing json file for " + entry.getKey(), e);
            } catch (SemanticException e) {
                return packageError("Error while retrieving json for " + entry.getKey(), e);
            }
        }
        return true;
    }

    private boolean extractData() {
        // prepare directory
        List<String> targetDirs = intermediaDirs.getDirs();
        for (int i = 0; i < sourceDirs.length; i++) {
            File currentDir = new File(semanticMdxPath + File.separator + targetDirs.get(i));
            if (!currentDir.exists()) {
                currentDir.mkdirs();
            }
            //extract data
            File sourceDir = new File(semanticMdxPath + File.separator + sourceDirs[i]);
            if (!sourceDir.exists() || sourceDir.listFiles() == null) {
                detail = "Source directory " + sourceDir.getName() + " or its content does not exist";
                log.warn(detail);
                continue;
            }
            File[] files = sourceDir.listFiles();
            List<File> fileList = new ArrayList<>(Arrays.asList(files));
            fileList = Utils.sortFileListByLastModified(fileList);

            for (File file : fileList) {
                if (file.isDirectory() || file.getName().contains("marker")) {
                    continue;
                }
                if (sourceDir.getName().contentEquals("logs")
                        && !file.getName().contains("mdx.log")
                        && !file.getName().contains("semantic.log")
                        && !file.getName().contains("semantic.out")
                        && !file.getName().contains("semantic_access")
                        && !file.getName().contains("performance.log")
                        && !file.getName().contains("heapdump.hprof")
                        && !(file.getName().startsWith("gc") && file.getName().endsWith(".log"))
                        && !(file.getName().contains("jstack.log"))
                        && !(file.getName().contains("jmap.log"))
                        && !(file.getName().contains("top.log"))) {
                    continue;
                }
                // 清理七天前的gc log文件
                if (file.getName().startsWith("gc") && file.getName().endsWith(".log")
                        && System.currentTimeMillis() - file.lastModified() > DiagnosisService.SEVEN_DAY) {
                    try {
                        Files.delete(file.toPath());
                    } catch (IOException e) {
                        log.warn("Delete old files seven days ago failed, file name:{}", file.getName());
                    }
                    continue;
                }
                //extract for each file
                if (!extractFromCurrentFile(file, i)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean extractFromCurrentFile(File file, int sourceDirIndex) {
        Date startDate, endDate;
        String newFileName = file.getName();
        detail = "Now extracting file with name " + file.getName();
        log.info(detail);
        // semantic 和 mdx 日志使用一个文件
        if (file.getName().contains("mdx.log")) {
            newFileName = "mdx.log";
        }
        if (file.getName().contains("semantic.log")) {
            newFileName = "semantic.log";
        }
        List<String> targetDirs = intermediaDirs.getDirs();
        File newFile = new File(semanticMdxPath + File.separator + targetDirs.get(sourceDirIndex) + File.separator + newFileName);
        if (!newFile.exists()) {
            if (!newFile.getParentFile().exists()) {
                newFile.getParentFile().mkdirs();
            }
            try {
                if (!newFile.createNewFile() && !newFile.exists()) {
                    return packageError("Error when extracting from log file", null);
                }
            } catch (IOException e) {
                return packageError("Error when extracting from log file", e);
            }
        }
        // semantic.out, jmap.log 和 heapdump 文件直接拷贝
        if (file.getName().contains("semantic.out") || file.getName().contains("jmap.log") ||
                file.getName().contains("heapdump.hprof")) {
            // 默认不提交 heapdump 文件
            if (file.getName().contains("heapdump.hprof") && !SemanticConfig.getInstance().isGetHeapDump()) {
                return true;
            }
            try {
                FileUtils.copyFile(file, newFile);
                return true;
            } catch (IOException e) {
                return packageError("IOException occurred", e);
            }
        }
        // 其它的日志只截取对应时间区间的日志
        boolean noContent = false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile, true)))
        ) {
            String line;
            boolean copyStart = false;
            boolean copyEnd = false;
            if (!sourceDirs[sourceDirIndex].contentEquals("logs")) {
                //for schema, just copy
                while ((line = br.readLine()) != null) {
                    bw.write(line + "\n");
                }
            } else {
                //only filter data for logs
                //only extract data modified in specified time period
                startDate = conditionDateFormat.parse(startAt);
                endDate = conditionDateFormat.parse(endAt);
                Long start = startDate.getTime();
                Long end = endDate.getTime();
                if (Utils.isTargetFile(startDate, file)) {
                    while ((line = br.readLine()) != null) {
                        ///extract data in specified time range
                        if (line.split(",").length == 0) {
                            continue;
                        }
                        String lineStart = "";
                        // the format of date time in gc.log：2020-11-25T14:48:01.484+0800: 0.679
                        if (file.getName().startsWith("gc") && file.getName().endsWith(".log")) {
                            String[] linePartArray = line.split(":");
                            if (linePartArray.length >= 3) {
                                lineStart = linePartArray[0].replace("T", " ") + ":" + linePartArray[1] + ":" + "00";
                            }
                        } else {
                            lineStart = line.split(",")[0];
                        }
                        if (Utils.isDateString(lineStart, "yyyy-MM-dd HH:mm:ss")) {
                            try {
                                Date dateInLine = conditionDateFormat.parse(lineStart);
                                Long inLineTime = dateInLine.getTime();
                                int toStart = inLineTime.compareTo(start);
                                int toEnd = inLineTime.compareTo(end);
                                if (toStart >= 0 && toEnd <= 0) {
                                    copyStart = true;
                                } else if (toEnd > 0) {
                                    copyEnd = true;
                                }
                            } catch (ParseException e) {
                                return packageError("Bad format of date in line as " + e.getMessage(), e);
                            }
                        }
                        if (copyEnd) {
                            break;
                        }
                        if (copyStart) {
                            bw.write(line + "\n");
                        }
                    }
                }
                if (!copyStart) {
                    //There is no data in specified time range from this file
                    bw.write("For file : " + file.getName() + " No data in this time range\n\n");
                    noContent = true;
                } else {
                    //There is data in specified time range from this file
                    bw.write("Content above from file : " + file.getName() + "\n\n");
                }
            }
        } catch (FileNotFoundException e) {
            return packageError("No source file", e);
        } catch (IOException e) {
            return packageError("IOException occurred", e);
        } catch (ParseException e) {
            return packageError("Bad format of query date as " + e.getMessage(), e);
        } catch (Exception e) {
            return packageError("Unexpected error of " + e.getMessage(), e);
        }
        // 删除空 semantic_access 和 gc 日志
        if (noContent && (newFileName.startsWith("semantic_access") || newFileName.startsWith("gc"))) {
            if (!newFile.delete()) {
                return packageError("delete " + newFileName + " fail", null);
            }
        }
        return true;
    }

    public boolean packageError(String msg, Exception e) {
        state = PackageState.ERROR;
        detail = msg;
        if (e != null) {
            log.error(msg, e);
        } else {
            log.error(msg);
        }
        return false;
    }

}
