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


package io.kylin.mdx.insight.common.util;

import io.kylin.mdx.insight.common.constants.ConfigConstants;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class UtilsTest {

    private static final int fileNum = 5;

    private static final String dirToDelete = "src/test/resources/directoryToDelete";

    @AfterClass
    public static void clean() {
        Utils.deleteDir(dirToDelete);
    }

    @Test
    public void testFormatStr() {
        String str = Utils.formatStr("Inserting dataset summary record gets a failure, r:%d, id:%d", 0, null);
        Assert.assertEquals("Inserting dataset summary record gets a failure, r:0, id:null", str);
    }

    @Test
    public void testBasiAuthDecode() {
        String basiAuthStr = "Basic QURNSU46S1lMSU4=";

        String[] basic = Utils.decodeBasicAuth(basiAuthStr);

        Assert.assertEquals("ADMIN", basic[0]);
        Assert.assertEquals("KYLIN", basic[1]);
    }

    @Test
    public void testEncodeTxt() {
        String encodedAuth = Utils.encodeTxt(10, "KYLIN");
        String decodeTxt = Utils.decodeTxt(10, encodedAuth).split(":")[0];
        Assert.assertEquals("KYLIN", decodeTxt);
    }

    @Test
    public void deleteDirTest() throws IOException {
        createFiles(dirToDelete, System.currentTimeMillis());
        File file = new File(dirToDelete);
        Assert.assertTrue(file.exists());
        Utils.deleteDir(dirToDelete);
        Assert.assertFalse(file.exists());
    }

    public List<File> createFiles(String dirToDelete, long startTime) throws IOException {
        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < fileNum; i++) {
            File file = new File(dirToDelete + File.separator + i + ".txt");
            if (file.exists()) {
                file.delete();
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            file.createNewFile();
            file.setLastModified(startTime);
            startTime += 1000;
            fileList.add(file);
        }
        return fileList;
    }

    @Test
    public void genMD5ChecksumTest() throws IOException {
        List<File> fileList = createFiles(dirToDelete, System.currentTimeMillis());
        try {
            String checkSum = Utils.genMD5Checksum(fileList.get(0));
            Assert.assertEquals(checkSum, "d41d8cd98f00b204e9800998ecf8427e");
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void isDateStringTest() {
        //correct value and format
        String dateValue = "2020-09-08 12:12:00";
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        Assert.assertTrue(Utils.isDateString(dateValue, dateFormat));

        //wrong value
        dateValue = "sbfff";
        dateFormat = "yyyy-MM-dd HH:mm:ss";
        Assert.assertFalse(Utils.isDateString(dateValue, dateFormat));

        //wrong format
        dateValue = "2020-09-08 12:12:00";
        dateFormat = "sfdsff";
        Assert.assertFalse(Utils.isDateString(dateValue, dateFormat));
    }

    @Test
    public void sortFileListByLastModifiedTest() throws IOException, InterruptedException {
        List<File> expectedFileList = createFiles(dirToDelete, System.currentTimeMillis());
        Collections.reverse(expectedFileList);
        List<File> originFileList = new ArrayList<>();
        for (int i = 0; i < fileNum; i++) {
            originFileList.add(expectedFileList.get((i + 1) % fileNum));
        }
        originFileList = Utils.sortFileListByLastModified(originFileList);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(originFileList.get(i).getName(), expectedFileList.get(i).getName());
        }
    }

    @Test
    public void isTargetFileTest() throws IOException {
        long startTime = System.currentTimeMillis();
        Date startDate = new Date(startTime);
        List<File> fileList = createFiles(dirToDelete, startTime + 1000);
        for (File file : fileList) {
            Assert.assertTrue(Utils.isTargetFile(startDate, file));
        }
    }

    @Test
    public void endWithSlash() {
        Assert.assertEquals("/", Utils.endWithSlash(""));
        Assert.assertEquals("/", Utils.endWithSlash("/"));
        Assert.assertEquals("/mdx/", Utils.endWithSlash("/mdx"));
        Assert.assertEquals("/mdx/", Utils.endWithSlash("/mdx/"));
    }

    @Test
    public void endWithoutSlash() {
        Assert.assertEquals("", Utils.endWithoutSlash(""));
        Assert.assertEquals("", Utils.endWithoutSlash("/"));
        Assert.assertEquals("/mdx", Utils.endWithoutSlash("/mdx"));
        Assert.assertEquals("/mdx", Utils.endWithoutSlash("/mdx/"));
    }

    @Test
    public void startWithoutSlash() {
        Assert.assertEquals("", Utils.startWithoutSlash(""));
        Assert.assertEquals("", Utils.startWithoutSlash("/"));
        Assert.assertEquals("mdx", Utils.startWithoutSlash("/mdx"));
        Assert.assertEquals("mdx/", Utils.startWithoutSlash("/mdx/"));
    }

    @Test
    public void filterMapByKey() {
        Map<String, String> source = new HashMap<>();
        source.put(ConfigConstants.KYLIN_HOST, "localhost");
        source.put(ConfigConstants.KYLIN_PORT, "7080");
        source.put(ConfigConstants.DATASET_ALLOW_ACCESS_BY_DEFAULT, "false");
        Map<String, String> target1 = Utils.filterMapByKey(source, Collections.singleton("insight.kylin.*"));
        Assert.assertEquals(target1.size(), 2);
        Map<String, String> target2 = Utils.filterMapByKey(source, Collections.singleton(ConfigConstants.DATASET_ALLOW_ACCESS_BY_DEFAULT));
        Assert.assertEquals(target2.size(), 1);
    }

}
