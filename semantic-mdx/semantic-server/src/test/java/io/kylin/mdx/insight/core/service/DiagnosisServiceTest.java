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


package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.PackagePhase;
import io.kylin.mdx.insight.core.entity.PackageState;
import io.kylin.mdx.insight.core.support.PackageThread;
import io.kylin.mdx.insight.core.entity.Paths;
import io.kylin.mdx.insight.server.SemanticLauncher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
@Slf4j
public class DiagnosisServiceTest extends BaseEnvSetting {
    //TODO : FIX ME : currently fetching dataset metadata cannot work properly

    private static final String TEMP_PATH = "diagnosis_tmp";

    private static final String ROOT_PATH = "src/test/resources/diagnosisDir";

    private static final SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private final int daysAgo = -8;

    @Autowired
    private DiagnosisService diagnosisService;

    @AfterClass
    public static void clear() {
        Utils.deleteDir(ROOT_PATH + File.separator + Paths.PACKAGE_DIR_PATH);
        File rootDit = new File(ROOT_PATH + File.separator + TEMP_PATH);
        File[] tmpDirs = rootDit.listFiles();
        if (tmpDirs != null) {
            for (File tmpDir : tmpDirs) {
                if (tmpDir.isDirectory()) {
                    Utils.deleteDir(tmpDir.getAbsolutePath());
                    log.info("Delete temp directory : " + tmpDir.getAbsolutePath());
                }
            }
        }
    }

    private String createTmpDir() {
        String tmpName = TEMP_PATH + File.separator + "tmp_" + System.currentTimeMillis();
        String tmpPath = ROOT_PATH + File.separator + tmpName;
        File tmpDir = new File(tmpPath);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        return tmpName;
    }

    @Test
    public void retrieveClusterInfoTest() {
        SemanticConfig.getInstance().setClustersInfo("localhost:8355,cnn:bbc");
        Map<String, String> nodeStatusMap = new HashMap<>();
        nodeStatusMap.put("localhost:8355", "inactive");
        nodeStatusMap.put("cnn:bbc", "inactive");
        Map<String, String> resultMap = new HashMap<>();
        diagnosisService.retrieveClusterInfo(resultMap);
        for (String clusterNode : resultMap.keySet()) {
            Assert.assertEquals(resultMap.get(clusterNode), nodeStatusMap.get(clusterNode));
        }
    }

    @Test
    public void extractSpecifiedDataTest() throws IOException {
        String tmpName = createTmpDir();

        // 创建 conf 目录
        File confDir = new File(ROOT_PATH + "/conf");
        FileUtils.copyDirectory(new File("src/test/resources/conf"), confDir);

        // 创建 logs 目录
        File logsDir = new File(ROOT_PATH + "/logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        FileUtils.write(new File(logsDir, "mdx.log"), "");
        FileUtils.write(new File(logsDir, "semantic.log"), "");
        FileUtils.write(new File(logsDir, "performance.log"), "");
        FileUtils.write(new File(logsDir, "gc.log"), "2021-10-18T16:32:10.881-0800: 0.655: [GC pause (G1 Evacuation Pause) (young), 0.0144384 secs]");

        long currentTime = System.currentTimeMillis();
        Date startDate = new Date(currentTime);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DATE, daysAgo);
        startDate = calendar.getTime();

        Date date = new Date(currentTime);
        String queryTime = queryDateFormat.format(date);
        Map<String, String> jsonMap = new HashMap<>();
        String project = "UT_project";
        String dataset = "UT_dataset";
        String datasetStr = "UT_dataset string";
        jsonMap.put("Project-" + project + "_Dataset-" + dataset, datasetStr);

        PackageThread packageThread = diagnosisService.extractSpecifiedData(
                startDate.getTime(),
                currentTime,
                ROOT_PATH,
                tmpName,
                queryTime,
                SemanticConfig.getInstance().getMdxHost(),
                SemanticConfig.getInstance().getMdxPort(),
                jsonMap);
        while (packageThread.isAlive()) {
            Thread.yield();
        }
        Assert.assertNotNull(packageThread);
        Assert.assertEquals(packageThread.state, PackageState.SUCCESS);

        Utils.deleteDir(confDir.getPath());
        Utils.deleteDir(logsDir.getPath());
    }

