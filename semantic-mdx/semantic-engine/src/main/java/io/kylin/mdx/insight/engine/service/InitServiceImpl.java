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

import com.alibaba.fastjson.JSON;
import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.insight.common.MdxContext;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.SemanticUserAndPwd;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.sync.*;
import io.kylin.mdx.insight.core.service.*;
import io.kylin.mdx.insight.core.sync.MetaSyncScheduleTask;
import io.kylin.mdx.insight.engine.manager.SyncManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.kylin.mdx.insight.common.constants.ConfigConstants.KYLIN_HOST;
import static io.kylin.mdx.insight.common.constants.ConfigConstants.KYLIN_PORT;
import static io.kylin.mdx.insight.common.constants.ConfigConstants.KYLIN_PASSWORD;
import static io.kylin.mdx.insight.common.constants.ConfigConstants.KYLIN_USERNAME;
import static io.kylin.mdx.insight.common.constants.ConfigConstants.DATASET_ALLOW_ACCESS_BY_DEFAULT;
import static io.kylin.mdx.insight.common.constants.ConfigConstants.DATASET_ALLOW_MAX_SIZE_EXPORT_FILE;

/**
 * @author qi.wu
 */
@Slf4j
@Service
public class InitServiceImpl implements InitService {

    private static final SemanticConfig SEMANTIC_CONFIG = SemanticConfig.getInstance();

    private static final String KYLIN_STATUS = "insight.kylin.status";

    private static final String KYLIN_LAST_UPDATED = "insight.kylin.last_updated";

    private static final int CONF_LEN = 2;

    private final MetaStore metaStore = MetaStore.getInstance();

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private UserService userService;

    @Autowired
    private MdxQueryService mdxQueryService;

    @Autowired
    private SqlQueryService sqlQueryService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private MetaSyncService metaSyncService;

    @Autowired
    SyncManager syncManager;

    @Override
    public boolean sync() throws SemanticException {
        if (MdxContext.getAndSetInitStatus(true, false) && !SEMANTIC_CONFIG.isConvertorMock()) {
            startMetaMonitor();
        }
        if (!metaSyncService.syncCheck()) {
            return false;
        }
        // TODO: remove this
        // metaSyncService.loadKiLicense();
        loadProjectList();
        loadAllProjectModelsRelateDataset();
        MdxContext.getAndSetSyncStatus(false, true);
        return true;
    }

    @Override
    public void startQueryLogPersistence() {
        QueryLogPersistence.INSTANCE = new QueryLogPersistence(mdxQueryService, sqlQueryService);
        QueryLogPersistence.INSTANCE.start();
    }

    @Override
    public boolean startQueryLogHousekeep() {
        int mdxQueryMaxRows = SEMANTIC_CONFIG.getMdxQueryHousekeepMaxRows();
        if (mdxQueryMaxRows > 0) {
            MetaHousekeep metaHousekeep = new MetaHousekeep(mdxQueryService, mdxQueryMaxRows);
            metaHousekeep.start();
        }
        return true;
    }

    @Override
    public Map<String, String> getConfigurations() {
        Map<String, String> confMap = new LinkedHashMap<>(10);
        if (MdxContext.isSyncStatus() && !SEMANTIC_CONFIG.isConvertorMock()) {
            try {
                sync();
            } catch (SemanticException s) {
                // 此处失败不影响
            }
        }
        // MDX 同步信息
        confMap.put(KYLIN_HOST, SEMANTIC_CONFIG.getKylinHost());
        confMap.put(KYLIN_PORT, SEMANTIC_CONFIG.getKylinPort());
        confMap.put(KYLIN_USERNAME, SEMANTIC_CONFIG.getKylinUser());
        confMap.put(KYLIN_LAST_UPDATED, metaStore.getLastUpdateTime().toString());
        confMap.put(KYLIN_STATUS, MdxContext.getSyncStatus());
        // 补充配置信息
        confMap.put(DATASET_ALLOW_ACCESS_BY_DEFAULT, String.valueOf(SEMANTIC_CONFIG.isDatasetAccessByDefault()));
        confMap.put(DATASET_ALLOW_MAX_SIZE_EXPORT_FILE, String.valueOf(DataSize.parse(SEMANTIC_CONFIG.getExportFileMaxSize()).toBytes()));
        return confMap;
    }

    @Override
    public String updateConfigurations(Map<String, String> confMap) throws SemanticException {
        String username;
        String password;
        if (confMap.size() == CONF_LEN && confMap.containsKey(KYLIN_USERNAME) && confMap.containsKey(KYLIN_PASSWORD)) {
            username = confMap.get(KYLIN_USERNAME);
            log.info("Attempting to update synchronizing user, new synchronizing user name: {}", username);
            password = new String(Base64.decodeBase64(confMap.get(KYLIN_PASSWORD)
                    .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            userService.systemAdminCheck(username, password);
        } else {
            throw new SemanticException(ErrorCode.INVALIDATE_SYNC_INFO);
        }
        userService.updateConfUsr(username, password);
        SEMANTIC_CONFIG.setKylinUser(username);
        SEMANTIC_CONFIG.setKylinPwd(password);
        if (MdxContext.isInitStatus()) {
            sync();
        } else {
            restartSync();
        }
        return SemanticConstants.RESP_SUC;
    }

    @Override
    public String restartSync() throws SemanticException {
        userService.systemAdminCheck(SemanticUserAndPwd.getUser(), SemanticUserAndPwd.getDecodedPassword());
        sync();
        return SemanticConstants.RESP_SUC;
    }

    @Override
    public void loadProjectList() throws SemanticException {
        projectManager.initLoadProjectList();
    }

    private void loadAllProjectModelsRelateDataset() throws SemanticException {
        long start = System.currentTimeMillis();
        try {
            List<String> projectNames = datasetService.getProjectsRelatedDataset();
            log.info("Models init process started, projects:{}", JSON.toJSONString(projectNames));
            for (String projectName : projectNames) {
                modelService.loadGenericModels(projectName);
            }
        } catch (Throwable e) {
            // 项目在 KYLIN 中不存在或发生其他异常，不影响同步线程启动
            log.error("loadGenericModels catch exception:", e);
        }
        log.info("Models have loaded successfully, elapsed time:{}", (System.currentTimeMillis() - start));
    }

    private void startMetaMonitor() {
        if (SEMANTIC_CONFIG.isDatasetVerifyEnable()) {
            metaSyncService.addObserver(syncManager);
            MetaSyncScheduleTask metaSyncScheduleTask = new MetaSyncScheduleTask(metaSyncService, metadataService, metaStore);
            metaSyncScheduleTask.start();
        }
        syncManager.start();
    }

}
