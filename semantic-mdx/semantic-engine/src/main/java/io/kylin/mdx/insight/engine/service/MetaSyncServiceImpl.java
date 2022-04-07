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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.kylin.mdx.insight.common.MdxContext;
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.SemanticUserAndPwd;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.manager.CubeManager;
import io.kylin.mdx.insight.core.manager.GroupManager;
import io.kylin.mdx.insight.core.manager.LicenseManager;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.manager.SegmentManager;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.generic.KylinUserInfo;
import io.kylin.mdx.insight.core.model.semantic.DatasetStatus;
import io.kylin.mdx.insight.core.service.BrokenService;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.service.MetaSyncService;
import io.kylin.mdx.insight.core.service.MetadataService;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.core.support.Execution;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import io.kylin.mdx.insight.core.sync.DatasetEventObject;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.BrokenDataset;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.DatasetChangedSource;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.DatasetEventType;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.DatasetInvalidateSource;
import io.kylin.mdx.insight.core.sync.DatasetValidateResult;
import io.kylin.mdx.insight.core.sync.DatasetValidateResult.DatasetValidateType;
import io.kylin.mdx.insight.core.sync.DatasetValidator;
import io.kylin.mdx.insight.core.sync.EventObject;
import io.kylin.mdx.insight.core.sync.KylinEventObject;
import io.kylin.mdx.insight.core.sync.KylinEventObject.KylinCubeChanged;
import io.kylin.mdx.insight.core.sync.KylinEventObject.KylinEventType;
import io.kylin.mdx.insight.core.sync.Observer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class does the following in the background in an infinite loop:
 * <p>
 * 1. pull the Kylin cube metadata periodically
 * 2. compare the cubes in project to the cache, then fire observers
 * 3. Putting together cube-model and dataset information validates dataset's effective
 * 4. if dataset got something wrong, set this dataset's status to broken
 *
 * @author qi.wu
 */

@Slf4j(topic = "meta.sync")
@Service
public class MetaSyncServiceImpl implements MetaSyncService {
    private static final SemanticConfig SEMANTIC_CONFIG = SemanticConfig.getInstance();

    private List<Observer> observers = new LinkedList<>();

    @Autowired
    private CubeManager cubeManager;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private SegmentManager segmentManager;

    @Autowired
    private UserService userService;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private BrokenService brokenService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private LicenseManager licenseManager;

    @Autowired
    private ModelService modelService;

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void fireObservers(EventObject eventObject) throws SemanticException {
        for (io.kylin.mdx.insight.core.sync.Observer observer : observers) {
            observer.notify(eventObject);
        }
    }

    @Override
    public void fireObserversAsync(EventObject eventObject) {
        for (Observer observer : observers) {
            observer.asyncNotify(eventObject);
        }
    }

    @Override
    public boolean syncCheck() throws SemanticException {
        Execution syncCheckExecution = new Execution("[Meta-sync]  user info check");
        UserInfo userInfo = userService.selectConfUser();
        if (userInfo == null) {
            String user = SemanticUserAndPwd.getUser();
            String decodedPwd = SemanticUserAndPwd.getDecodedPassword();
            log.info("user info not exists in database, please fetch from config");
            if (StringUtils.isBlank(user) || StringUtils.isBlank(decodedPwd)) {
                return false;
            }
            userService.systemAdminCheck(user, decodedPwd);
            userService.updateConfUsr(user, decodedPwd);
            log.info("user info store success from config to database");
        } else {
            log.info("user info exists in database, get it");
            String decodedPassword;
            try {
                decodedPassword = userInfo.getDecryptedPassword();
            } catch (PwdDecryptException e) {
                throw new SemanticException(e);
            }
            userService.systemAdminCheck(userInfo.getUsername(), decodedPassword);
            SEMANTIC_CONFIG.setKylinUser(userInfo.getUsername());
            SEMANTIC_CONFIG.setKylinPwd(decodedPassword);
            log.info("user info store success from database");
        }
        syncCheckExecution.logTimeConsumed(1000, 3000);
        return true;
    }

    @Override
    public void loadKiLicense() throws SemanticException {
        Execution loadLicenseExecution = new Execution("[Meta-sync] load license job");
        licenseManager.init();
        loadLicenseExecution.logTimeConsumed(1000, 3000);
    }