    public void createPackages(String dirToDelete, String createPackageTime) throws IOException {
        List<String> intermediaDirs = new ArrayList<>();
        intermediaDirs.add(dirToDelete + File.separator + "127.0.0.1_7080_full_" + createPackageTime + "_diagnose_package524A609.tar.gz");
        for (String intermediaDir : intermediaDirs) {
            File file = new File(intermediaDir);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
        }
    }

    @Test
    public void clearOldPackageTest() throws IOException {
        Date queryDate = new Date(System.currentTimeMillis());
        String queryTime = queryDateFormat.format(queryDate);
        Calendar c = Calendar.getInstance();
        c.setTime(queryDate);
        c.add(Calendar.DATE, daysAgo);
        queryDate = c.getTime();
        String createPackageTime = queryDateFormat.format(queryDate);
        createPackages(ROOT_PATH + File.separator + Paths.PACKAGE_DIR_PATH, createPackageTime);
        Assert.assertTrue(diagnosisService.clearOldPackage(queryTime, ROOT_PATH));
    }

    @Test
    public void clearOldTempFilesTest() {
        String tmpName = createTmpDir();
        Date queryDate = new Date(System.currentTimeMillis());
        String queryTime = queryDateFormat.format(queryDate);
        Calendar c = Calendar.getInstance();
        c.setTime(queryDate);
        c.add(Calendar.DATE, daysAgo);
        queryDate = c.getTime();
        String createTempFileTime = queryDateFormat.format(queryDate);
        createTempFiles(ROOT_PATH + File.separator + tmpName, createTempFileTime);
        Assert.assertTrue(diagnosisService.clearOldTempFiles(queryTime, ROOT_PATH, tmpName));
    }

    private void createTempFiles(String dirToCreate, String createTempFileTime) {
        List<String> intermediaDirs = new ArrayList<>();
        intermediaDirs.add(dirToCreate + File.separator + "conf_" + createTempFileTime);
        intermediaDirs.add(dirToCreate + File.separator + "logs_" + createTempFileTime);
        intermediaDirs.add(dirToCreate + File.separator + "schema_" + createTempFileTime);
        for (String intermediaDir : intermediaDirs) {
            File file = new File(intermediaDir);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            if (!file.exists()) {
                file.mkdir();
            }
        }
    }

    @Test
    public void getPackageStateTest() {
        String tmpName = createTmpDir();
        Date startDate = new Date(System.currentTimeMillis());
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        c.add(Calendar.DATE, daysAgo);
        startDate = c.getTime();

        Date date = new Date(System.currentTimeMillis());
        String queryTime = queryDateFormat.format(date);

        PackageThread packageThread = diagnosisService.extractSpecifiedData(
                startDate.getTime(),
                System.currentTimeMillis(),
                ROOT_PATH,
                tmpName,
                queryTime,
                SemanticConfig.getInstance().getMdxHost(),
                SemanticConfig.getInstance().getMdxPort(),
                Collections.emptyMap());

        packageThread.phase = PackagePhase.EXTRACT_PACKAGE_INFO;
        packageThread.state = PackageState.RUNNING;
        String state = diagnosisService.getPackageState(packageThread);
        Assert.assertNotNull(state);
        diagnosisService.stopPackaging(packageThread, ROOT_PATH);

        packageThread.packageError("", new Exception());
    }

    @Test
    public void testInfo() {
        diagnosisService.logProcessInfo();
    }

