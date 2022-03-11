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


package io.kylin.mdx.insight.core.meta;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.async.AsyncService;
import io.kylin.mdx.insight.common.async.BatchTaskExecutor;
import io.kylin.mdx.insight.common.http.HttpUri;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.manager.AsyncManager;
import io.kylin.mdx.insight.core.meta.acl.AclConvertor;
import io.kylin.mdx.insight.core.meta.acl.KylinAclConvertor;
import io.kylin.mdx.insight.core.model.acl.AclProjectModel;
import io.kylin.mdx.insight.core.model.generic.ActualTable;
import io.kylin.mdx.insight.core.model.generic.ActualTableCol;
import io.kylin.mdx.insight.core.model.generic.ColumnIdentity;
import io.kylin.mdx.insight.core.model.generic.ColumnInfo;
import io.kylin.mdx.insight.core.model.generic.CubeMeasure;
import io.kylin.mdx.insight.core.model.generic.HierachyInfo;
import io.kylin.mdx.insight.core.model.generic.JoinTableInfo;
import io.kylin.mdx.insight.core.model.generic.JoinTableInfo.JoinCondition;
import io.kylin.mdx.insight.core.model.generic.JoinTableInfo.TableNode;
import io.kylin.mdx.insight.core.model.generic.JoinTableInfo.TwoJoinTable;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.generic.KylinUserInfo;
import io.kylin.mdx.insight.core.model.generic.RawJoinTable;
import io.kylin.mdx.insight.core.model.kylin.KylinBeanWrapper;
import io.kylin.mdx.insight.core.model.kylin.KylinColumnDesc;
import io.kylin.mdx.insight.core.model.kylin.KylinCubeDesc;
import io.kylin.mdx.insight.core.model.kylin.KylinDataModelDesc;
import io.kylin.mdx.insight.core.model.kylin.KylinDataModelDesc.DataModel;
import io.kylin.mdx.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j(topic = "http.call")
public class KylinConvertor extends AbstractConvertor<KylinBeanWrapper> {

    private static final String COLUMN_DERIVED_FK = "{FK}";

    private static final String STATUS = "READY";

    private final AclConvertor aclConvertor = new KylinAclConvertor(this);

    @Override
    public KylinGenericModel convert(KylinBeanWrapper kylinBean) {
        // TODO: adapt
        KylinColumnDesc columnDesc = kylinBean.getColumnDesc();
        KylinCubeDesc cubeDesc = kylinBean.getCubeDesc();
        KylinDataModelDesc modelDesc = kylinBean.getModelDesc();
        DataModel model = modelDesc.getModels().get(0);

        KylinGenericModel genericModel = new KylinGenericModel();

        genericModel.setModelName(cubeDesc.getName());
        genericModel.setLastModified(cubeDesc.getLast_modified());
        genericModel.setRawJoinTableInfo(model.getLookups());
        genericModel.setSignature(cubeDesc.getSignature());

        //事实表就以表名为别名
        ActualTable factTable = new ActualTable(model.getFact_table());
        genericModel.setFactTable(new MutablePair<>(factTable.getTableName(), factTable));

        buildActualTableAndTableAlias(model, genericModel);
        buildTableRelations(model, genericModel);
        buildActCol2ColInfo(columnDesc, genericModel);
        buildModelDimensions(model, genericModel);
        buildModelMeasures(model, genericModel);
        buildCubeDimensions(cubeDesc, genericModel);
        buildCubeMeasures(cubeDesc, genericModel);
        buildCubeHierarchy(cubeDesc, genericModel);
        return genericModel;
    }

    private void buildCubeHierarchy(KylinCubeDesc cubeDesc, KylinGenericModel kylinGenericModel) {
        List<HierachyInfo> hierarchyInfos = new ArrayList<>();
        for (KylinCubeDesc.AggregationGroup aggGroup : cubeDesc.getAggregation_groups()) {
            aggGroup.getSelect_rule()
                    .getHierarchy_dims()
                    .forEach(hierarchy -> {
                        List<ColumnIdentity> hierarchyInfo = hierarchy.stream()
                                .map(ColumnIdentity::new)
                                .collect(Collectors.toList());
                        hierarchyInfos.add(new HierachyInfo(hierarchyInfo));
                    });
        }
        kylinGenericModel.setHierachyInfos(hierarchyInfos);
    }