    @Override
    public void syncProjects() throws SemanticException {
        Execution projectChangeExecution = new Execution("[Meta-sync] project-change job");
        log.info("sync projects has started");
        projectManager.verifyProjectListChange();
        projectChangeExecution.logTimeConsumed(500, 1000);
    }

    @Override
    public void syncDataset() throws SemanticException {
        Execution datasetVerifyExecution = new Execution("[Meta-sync] verifying dataset");
        List<BrokenDataset> brokenDatasets = new LinkedList<>();
        List<DatasetEntity> selfFixedDatasets = new LinkedList<>();

        List<String> datasetProjects = datasetService.getProjectsRelatedDataset();
        Set<String> allEffectiveProject = projectManager.getAllProject();
        for (String project : datasetProjects) {
            if (!allEffectiveProject.contains(project)) {
                log.warn("Project: [{}] doesn't exist in Kylin, skip its dataset verify.", project);
                continue;
            }
            List<KylinGenericModel> nocacheModels = cubeManager.getCubeModelByKylin(project);
            if (!MdxContext.isFirstVerify()) {
                if (SemanticConfig.getInstance().isModelVersionVerifyEnable()
                        && !SemanticUtils.anyThingChangedInModels(project, nocacheModels)) {
                    continue;
                }
            }
            DatasetEntity search = new DatasetEntity();
            search.setProject(project);
            List<DatasetEntity> datasetList = datasetService.selectDatasetsBySearch(search);
            boolean cubeChange = false;
            for (DatasetEntity dataset : datasetList) {
                DatasetValidator datasetValidator = new DatasetValidator(dataset, datasetService, nocacheModels, modelService);
                DatasetValidateResult result = datasetValidator.validate();
                if (result.getDatasetValidateType() == DatasetValidateType.SELF_FIX) {
                    cubeChange = true;
                    selfFixedDatasets.add(dataset);
                }
                if (result.getDatasetValidateType() == DatasetValidateType.BROKEN) {
                    cubeChange = true;
                    log.warn("Meta-sync job check dataset broken, info:{}", JSON.toJSONString(dataset));
                    brokenDatasets.add(new BrokenDataset(dataset, result.getBrokenInfo()));
                }
                if (result.getDatasetValidateType() == DatasetValidateType.NORMAL) {
                    if (DatasetStatus.BROKEN.name().equals(dataset.getStatus())) {
                        cubeChange = true;
                        brokenService.recoverOneDatasetNormal(dataset.getId());
                        fireObserversAsync(
                                new DatasetEventObject(
                                        new DatasetChangedSource(dataset.getProject(), dataset.getDataset()),
                                        DatasetEventType.DATASET_RETURN_NORMAL)
                        );
                        log.info("[Meta-sync dataset-verify] Dataset return normal. project:{},dataset:{}",
                                dataset.getProject(), dataset.getDataset());
                    }
                }
            }
            if (cubeChange) {
                fireObservers(new KylinEventObject(new KylinCubeChanged(project, null, nocacheModels), KylinEventType.CUBE_CHANGED));
            }
        }
        if (!Utils.isCollectionEmpty(brokenDatasets)
                || !Utils.isCollectionEmpty(selfFixedDatasets)) {
            fireObserversAsync(new DatasetEventObject(new DatasetInvalidateSource(brokenDatasets, selfFixedDatasets),
                    DatasetEventType.DATASET_INVALIDATED));
        }
        datasetVerifyExecution.logTimeConsumed(2000, 5000);
    }

    @Override
    public void syncCube() throws SemanticException {
        Execution cubeChangeExecution = new Execution("[Meta-sync] cube-monitor job");
        log.info("sync cube has started");
        List<String> cachedProjects = projectManager.getProjectNamesByCache();
        for (String project : cachedProjects) {
            log.info("current project {} is in process of cube.", project);
            Set<String> cubeNamesFromKe = cubeManager.getCubeByKylin(project);
            Set<String> cubeNamesFromCache = cubeManager.getCubeByCache(project);
            List<KylinGenericModel> cubeModelsFromKe = cubeManager.getCubeModelByKylin(project);
            ImmutableSet<String> cacheAdded = compareSet(cubeNamesFromKe, cubeNamesFromCache);
            if (!Utils.isCollectionEmpty(cacheAdded)) {
                fireObservers(new KylinEventObject(
                        new KylinCubeChanged(project, cacheAdded, cubeModelsFromKe), KylinEventType.CUBE_NEWED));
            }

            ImmutableSet<String> cacheDeleted = compareSet(cubeNamesFromCache, cubeNamesFromKe);
            if (!Utils.isCollectionEmpty(cacheDeleted)) {
                fireObservers(new KylinEventObject(
                        new KylinCubeChanged(project, cacheDeleted, cubeModelsFromKe), KylinEventType.CUBE_DELETED));
            }
        }
        cubeChangeExecution.logTimeConsumed(1000, 3000);
    }

