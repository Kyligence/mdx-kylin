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


package io.kylin.mdx.insight.engine.manager;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.dao.MdxInfoMapper;
import io.kylin.mdx.insight.core.entity.MdxInfo;
import io.kylin.mdx.insight.core.manager.LicenseManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class LicenseManagerImpl implements LicenseManager {

    @Autowired
    private MdxInfoMapper mdxInfoMapper;

    private static final String VERSION_FILE_NAME = "VERSION";
    private static final String COMMIT_FILE_NAME = "commit_SHA1";

    private static final String ONLY_MDX_TXT = "only mdx";
    private static final String[] ONLY_MDX_ANALYSIS = {"mdx"};
    private static final String[] DEFAULT_ANALYSIS = {"sql", "mdx"};

    private static final String DEFAULT_LICENSE_UNDERLINE = "2029-01-01 23:59:59";

    private static final int MDX_VERSION_ID = 1;

    /**
     * evaluation |  professional |  premium
     */
    private String kiType = "evaluation";

    private String liveDateRange = "2019-06-01, 2099-01-01";

    private Date underlineDate;

    private String[] analyticTypes = DEFAULT_ANALYSIS;

    /**
     * read from VERSION in root dir
     */
    private String kiVersion;

    private String commitId;

    private final AtomicBoolean startupFirstInit = new AtomicBoolean(true);

    public LicenseManagerImpl() {
    }

    @Override
    public void init() throws SemanticException {
        this.kiVersion = getFileContent(VERSION_FILE_NAME);
        this.commitId = getFileContent(COMMIT_FILE_NAME);
        this.underlineDate = parseKiLicenseUnderLine();

        if (startupFirstInit.get()) {
            updateMdxVersion(kiVersion);
            startupFirstInit.getAndSet(false);
        }
    }

    @Override
    public void updateMdxVersion(String mdxVersion) {
        if (!StringUtils.isNotBlank(mdxVersion)) {
            return;
        }
        if (mdxInfoMapper == null) {
            return;
        }
        MdxInfo mdxInfo = mdxInfoMapper.selectByPrimaryKey(MDX_VERSION_ID);
        if (mdxInfo == null) {
            MdxInfo insertMdxInfo = new MdxInfo(MDX_VERSION_ID, mdxVersion, Utils.currentTimeStamp(), Utils.currentTimeStamp());
            mdxInfoMapper.insert(insertMdxInfo);
            return;
        }
        MdxInfo upgradeMdxInfo = new MdxInfo(mdxInfo.getId(), mdxVersion, mdxInfo.getCreateTime(), Utils.currentTimeStamp());
        mdxInfoMapper.updateByPrimaryKey(upgradeMdxInfo);
    }


    @Override
    public String getFileContent(String filename) {
        String path = SemanticConfig.getInstance().getInsightHome();

        if (StringUtils.isBlank(path)) {
            return "UNKNOWN";
        }

        File file = new File(path, filename);
        if (!file.exists()) {
            return "UNKNOWN";
        }

        try (InputStream input = new FileInputStream(file)) {
            return IOUtils.toString(input);
        } catch (IOException e) {
            log.warn("Read version file error", e);
            return "UNKNOWN";
        }

    }

    @Override
    public int getUserLimit() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getKiType() {
        return kiType;
    }

    @Override
    public String getKiVersion() {
        return kiVersion;
    }

    @Override
    public String getCommitId() {
        return commitId;
    }

    @Override
    public String[] getAnalyticTypes() {
        return analyticTypes;
    }

    @Override
    public String getDefaultLiveDateRange() {
        return this.liveDateRange;
    }

    @Override
    public String getLiveDateRange() {
        return this.liveDateRange;
    }

    private Date parseKiLicenseUnderLine() {
        String[] split = this.liveDateRange.split(SemanticConstants.COMMA);
        final int DATE_PART_SIZE = 2;
        if (split.length != DATE_PART_SIZE) {
            return Utils.convertDateTimeStrToUTCDate(DEFAULT_LICENSE_UNDERLINE);
        }

        String underline = split[1];

        try {
            return Utils.convertDateTimeStrToUTCDate(underline + " " + SemanticConstants.MAX_TIME_DAY_STR);
        } catch (Exception e) {
            log.warn("Parse date time error, underline:{}", underline);
            return Utils.convertDateTimeStrToUTCDate(DEFAULT_LICENSE_UNDERLINE);
        }
    }
}