    private void buildCubeMeasures(KylinCubeDesc cubeDesc, KylinGenericModel kylinGenericModel) {
        List<CubeMeasure> cubeMeasures = new ArrayList<>();
        for (KylinCubeDesc.Measure measure : cubeDesc.getMeasures()) {
            KylinCubeDesc.Measure.Function function = measure.getFunction();

            ColumnIdentity colIdentity = null;
            if (!SemanticConstants.FUNCTION_CONSTANT_TYPE.equals(function.getParameter().getType())) {
                colIdentity = new ColumnIdentity(function.getParameter().getValue());
            } else {
                colIdentity = new ColumnIdentity(SemanticConstants.FUNCTION_CONSTANT_TYPE, "1");
            }
            cubeMeasures.add(new CubeMeasure(measure.getName(),
                    measure.getDescription(),
                    colIdentity,
                    function.getReturntype(),
                    function.getExpression()));
        }
        kylinGenericModel.setCubeMeasures(cubeMeasures);
    }

    private void buildCubeDimensions(KylinCubeDesc cubeDesc, KylinGenericModel kylinGenericModel) {
        Set<ColumnIdentity> cubeDimensions = new TreeSet<>();
        for (KylinCubeDesc.Dimension dimension : cubeDesc.getDimensions()) {

            String column = dimension.getColumn();
            if (!StringUtils.isBlank(column) && !COLUMN_DERIVED_FK.equalsIgnoreCase(column)) {
                cubeDimensions.add(new ColumnIdentity(dimension.getTable(), column, dimension.getName()));
            } else {
                List<String> columnNames = findColumnName(dimension);
                if (columnNames != null && columnNames.size() == 1) {
                    cubeDimensions.add(new ColumnIdentity(dimension.getTable(), columnNames.get(0), dimension.getName()));
                } else if (columnNames != null) {
                    columnNames.forEach(columnName -> cubeDimensions.add(new ColumnIdentity(dimension.getTable(), columnName, columnName)));
                }
            }
        }

        kylinGenericModel.setCubeDimensions(cubeDimensions);
    }

    private List<String> findColumnName(KylinCubeDesc.Dimension dimension) {
        List<String> derived = dimension.getDerived();
        if (Utils.isCollectionEmpty(derived)) {
            return Collections.emptyList();
        }
        return derived;
    }

    private void buildModelMeasures(DataModel model, KylinGenericModel kylinGenericModel) {
        Set<ColumnIdentity> modelMeasures = new TreeSet<>();
        for (String tblAliasWithCol : model.getMetrics()) {

            modelMeasures.add(new ColumnIdentity(tblAliasWithCol));
        }
        kylinGenericModel.setModelMeasures(modelMeasures);
    }

    private void buildModelDimensions(DataModel model, KylinGenericModel kylinGenericModel) {
        Set<ColumnIdentity> modelDimensions = new TreeSet<>();
        for (DataModel.Dimension dimension : model.getDimensions()) {
            String tblAlias = dimension.getTable();
            dimension.getColumns()
                    .forEach(colName -> {
                        modelDimensions.add(new ColumnIdentity(tblAlias, colName));
                    });
        }
        kylinGenericModel.setModelDimensions(modelDimensions);
    }

    private void buildActCol2ColInfo(KylinColumnDesc columnDesc, KylinGenericModel kylinGenericModel) {
        Map<ActualTableCol, ColumnInfo> actCol2ColInfo = new HashMap<>();
        for (KylinColumnDesc.Table tableMeta : columnDesc.getData()) {

            ActualTable actTbl = new ActualTable(tableMeta.getTable_SCHEM(), tableMeta.getTable_NAME());

            tableMeta.getColumns()
                    .forEach(column -> {
                        ColumnInfo columnInfo = new ColumnInfo(column.getData_TYPE());
                        actCol2ColInfo.put(new ActualTableCol(actTbl, column.getColumn_NAME()), columnInfo);
                    });
        }
        kylinGenericModel.setActCol2ColInfo(actCol2ColInfo);
    }