    @Override
    public void syncUser() throws SemanticException {
        Execution syncUserCExecution = new Execution("[Meta-sync] sync user job");
        log.info("sync user has started");
        List<KylinUserInfo> users = userService.getUsersByKylin();
        userService.saveUsersToCache(users);
        Set<String> userNames = userService.getUsersNameByKylin();
        Set<String> userNamesByDatabase = userService.getUsersNameByDatabase();
        ImmutableSet<String> userAdded = compareSet(userNames, userNamesByDatabase);

        if (!Utils.isCollectionEmpty(userAdded)) {
            List<UserInfo> userInfos = userAdded.stream().map(UserInfo::new).collect(Collectors.toList());
            userService.insertUsers(userInfos);
        }

        ImmutableSet<String> userDeleted = compareSet(userNamesByDatabase, userNames);
        if (!Utils.isCollectionEmpty(userDeleted)) {
            List<String> deleteUsers = userDeleted.stream().map(String::toUpperCase).collect(Collectors.toList());
            userService.deleteUsers(deleteUsers);
        }
        syncUserCExecution.logTimeConsumed(1000, 3000);
    }

    @Override
    public void syncGroup() {
        Execution syncGroupExecution = new Execution("[Meta-sync] sync group job");
        log.info("sync group has started");
        List<String> groups = groupManager.getGroupByKe();
        groupManager.saveGroups(groups);
        syncGroupExecution.logTimeConsumed(1000, 3000);
    }

    @Override
    public void syncSegment() throws SemanticException {
        Execution syncSegmentExecution = new Execution("[Meta-sync] sync segment job");
        List<String> datasetProjects = datasetService.getProjectsRelatedDataset();
        Set<String> allEffectiveProject = projectManager.getAllProject();
        for (String project : datasetProjects) {
            if (!allEffectiveProject.contains(project)) {
                log.warn("Project: [{}] doesn't exist in Kylin, skip its dataset verify.", project);
                continue;
            }
            Set<String> cacheSegment = segmentManager.getSegmentByCache(project);
            Set<String> noCacheSegment = segmentManager.getSegmentByKylin(project);
            if (cacheSegment == null) {
                segmentManager.saveSegment(project, noCacheSegment);
                continue;
            }
            ImmutableSet<String> changeSegment = Sets.symmetricDifference(cacheSegment, noCacheSegment).immutableCopy();
            if (changeSegment.size() > 0) {
                clearProjectCache(project);
                segmentManager.saveSegment(project, noCacheSegment);
            }
        }
        syncSegmentExecution.logTimeConsumed(1000, 3000);

    }

    @Override
    public void clearProjectCache(String project) {
        HttpHeaders requestHeaders = new HttpHeaders();
        String userPwd = Utils.buildBasicAuth(SemanticUserAndPwd.getUser(), SemanticUserAndPwd.getDecodedPassword());
        requestHeaders.add("Authorization", userPwd);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, requestHeaders);
        String url = SemanticConfig.getInstance().getClearCacheUrl(project);
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            log.error("Clear {} cache failed, please check the MDX server、config user or password is normal.", project);
            log.error("current response: {}", responseEntity.getBody());
        } else {
            log.info("Clear {} cache success.", project);
        }

    }

    private ImmutableSet<String> compareSet(Set<String> set1, Set<String> set2) {
        return Sets.difference(set1, set2).immutableCopy();
    }

    @Override
    public void syncProjectAclChange() {
        Execution execution = new Execution("[Meta-sync] acl change monitor job");
        log.info("sync project acl has start");
        Set<String> projects = projectManager.getAllProject();
        for (String project : projects) {
            for (String realUser : userService.getUsersByProjectFromCache(project)) {
                projectManager.doProjectAcl(realUser, project);
            }
        }
        execution.logTimeConsumed(2000, 5000);
    }

}