    @Test
    public void testExtractData() throws IOException {
        String tmpName = createTmpDir();

        // 创建 conf 目录
        File confDir = new File(ROOT_PATH + "/conf");
        FileUtils.copyDirectory(new File("src/test/resources/conf"), confDir);

        // 创建 logs 目录
        File logsDir = new File(ROOT_PATH + "/logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        FileUtils.write(new File(logsDir, "mdx.log"), "");
        FileUtils.write(new File(logsDir, "semantic.log"), "");
        FileUtils.write(new File(logsDir, "performance.log"), "");
        FileUtils.write(new File(logsDir, "test.log"), "test");
        FileUtils.write(new File(logsDir, "jmap.log"), "");
        FileUtils.write(new File(logsDir, "gc.log"), "2021-10-18T16:32:10.881-0800: 0.655: [GC pause (G1 Evacuation Pause) (young), 0.0144384 secs]");
        new File(logsDir, "gc.log").setLastModified(System.currentTimeMillis() - DiagnosisService.SEVEN_DAY * 2);
        FileUtils.write(new File(logsDir, "heapdump.hprof"), "");
        long currentTime = System.currentTimeMillis();
        Date startDate = new Date(currentTime);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DATE, daysAgo);
        startDate = calendar.getTime();

        Date date = new Date(currentTime);
        String queryTime = queryDateFormat.format(date);
        Map<String, String> jsonMap = new HashMap<>();
        String project = "UT_project";
        String dataset = "UT_dataset";
        String datasetStr = "UT_dataset string";
        jsonMap.put("Project-" + project + "_Dataset-" + dataset, datasetStr);

        PackageThread packageThread = diagnosisService.extractSpecifiedData(
                startDate.getTime(),
                currentTime,
                ROOT_PATH,
                tmpName,
                queryTime,
                SemanticConfig.getInstance().getMdxHost(),
                SemanticConfig.getInstance().getMdxPort(),
                jsonMap);
        while (packageThread.isAlive()) {
            Thread.yield();
        }
        Assert.assertNotNull(packageThread);
        Assert.assertEquals(packageThread.state, PackageState.SUCCESS);

        Utils.deleteDir(confDir.getPath());
        Utils.deleteDir(logsDir.getPath());
    }
    @Test
    public void testExtractDataTime() throws IOException {
        String tmpName = createTmpDir();

        // 创建 conf 目录
        File confDir = new File(ROOT_PATH + "/conf");
        FileUtils.copyDirectory(new File("src/test/resources/conf"), confDir);

        // 创建 logs 目录
        File logsDir = new File(ROOT_PATH + "/logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        FileUtils.write(new File(logsDir, "mdx.log"), "");
        FileUtils.write(new File(logsDir, "semantic.log"), "");
        FileUtils.write(new File(logsDir, "performance.log"), "performance");
        FileUtils.write(new File(logsDir, "test.log"), "test");
        FileUtils.write(new File(logsDir, "jmap.log"), "");
        FileUtils.write(new File(logsDir, "gc.log"), "2021-10-18T16:32:10.881-0800: 0.655: [GC pause (G1 Evacuation Pause) (young), 0.0144384 secs]");
        //new File(logsDir, "gc.log").setLastModified(System.currentTimeMillis() - DiagnosisService.SEVEN_DAY * 2);
        FileUtils.write(new File(logsDir, "heapdump.hprof"), "");
        long currentTime = System.currentTimeMillis();
        Date startDate = new Date(currentTime);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DATE, daysAgo);
        startDate = calendar.getTime();

        Date date = new Date(currentTime);
        String queryTime = queryDateFormat.format(date);
        Map<String, String> jsonMap = new HashMap<>();
        String project = "UT_project";
        String dataset = "UT_dataset";
        String datasetStr = "UT_dataset string";
        jsonMap.put("Project-" + project + "_Dataset-" + dataset, datasetStr);

        PackageThread packageThread = diagnosisService.extractSpecifiedData(
                startDate.getTime()/1000,
                currentTime,
                ROOT_PATH,
                tmpName,
                queryTime,
                SemanticConfig.getInstance().getMdxHost(),
                SemanticConfig.getInstance().getMdxPort(),
                jsonMap);
        while (packageThread.isAlive()) {
            Thread.yield();
        }
        Assert.assertNotNull(packageThread);
        Assert.assertEquals(packageThread.state, PackageState.SUCCESS);

        Utils.deleteDir(confDir.getPath());
        Utils.deleteDir(logsDir.getPath());
    }

}
