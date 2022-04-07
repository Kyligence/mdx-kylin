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

import com.google.common.base.Splitter;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Slf4j
public class Utils {

    private static final String DATE_TIME_PATTERN_STR = "yyyy-MM-dd HH:mm:ss";

    private static final int SECOND_TO_MILLI = 1000;

    private static final ThreadLocal<MessageDigest> messageDigest = new ThreadLocal<>();

    private static MessageDigest messageDigestMD5Instance() throws NoSuchAlgorithmException {
        if (messageDigest.get() == null) {
            messageDigest.set(MessageDigest.getInstance("MD5"));
        }
        return messageDigest.get();
    }

    public static final Locale DEFAULT_LOCALE = Locale.US;

    public static String buildBasicAuth(String username, String password) {
        String encodedAuth = Base64.encodeBase64String((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return SemanticConstants.BASIC_AUTH_PREFIX + encodedAuth;
    }

    public static String buildAuthentication(String username, String password) {
        return Base64.encodeBase64String((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] encodeAuthentication(String username, String password) {
        return Base64.encodeBase64((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, String> createSplitMap(String str) {
        return Splitter.on(SemanticConstants.COMMA)
                .omitEmptyStrings()
                .trimResults()
                .withKeyValueSeparator(SemanticConstants.EQUAL)
                .split(str);
    }

    public static void addNoEmptyStrToSet(Set<String> set, String str) {
        if (StringUtils.isNotBlank(str)) {
            set.add(str);
        }
    }

    public static String[] decodeBasicAuth(String encodedUserPwd) {
        String encodedTxt = StringUtils.substringAfter(encodedUserPwd, SemanticConstants.BASIC_AUTH_PREFIX);
        String decodeUserPwd = new String(Base64.decodeBase64(encodedTxt), StandardCharsets.UTF_8);

        String user = StringUtils.substringBefore(decodeUserPwd, SemanticConstants.COLON);
        String pwd = StringUtils.substringAfter(decodeUserPwd, SemanticConstants.COLON);

        return new String[]{user, pwd};
    }

    public static boolean isCollectionEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    public static boolean isMapEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    public static String concat(String separator, String... args) {
        if (args == null || args.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                sb.append(args[i]);
            } else {
                sb.append(separator).append(args[i]);
            }
        }
        return sb.toString();
    }

    public static String formatStr(String str, Object... args) {
        return String.format(str, args);
    }

    public static String nullToEmptyStr(String str) {
        return str == null ? "" : str;
    }

    public static Integer nullToDefaultInteger(Integer src, Integer defaultVal) {
        return src == null ? defaultVal : src;
    }

    public static String blankToDefaultString(String src, String defaultVal) {
        return StringUtils.isBlank(src) ? defaultVal : src;
    }

    public static <T, H> void insertValToMap(Map<T, List<H>> map, T key, H val) {
        List<H> list = map.computeIfAbsent(key, k -> new LinkedList<>());
        list.add(val);
    }

    public static long currentTimeStamp() {
        return System.currentTimeMillis() / SECOND_TO_MILLI;
    }

    public static String convertDateStr(long time, boolean isSecondUnit) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN_STR);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(SemanticConfig.getInstance().getTimeZone()));
        return isSecondUnit ? simpleDateFormat.format(time * SECOND_TO_MILLI) : simpleDateFormat.format(time);
    }

    public static long getMilliseconds(long second) {
        return second * SemanticConstants.MILLISECOND_UNIT;
    }

    public static Date convertDateTimeStrToUTCDate(String date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN_STR);
        simpleDateFormat.setTimeZone(SemanticConstants.UTC_TIMEZONE);
        try {
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            log.warn("Convert date str error, date:{}", date, e);
            throw new RuntimeException(e);
        }
    }

    public static String encodeTxt(int length, String txt) {
        String salt = RandomStringUtils.randomAlphabetic(length);
        return Base64.encodeBase64String((salt + txt + ":" + currentTimeStamp()).getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeTxt(int length, String encodedTxt) {
        String decodeUserPwd = new String(Base64.decodeBase64(encodedTxt), StandardCharsets.UTF_8);
        return decodeUserPwd.substring(length);
    }

    public static boolean checkConnection(String ip, String port) {
        //try to check the state of target
        try (Socket connect = new Socket()) {
            connect.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 100);
            return connect.isConnected() && !connect.isClosed();
        } catch (Exception e) {
            log.error("Connection to {}:{} failed", ip, port);
            return false;
        }
    }

    public static void deleteDir(String dirPath) {
        File file = new File(dirPath);
        if (file.isFile()) {
            file.delete();
            return;
        }
        File[] files = file.listFiles();
        if (files == null) {
            file.delete();
            return;
        }
        for (File value : files) {
            deleteDir(value.getAbsolutePath());
        }
        file.delete();
    }

    public static String genMD5Checksum(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(Files.readAllBytes(file.toPath()));
        byte[] digestBytes = messageDigest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digestBytes) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static boolean isDateString(String dateValue, String dateFormat) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat(dateFormat);
            java.util.Date dd = fmt.parse(dateValue);
            return dateValue.equals(fmt.format(dd));
        } catch (Exception e) {
            return false;
        }
    }

    public static List<File> sortFileListByLastModified(List<File> fileList) {
        if (fileList != null && fileList.size() > 0) {
            fileList.sort((file, newFile) -> Long.compare(newFile.lastModified(), file.lastModified()));
        }
        return fileList;
    }

    public static boolean isTargetFile(Date startDate, File file) {
        Date lastModifiedDate = new java.util.Date(file.lastModified());
        int toStart = lastModifiedDate.compareTo(startDate);
        return toStart > -1;
    }

    public static String endWithSlash(String path) {
        if (path.endsWith("/")) {
            return path;
        } else {
            return path + "/";
        }
    }

    public static String endWithoutSlash(String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        } else {
            return path;
        }
    }

    public static String startWithoutSlash(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        } else {
            return path;
        }
    }

    /**
     * 从 set 中过滤 map 中的 key
     * #    支持 set 中以 * 结尾的通配
     *
     * @param source 原始 Map
     * @param filter 过滤 Set
     * @return
     */
    public static Map<String, String> filterMapByKey(Map<String, String> source, Set<String> filter) {
        Map<String, String> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (filter.contains(entry.getKey())) {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }
        // 支持 * 结尾通配符
        for (String key : filter) {
            if (key.endsWith("*")) {
                String prefix = key.substring(0, key.length() - 1);
                for (Map.Entry<String, String> entry : source.entrySet()) {
                    if (entry.getKey().startsWith(prefix)) {
                        newMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return newMap;
    }

    public static String genMD5(String raw) {
        try {
            MessageDigest messageDigest = messageDigestMD5Instance();
            messageDigest.update(raw.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