    private void buildTableRelations(DataModel model, KylinGenericModel kylinGenericModel) {
        JoinTableInfo joinTableInfo = new JoinTableInfo();
        Map<TwoJoinTable, JoinCondition> joinConditionMap = new HashMap<>();
        if (model.getLookups() == null || model.getLookups().size() == 0) {
            joinTableInfo.setRootTableNode(new TableNode(kylinGenericModel.getFactTable().getLeft()));
            joinTableInfo.setJoinConditionMap(joinConditionMap);
            kylinGenericModel.setJoinTableInfo(joinTableInfo);
            return;
        }

        Map<String, TableNode> tableNodeMap = new HashMap<>();
        for (RawJoinTable lookup : model.getLookups()) {
            RawJoinTable.Join join = lookup.getJoin();
            List<String> primaryKeys = join.getPrimary_key();
            List<String> foreignKeys = join.getForeign_key();

            String leftTable = StringUtils.substringBefore(foreignKeys.get(0), ".");
            String rightTable = StringUtils.substringBefore(primaryKeys.get(0), ".");

            TableNode leftTableNode = tableNodeMap.get(leftTable);
            if (leftTableNode == null) {
                leftTableNode = new TableNode(leftTable);
                tableNodeMap.put(leftTable, leftTableNode);
            }
            TableNode rightTableNode = tableNodeMap.get(rightTable);
            if (rightTableNode == null) {
                rightTableNode = new TableNode(rightTable);
                tableNodeMap.put(rightTable, rightTableNode);
            }
            rightTableNode.addPrimaryKey(primaryKeys);

            leftTableNode.addChildNode(rightTableNode);

            TwoJoinTable twoJoinTable = new TwoJoinTable(leftTable, rightTable);
            JoinCondition joinCondition = new JoinCondition(join.getType());
            for (int i = 0; i < primaryKeys.size(); i++) {
                joinCondition.addJoinCondition(foreignKeys.get(i), primaryKeys.get(i));
            }

            joinConditionMap.put(twoJoinTable, joinCondition);
        }

        joinTableInfo.setRootTableNode(tableNodeMap.get(kylinGenericModel.getFactTable().getLeft()));
        joinTableInfo.setJoinConditionMap(joinConditionMap);
        kylinGenericModel.setJoinTableInfo(joinTableInfo);
    }

    private void buildActualTableAndTableAlias(DataModel model, KylinGenericModel kylinGenericModel) {
        MutablePair<String, ActualTable> factTablePair = kylinGenericModel.getFactTable();

        Map<ActualTable, List<String>> actTbl2TblAliases = new HashMap<>();
        Map<String, ActualTable> tblAlias2ActTbl = new HashMap<>();
        tblAlias2ActTbl.put(factTablePair.getLeft(), factTablePair.getRight());
        actTbl2TblAliases.put(factTablePair.getRight(), Arrays.asList(factTablePair.getLeft()));

        for (RawJoinTable lookup : model.getLookups()) {
            String tblAlias = lookup.getAlias();

            ActualTable actTbl = new ActualTable(lookup.getTable());
            tblAlias2ActTbl.put(tblAlias, actTbl);

            List<String> tblAliases = Optional
                    .ofNullable(actTbl2TblAliases.get(actTbl))
                    .orElseGet(() -> {
                        List<String> aliases = new ArrayList<>();
                        actTbl2TblAliases.put(actTbl, aliases);
                        return aliases;
                    });
            tblAliases.add(tblAlias);
        }
        kylinGenericModel.setTblAlias2ActTbl(tblAlias2ActTbl);
        kylinGenericModel.setActTbl2TblAliases(actTbl2TblAliases);
    }

