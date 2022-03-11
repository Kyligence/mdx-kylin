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


package io.kylin.mdx.insight.common;

import io.kylin.mdx.insight.common.util.IOUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
class SemanticConfigBase {

    private static final String PROPERTIES_FILE = "insight.properties";

    private static final String OVERRIDE_PROPERTIES_FILE = "insight.override.properties";

    private static final String MDX_HOME = "MDX_HOME";

    private static final String INSIGHT_HOME = "INSIGHT_HOME";

    @Getter
    private final Properties properties = new Properties();

    private String kylinUsr;

    private String kylinPwd;

    public String getKylinUser() {
        return kylinUsr;
    }

    public void setKylinUser(String usr) {
        this.kylinUsr = usr;
    }

    public String getKylinPwd() {
        return this.kylinPwd;
    }

    public void setKylinPwd(String pwd) {
        this.kylinPwd = pwd;
    }

    public void loadConfig() {
        File propFile = getPropertiesFile();
        if (!propFile.exists()) {
            log.error("fail to locate " + propFile.getAbsolutePath());
            throw new RuntimeException("fail to locate " + propFile.getAbsolutePath());
        }
        Properties conf = new Properties();
        try (FileInputStream is = new FileInputStream(propFile)) {
            conf.load(is);
            File propOverrideFile = new File(propFile.getParentFile(), propFile.getName() + ".override");
            if (propOverrideFile.exists()) {
                try (FileInputStream ois = new FileInputStream(propOverrideFile)) {
                    Properties propOverride = new Properties();
                    propOverride.load(ois);
                    conf.putAll(propOverride);
                }
            }
        } catch (IOException e) {
            log.error("fail to load " + propFile.getAbsolutePath());
            throw new RuntimeException("fail to load " + propFile.getAbsolutePath(), e);
        }
        this.properties.clear();
        this.properties.putAll(conf);
    }

    public void initConfig() {
        this.kylinUsr = getOptional(SemanticConstants.KYLIN_USERNAME, "");
        this.kylinPwd = getOptional(SemanticConstants.KYLIN_PASSWORD, "");
    }

    public String getInsightHome() {
        String insightHome = System.getProperty(INSIGHT_HOME);
        if (StringUtils.isNotBlank(insightHome)) {
            return insightHome;
        }
        insightHome = System.getenv(INSIGHT_HOME);
        if (StringUtils.isNotBlank(insightHome)) {
            return insightHome;
        }
        try {
            insightHome = new File("../").getCanonicalPath();
            log.warn(INSIGHT_HOME + " property hasn't been set, use path:{}", insightHome);
        } catch (IOException e) {
            log.error("Find INSIGHT_HOME error", e);
        }
        return insightHome;
    }

    public String getMdxHome() {
        String mdxHome = System.getProperty(MDX_HOME);
        if (StringUtils.isBlank(mdxHome)) {
            mdxHome = System.getenv(MDX_HOME);
            if (StringUtils.isBlank(mdxHome)) {
                throw new RuntimeException("Didn't find " + MDX_HOME + " in system property or system env, please set");
            }
        }
        return mdxHome;
    }

    public String getPropertiesDirPath() {
        return getMdxHome() + File.separator + "conf";
    }

    private File getPropertiesFile() {
        String path = getPropertiesDirPath();
        File overrideFile = new File(path, OVERRIDE_PROPERTIES_FILE);
        if (overrideFile.exists()) {
            return overrideFile;
        } else {
            return new File(path, PROPERTIES_FILE);
        }
    }

    public FileInputStream getPropertiesFileInputStream() {
        File propFile = getPropertiesFile();
        if (!propFile.exists()) {
            log.error("fail to locate " + PROPERTIES_FILE);
            throw new RuntimeException("fail to locate " + PROPERTIES_FILE);
        }
        return IOUtils.open(propFile);
    }

    public String getOptional(String propertyKey, String defaultValue) {
        String property = System.getProperty(propertyKey);
        if (StringUtils.isNotBlank(property)) {
            return property.trim();
        }
        property = properties.getProperty(propertyKey);
        if (StringUtils.isBlank(property)) {
            return defaultValue.trim();
        } else {
            return property.trim();
        }
    }

    public int getIntValue(String propertyKey, int defaultValue) {
        String value = getOptional(propertyKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.error("Parse exception, property:{}, use default value: {}", propertyKey, defaultValue);
            return defaultValue;
        }
    }

    public long getLongValue(String propertyKey, long defaultValue) {
        String value = getOptional(propertyKey, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            log.error("Parse exception, property:{}, use default value: {}", propertyKey, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBooleanValue(String propertyKey, boolean defaultValue) {
        String value = getOptional(propertyKey, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

}
