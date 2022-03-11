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


package io.kylin.mdx.insight.engine.service;

import io.kylin.mdx.insight.common.SemanticConfig;
import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.dao.MdxInfoMapper;
import io.kylin.mdx.insight.core.entity.Paths;
import io.kylin.mdx.insight.core.manager.LicenseManager;
import io.kylin.mdx.insight.core.support.PackageThread;
import io.kylin.mdx.insight.core.entity.DiagnosisContext;
import io.kylin.mdx.insight.core.entity.PackagePhase;
import io.kylin.mdx.insight.core.entity.PackageState;
import io.kylin.mdx.insight.core.service.DiagnosisService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class DiagnosisServiceImpl extends DiagnosisService {

    private static final Logger stackLog = LoggerFactory.getLogger("diagnosis.jstack");

    private static final Logger mapLog = LoggerFactory.getLogger("diagnosis.jmap");

    private static final Logger topLog = LoggerFactory.getLogger("diagnosis.top");

    private final SemanticConfig semanticConfig = SemanticConfig.getInstance();

    private String processId;

    private String javaHome;

    private DiagnosisContext intermediaDirs;

    /**
     * currently, keep packages and tmp files in 7 days
     */
    private static final int daysAgo = -7;

    private final String[] sourceDirs = {
            "conf",
            "logs",
            "dataset",
            Paths.PUBLIC_PATH + File.separator + "WEB-INF" + File.separator + "schema",
            Paths.PUBLIC_PATH + File.separator + "WEB-INF"
    };

    @Autowired
    private LicenseManager licenseManager;

    @Autowired
    private MdxInfoMapper mdxInfoMapper;

    @PostConstruct
    public void init() {
        if (semanticConfig.isLogJavaInfoEnable()) {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            processId = runtime.getName().split("@")[0];
            javaHome = System.getProperty("java.home");
            if (javaHome.endsWith("jre")) {
                javaHome = javaHome.substring(0, javaHome.lastIndexOf(File.separator + "jre"));
            }
        }
    }

    @Override
    @Scheduled(cron = "${insight.semantic.log.interval:0 0/5 * * * ?}")
    public void logProcessInfo() {
        if (semanticConfig.isLogJavaInfoEnable()) {
            logStackInfo();
            logHeapInfo();
            logTopInfo();
        }
    }

    @Override
    public void logStackInfo() {
        String jStackCmd = javaHome + "/bin/jstack " + processId;
        CommandResult stackResult = execCommand(jStackCmd, false);
        if (StringUtils.isNotBlank(stackResult.getSuccessMessage())) {
            stackLog.info(stackResult.getSuccessMessage());
        } else {
            stackLog.warn(stackResult.getErrorMessage());
        }
    }

    @Override
    public void logHeapInfo() {
        String jMapCmd = javaHome + "/bin/jmap -histo " + processId;
        CommandResult stackResult = execCommand(jMapCmd, true);
        if (StringUtils.isNotBlank(stackResult.getSuccessMessage())) {
            mapLog.info(stackResult.getSuccessMessage());
        } else {
            mapLog.warn(stackResult.getErrorMessage());
        }
    }

    @Override
    public void logTopInfo() {
        String topCmd = "top -b -n 1";
        CommandResult stackResult = execCommand(topCmd, false);
        if (StringUtils.isNotBlank(stackResult.getSuccessMessage())) {
            topLog.info(stackResult.getSuccessMessage());
        } else {
            topLog.warn(stackResult.getErrorMessage());
        }
    }

    @Override
    public boolean retrieveClusterInfo(Map<String, String> resultMap) {
        String clusterInfo = SemanticConfig.getInstance().getClustersInfo();
        if (clusterInfo == null || clusterInfo.isEmpty() || clusterInfo.split(",").length < 1) {
            return false;
        }
        String[] clusters = clusterInfo.split(",");
        for (String cluster : clusters) {
            if (cluster.split(":").length < 2) {
                return false;
            }
            String ip = cluster.split(":")[0];
            String port = cluster.split(":")[1];
            String status = "unknown";
            resultMap.put(cluster, status);
            status = Utils.checkConnection(ip, port) ? "active" : "inactive";
            resultMap.put(cluster, status);
        }
        return true;
    }

    @Override
    public PackageThread extractSpecifiedData(Long startTimeStamp,
                                              Long endTimeStamp,
                                              String rootPath,
                                              String tmpPath,
                                              String queryTime,
                                              String ip,
                                              String port,
                                              Map<String, String> mapDatasetName2Json) {
        this.intermediaDirs = new DiagnosisContext();
        intermediaDirs.confDir = tmpPath + File.separator + "conf_" + queryTime;
        intermediaDirs.logsDir = tmpPath + File.separator + "logs_" + queryTime;
        intermediaDirs.schemaDir = tmpPath + File.separator + "schema_" + queryTime;
        intermediaDirs.datasetSourceDir = tmpPath + File.separator + "schema_" + queryTime;
        intermediaDirs.datasetDir = tmpPath + File.separator + "dataset_" + queryTime;
        clearOldPackage(queryTime, rootPath);
        clearOldTempFiles(queryTime, rootPath, tmpPath);
        //another thread do the task
        PackageThread generateThread = new PackageThread(startTimeStamp, endTimeStamp, rootPath, queryTime, ip, port,
                licenseManager, mapDatasetName2Json, intermediaDirs, mdxInfoMapper);
        generateThread.start();
        return generateThread;
    }

    /**
     * @param queryTime       : the time string of server receiving this query
     * @param semanticMdxPath : the path of 'semantic-mdx' folder
     * @return
     */
    @Override
    public boolean clearOldPackage(String queryTime, String semanticMdxPath) {
        boolean result = true;
        try {
            SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Date queryDate = queryDateFormat.parse(queryTime);
            Calendar c = Calendar.getInstance();
            c.setTime(queryDate);
            c.add(Calendar.DATE, daysAgo);
            queryDate = c.getTime();
            File diagnoseDir = new File(semanticMdxPath + File.separator + Paths.PACKAGE_DIR_PATH);
            Date oldDate;
            if (diagnoseDir.exists()) {
                File[] files = diagnoseDir.listFiles();
                if (files == null) {
                    return true;
                }
                for (File file : files) {
                    if (file.exists()) {
                        String fileName = file.getName();
                        //only check file with standard format name as : [ip_port_full_yyyyMMdd_hhmmss_diagnose_packageSixBitCheckSum.tar.gz]
                        if (!PackageName.isPackageName(fileName)) {
                            continue;
                        }
                        PackageName currentPackageName = new PackageName(fileName);
                        String dateTimeStr = currentPackageName.getDateTimeStr();
                        oldDate = queryDateFormat.parse(dateTimeStr);
                        if (oldDate.compareTo(queryDate) < 0) {
                            //delete file out of time
                            result = result && file.delete();
                        }
                    }
                }
            }
        } catch (ParseException pe) {
            log.error("Clear old package failed", pe);
            return false;
        }
        return result;
    }

    /**
     * @param queryTime : the time string of server receiving this query
     * @param mdxPath   : the path of 'semantic-mdx' folder
     * @return
     */
    @Override
    public boolean clearOldTempFiles(String queryTime, String mdxPath, String tmpPath) {
        try {
            SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Date queryDate = queryDateFormat.parse(queryTime);
            Calendar c = Calendar.getInstance();
            c.setTime(queryDate);
            c.add(Calendar.DATE, daysAgo);
            queryDate = c.getTime();
            File tmpDir = new File(mdxPath + File.separator + tmpPath);
            Date oldDate;
            if (tmpDir.exists()) {
                File[] files = tmpDir.listFiles();
                if (files == null) {
                    return true;
                }
                for (File file : files) {
                    if (file.exists()) {
                        String fileName = file.getName();
                        //only check file with standard format name as : [path/folder_yyyyMMdd_HHmmss]
                        if (!TempDirectoryName.isTempDirectoryName(fileName)) {
                            continue;
                        }
                        TempDirectoryName currentTempDirName = new TempDirectoryName(fileName);
                        String dateTimeStr = currentTempDirName.getDateTimeStr();
                        oldDate = queryDateFormat.parse(dateTimeStr);
                        if (oldDate.compareTo(queryDate) < 0) {
                            //delete file out of time
                            Utils.deleteDir(file.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (ParseException pe) {
            log.error("Clear old package failed", pe);
            return false;
        }
        return true;
    }

    @Override
    public String getPackageState(PackageThread packageThread) {
        //get the result of thread
        PackageState currentState = packageThread.state;
        PackagePhase currentPhase = packageThread.phase;
        String currentDetail = packageThread.detail;
        String[] states = new String[]{"", "", ""};
        String[] phases = new String[]{PackagePhase.EXTRACT_PACKAGE_INFO.toString(),
                PackagePhase.START_COMPRESSION.toString(),
                PackagePhase.PACKAGE_COMPLETE.toString()};
        String[] details = new String[]{"", "", ""};
        //get current phase to infer the state of other phase
        int index = currentPhase.ordinal();
        //infer the state of 3 phases
        for (int i = 0; i < phases.length; i++) {
            if (i < index) {
                states[i] = PackageState.SUCCESS.getMessage();
                details[i] = "Get information succeeded";
            } else if (i == index) {
                states[i] = currentState.getMessage();
                details[i] = currentDetail;
            } else {
                if (currentState != PackageState.ERROR) {
                    states[i] = PackageState.PENDING.getMessage();
                    details[i] = "pending";
                } else {
                    states[i] = PackageState.ERROR.getMessage();
                    details[i] = "Error occurred before this step";
                }
            }
        }
        StringJoiner commaJoiner = new StringJoiner(",");
        // current p1 p2 p3, each one is state:phase:detail
        for (int i = 0; i < phases.length; i++) {
            StringJoiner colonJoiner = new StringJoiner(":");
            colonJoiner.add(states[i]);
            colonJoiner.add(phases[i]);
            colonJoiner.add(details[i]);
            commaJoiner.add(colonJoiner.toString());
        }
        return commaJoiner.toString();
    }

    @Override
    public void stopPackaging(PackageThread packageThread, String rootPath) {
        PackageState currentState = packageThread.state;
        if (currentState.equals(PackageState.PENDING) || currentState.equals(PackageState.RUNNING)) {
            packageThread.interrupt();
        }
        //delete intermediate directory if exists
        for (String intermediaDir : intermediaDirs.getDirs()) {
            File currentDir = new File(rootPath + File.separator + intermediaDir);
            if (currentDir.exists()) {
                Utils.deleteDir(currentDir.getAbsolutePath());
            }
        }
    }


    /**
     * standard package name as : [ip_port_full_yyyyMMdd_hhmmss_diagnose_packagesixBitCheckSum.tar.gz]
     */
    private static class PackageName {

        private static int packageNamePartNum = 7;

        private static int packageNameDateIndex = 3;

        private static int getPackageNameTimeIndex = 4;

        private static String separator = "_";

        private String ip;
        private String port;
        private String type;
        private String date;
        private String time;
        private String diagnose;
        private String checkSumSuffix;

        public static boolean isPackageName(String fileName) {
            return fileName.split(separator).length == packageNamePartNum;
        }

        PackageName(String fileName) {
            parseName(fileName);
        }

        public void parseName(String fileName) {
            String[] stringPart = fileName.split(separator);
            ip = stringPart[0];
            port = stringPart[1];
            type = stringPart[2];
            date = stringPart[packageNameDateIndex];
            time = stringPart[getPackageNameTimeIndex];
            diagnose = stringPart[5];
            checkSumSuffix = stringPart[6];
        }

        public String getDateTimeStr() {
            return date + separator + time;
        }
    }

    /**
     * standard tmp folder name as : [path/folder_yyyyMMdd_HHmmss]
     */
    private static class TempDirectoryName {

        private static int tmpFileNamePartNum = 3;

        private static int tmpFileNameDateIndex = 1;

        private static int tmpFileNameTimeIndex = 2;

        private static String separator = "_";

        private String folderName;
        private String date;
        private String time;

        public static boolean isTempDirectoryName(String fileName) {
            return fileName.split(separator).length == tmpFileNamePartNum;
        }

        TempDirectoryName(String fileName) {
            parseName(fileName);
        }

        public void parseName(String fileName) {
            String[] stringPart = fileName.split(separator);
            folderName = stringPart[0];
            date = stringPart[tmpFileNameDateIndex];
            time = stringPart[tmpFileNameTimeIndex];
        }

        public String getDateTimeStr() {
            return date + separator + time;
        }
    }

    public static CommandResult execCommand(String cmd, boolean isRowLimit) {
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            successMsg = new StringBuilder();
            int limitRow = 0;
            while ((!isRowLimit || limitRow <= 100) && (s = successResult.readLine()) != null) {
                successMsg.append(s).append("\n");
                limitRow++;
            }
            errorMsg = new StringBuilder();
            while ((!isRowLimit || limitRow <= 100) && (s = errorResult.readLine()) != null) {
                errorMsg.append(s).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String success = successMsg == null ? "" : successMsg.toString();
        if (success.endsWith("\n")) {
            success = success.substring(0, success.length() - 1);
        }
        String error = errorMsg == null ? "" : errorMsg.toString();
        if (error.endsWith("\n")) {
            error = error.substring(0, error.length() - 1);
        }
        return new CommandResult(success, error);
    }

    @Data
    @AllArgsConstructor
    public static class CommandResult {
        private String successMessage;
        private String errorMessage;
    }
}