    @Override
    public List<String> getCubeNames(ConnectionInfo connInfo) throws SemanticException {
        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());
        return getCubeNames(connInfo.getProject(), auth);
    }

    @Override
    public List<KylinBeanWrapper> buildDatasourceModel(ConnectionInfo connInfo) {
        List<KylinBeanWrapper> kylinBeanWrappers = new LinkedList<>();

        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());
        try {
            List<String> cubeNames = getCubeNames(connInfo.getProject(), auth);
            KylinColumnDesc columnDesc = getColumnDescByHttp(connInfo.getProject(), auth);

            for (String cubeName : cubeNames) {
                KylinCubeDesc cubeDesc = getCubeDescByHttp(cubeName, connInfo.getProject(), auth);
                KylinDataModelDesc modelDesc = getModelDescByHttp(cubeDesc.getModel_name(), connInfo.getProject(), auth);

                kylinBeanWrappers.add(new KylinBeanWrapper(columnDesc, modelDesc, cubeDesc));
            }
            return kylinBeanWrappers;
        } catch (SemanticException e) {
            log.error("The mdx-service inits KYLIN datasource to get semantic exception", e);
            return kylinBeanWrappers;
        } catch (Exception e) {
            log.error("The mdx-service inits KYLIN datasource to get unknown exception", e);
            return kylinBeanWrappers;
        }
    }

    private List<String> getCubeNames(String projectName, byte[] auth) throws SemanticException {
        String url = HttpUri.getKylinProjectCubesAPI(projectName);

        String content;
        if (SemanticConfig.getInstance().isConvertorMock()) {
            content = getCubeNames(projectName);
        } else {
            content = doHttpCall(url, auth);
        }
        JSONObject jsonObject = JSONObject.parseObject(content);
        validateRespSuccess(jsonObject);

        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("cubes");

        List<String> validCubes = new ArrayList<>(4);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject cubeJson = jsonArray.getJSONObject(i);
            if (!STATUS.equals(cubeJson.getString("status")) || cubeJson.getBooleanValue("is_draft")) {
                continue;
            }
            validCubes.add(cubeJson.getString("name"));
        }

        return validCubes;
    }

    @Override
    public List<String> getSegments(ConnectionInfo connInfo) throws SemanticException {

        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());

        String url = HttpUri.getKylinProjectCubesAPI(connInfo.getProject());

        String content;
        if (SemanticConfig.getInstance().isConvertorMock()) {
            content = getCubeNames(connInfo.getProject());
        } else {
            content = doHttpCall(url, auth);
        }

        JSONObject jsonObject = JSONObject.parseObject(content);
        validateRespSuccess(jsonObject);

        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("cubes");

        List<String> validSegments = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject cubeJson = jsonArray.getJSONObject(i);

            if (STATUS.equals(cubeJson.getString("status"))
                    && !cubeJson.getBooleanValue("is_draft")) {
                JSONArray segments = cubeJson.getJSONArray("segments");
                for (int j = 0; j < segments.size(); j++) {
                    JSONObject segment = segments.getJSONObject(j);
                    validSegments.add(segment.getString("uuid"));
                }
            }
        }
        return validSegments;
    }

    @Override
    public Map<String, Long> getHighCardinalityDimension(ConnectionInfo connInfo) throws SemanticException {

        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());

        String url = HttpUri.getKylinTableInfoUrI(connInfo.getProject());

        String content = doHttpCall(url, auth);

        JSONObject jsonObject = JSONObject.parseObject(content);
        validateRespSuccess(jsonObject);
        Map<String, Long> highCardinalityDimensionMap = new ConcurrentHashMap<>();
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject tableObject = jsonArray.getJSONObject(i);
            String databaseName = tableObject.getString("database");
            String tableName = tableObject.getString("name");
            JSONObject columnCardinality = tableObject.getJSONObject("cardinality");
            for (String columnName : columnCardinality.keySet()) {
                String key = "[" + databaseName + "]." + "[" + tableName + "]." + "[" + columnName + "]";
                Long value = columnCardinality.getLong(columnName) == null ? 0 : columnCardinality.getLong(columnName);
                highCardinalityDimensionMap.put(key, value);
            }
        }
        return highCardinalityDimensionMap;
    }

    private KylinCubeDesc getCubeDescByHttp(String cubeName, String projectName, byte[] auth) throws SemanticException {
        String url = HttpUri.getKylinCubeDesc(cubeName);

        String content;
        if (SemanticConfig.getInstance().isConvertorMock()) {
            content = getCubeDescByHttp(cubeName, projectName);
        } else {
            content = doHttpCall(url, auth);
        }

        try {
            JSONObject jsonObject = JSONObject.parseObject(content);
            validateRespSuccess(jsonObject);

            JSONObject cubeJson = jsonObject.getJSONObject("data");
            return cubeJson.toJavaObject(KylinCubeDesc.class);

        } catch (Exception e) {
            log.error("The http call KYLIN 4 API [getKylinCubeDesc] get exception, response body:{}", content);
            throw new SemanticException("Http: getKylinCubeDesc get exception", e, ErrorCode.FETCH_KYLIN_MODEL_INFO_ERROR);
        }
    }

    private KylinColumnDesc getColumnDescByHttp(String projectName, byte[] auth) throws SemanticException {
        String url = HttpUri.getKylinProjectColumnDescAPI(projectName);

        String content;
        if (SemanticConfig.getInstance().isConvertorMock()) {
            content = getColumnDescByHttp(projectName);
        } else {
            content = doHttpCall(url, auth);
        }

        try {
            JSONObject jsonObject = JSONObject.parseObject(content);
            validateRespSuccess(jsonObject);
            return jsonObject.toJavaObject(KylinColumnDesc.class);
        } catch (Exception e) {
            log.error("The http call KYLIN 4 API [getColumnDescByHttp] get exception, response body:{}", content);
            throw new SemanticException("Http: getColumnDescByHttp get exception", e, ErrorCode.FETCH_KYLIN_DIMS_ERROR);
        }
    }

    private KylinDataModelDesc getModelDescByHttp(String modelName, String projectName, byte[] auth) throws SemanticException {
        String url = HttpUri.getKylinModelsAPI(modelName, projectName);

        String content;
        if (SemanticConfig.getInstance().isConvertorMock()) {
            content = getModelDescByHttp(modelName, projectName);
        } else {
            content = doHttpCall(url, auth);
        }

        try {
            JSONObject jsonObject = JSONObject.parseObject(content);
            validateRespSuccess(jsonObject);
            JSONObject cubeJson = jsonObject.getJSONObject("data");
            return cubeJson.toJavaObject(KylinDataModelDesc.class);
        } catch (Exception e) {
            log.error("The http call Kylin 4 API [getModelDescByHttp] get exception, response body:{}", content);
            throw new SemanticException("Http: getModelDescByHttp get exception", e, ErrorCode.FETCH_KYLIN_MODEL_INFO_ERROR);
        }
    }

    @Override
    public List<KylinUserInfo> getUsers(ConnectionInfo connInfo) throws SemanticException {
        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());
        String content;
        if (SemanticConfig.getInstance().isConvertorMock()) {
            content = getKylinUser();
        } else {
            content = doHttpCall(HttpUri.getKylinUsers(SemanticConfig.getInstance().getUserPageSize()), auth);
        }
        try {
            JSONObject kylinJsonObject = JSONObject.parseObject(content);
            if (kylinJsonObject == null) {
                return Collections.emptyList();
            }
            JSONArray jsonArr = kylinJsonObject.getJSONObject("data").getJSONArray("users");
            if (jsonArr.size() == 0) {
                return Collections.emptyList();
            }
            return jsonArr.toJavaList(KylinUserInfo.class);
        } catch (Exception e) {
            log.error("The http call KYLIN4 API [getKylinUsers] get exception, response body:{}", content);
            throw new SemanticException("Http: getKylinUsers get exception", ErrorCode.FETCH_KYLIN_USER_LIST_ERROR);
        }
    }

    @Override
    public List<String> getGroups(ConnectionInfo connInfo) throws SemanticException {
        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());
        String content = doHttpCall(HttpUri.GET_KYLIN_GROUPS, auth);
        JSONObject result = JSON.parseObject(content);
        if (result == null || result.get("data") == null) {
            return Collections.emptyList();
        }
        return result.getJSONObject("data").getJSONObject("groups").keySet().stream().collect(Collectors.toList());
    }

    @Override
    public List<String> getUserAuthorities(ConnectionInfo connInfo, String username) throws SemanticException {
        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());
        String content;
        List<String> authorities = new LinkedList<>();
        content = doHttpCall(HttpUri.getKylinUsrAuthority(SemanticConfig.getInstance().getUserPageSize(), username), auth);
        try {
            JSONObject kylinJsonObject = JSONObject.parseObject(content);
            if (kylinJsonObject == null) {
                return Collections.emptyList();
            }
            JSONArray jsonArr = kylinJsonObject.getJSONObject("data").getJSONArray("users");
            if (jsonArr.size() == 0) {
                return Collections.emptyList();
            }
            for (int i = 0; i < jsonArr.size(); i++) {
                JSONObject jsonObject = jsonArr.getJSONObject(i);
                if (!username.equalsIgnoreCase(jsonObject.get("username").toString())) {
                    break;
                }

                JSONArray authArray = jsonObject.getJSONArray("authorities");
                for (int j = 0; j < authArray.size(); j++) {
                    JSONObject authority = authArray.getJSONObject(j);
                    authorities.add(authority.getString("authority"));
                }
            }
        } catch (Exception e) {
            log.error("The http call KYLIN 4 API [getKylinUsrAuthority] get exception, response body:{}", content, e);
            throw new SemanticException("Http: getKylinUsrAuthority get exception", e, ErrorCode.FETCH_KYLIN_USER_INFO_ERROR);
        }
        return authorities;
    }

    @Override
    public AclProjectModel getAclProjectModel(ConnectionInfo connInfo, String type, String name, List<String> tables) {
        return aclConvertor.getAclProjectModel(connInfo, type, name, tables);
    }

    @Override
    public List<String> getGroupsByProject(ConnectionInfo connInfo) {
        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());
        return filterAccessInfo(auth, connInfo.getProject(), "grantedAuthority");
    }

    @Override
    public List<String> getUsersByProject(ConnectionInfo connInfo) {
        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());
        List<String> users = new ArrayList<>();
        // 补充 ROLE_ADMIN 用户组成员
        List<String> admin = new ArrayList<>();
        AsyncService service = AsyncManager.getInstance().getAsyncService();
        BatchTaskExecutor executor = new BatchTaskExecutor(service);
        executor.submit(() -> users.addAll(filterAccessInfo(auth, connInfo.getProject(), "principal")));
        executor.submit(() -> admin.addAll(filterUsernames(auth, SemanticConstants.ROLE_ADMIN)));
        try {
            executor.executeWithThis();
        } catch (InterruptedException e) {
            log.error("The http call KYLIN 4 API [getUsersByProject] get exception", e);
            throw new SemanticException("Http: getUsersByProject throw exception", ErrorCode.FETCH_KYLIN_USER_INFO_ERROR);
        }
        List<String> result = new ArrayList<>(admin);
        result.addAll(users);
        return result.stream().map(String::toUpperCase).distinct().collect(Collectors.toList());
    }

    private List<String> filterAccessInfo(byte[] auth, String project, String type) {
        String uri = HttpUri.getKylinAccessProject(project);
        String content = doHttpCall(uri, auth);
        JSONObject result = JSON.parseObject(content);
        validateRespSuccess(result);
        try {
            JSONArray sids = result.getJSONArray("data");
            if (sids.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> users = new ArrayList<>();
            for (int i = 0; i < sids.size(); i++) {
                JSONObject jsonObject = sids.getJSONObject(i);
                JSONObject sidObject = jsonObject.getJSONObject("sid");
                if (sidObject.containsKey(type)) {
                    users.add(sidObject.getString(type));
                }
            }
            return users;
        } catch (Exception e) {
            log.error("The http call KYLIN API [getProjectInstance] get exception, response body:{}", content);
            throw new SemanticException("Http: getProjectInstance get exception", ErrorCode.FETCH_KYLIN_ACCESS_INFO_ERROR);
        }
    }

    private List<String> filterUsernames(byte[] auth, String group) {
        String uri = HttpUri.getKylinGroupMembers(group);
        String content = doHttpCall(uri, auth);
        JSONObject result = JSONObject.parseObject(content);
        validateRespSuccess(result);
        try {
            JSONObject groups = result.getJSONObject("data").getJSONObject("groups");
            if (groups == null) {
                return Collections.emptyList();
            }
            JSONArray members = groups.getJSONArray(group);
            List<String> users = new ArrayList<>();
            for (int i = 0; i < members.size(); i++) {
                String jsonObject = members.getString(i);
                users.add(jsonObject);
            }
            return users;
        } catch (Exception e) {
            log.error("The http call KYLIN 4 API [getUsersByProject] get exception, response body:{}", content);
            throw new SemanticException("Http: getUsersByProject get exception", ErrorCode.FETCH_KYLIN_USER_INFO_ERROR);
        }
    }

    protected String getCubeNames(String projectName) {
        return null;
    }

    protected String getKylinUser() {
        return null;
    }

    protected String getModelDescByHttp(String modelName, String projectName) {
        return null;
    }

    protected String getCubeDescByHttp(String cubeName, String projectName) {
        return null;
    }

    protected String getColumnDescByHttp(String projectName) {
        return null;
    }
}
