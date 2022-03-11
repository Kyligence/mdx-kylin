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

import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.dao.MdxQueryMapper;
import io.kylin.mdx.insight.core.dao.SqlQueryMapper;
import io.kylin.mdx.insight.core.entity.*;
import io.kylin.mdx.insight.core.service.MdxQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MdxQueryServiceImpl implements MdxQueryService {

    private MdxQueryMapper mdxQueryMapper;

    private SqlQueryMapper sqlQueryMapper;

    private PlatformTransactionManager txManager;

    @Autowired
    public MdxQueryServiceImpl(MdxQueryMapper mdxQueryMapper, SqlQueryMapper sqlQueryMapper, PlatformTransactionManager txManager) {
        this.mdxQueryMapper = mdxQueryMapper;
        this.sqlQueryMapper = sqlQueryMapper;
        this.txManager = txManager;
    }

    private static final Integer[] selectTime = {0, 3000, 10000, 30000, 60000, null};

    // The size of the Mdx_Query to be deleted each time
    private static final int batchSize = 100;

    @Override
    public String insertMdxQuery(MdxQuery mdxQuery) {
        int r = mdxQueryMapper.insertOneReturnId(mdxQuery);
        if (r > 0 && mdxQuery.getId() != null) {
            return SemanticConstants.RESP_SUC + ":" + mdxQuery.getId();
        } else {
            throw new SemanticException(Utils.formatStr("Inserting MDX_QUERY record gets a failure, r:%d, " +
                    "mdx_query_id:%s", r, mdxQuery.getMdxQueryId()));
        }
    }

    @Override
    public void houseKeep(int maxRows) {
        int count = countRecords();
        if (maxRows <= 0 || count < maxRows + batchSize) {
            return;
        }
        deleteByRows(maxRows, count);
    }

    @Override
    public int countRecords() {
        return mdxQueryMapper.countRecords();
    }

    @Override
    public void deleteByRows(int maxRows, int count) {
        //  calculate the range of Query ids to be deleted
        int deleteSize = count - maxRows;
        int times = Math.floorDiv(deleteSize, batchSize);
        int deleteRemain = Math.floorMod(deleteSize, batchSize);
        log.info("Deleting MDX query logs size：{}", deleteSize);
        for (int t = 0; t < times; t++){
            List<String> deleteUuidIdRange = mdxQueryMapper.getDeleteUuid(batchSize);
            batchDelete(deleteUuidIdRange);
        }
        if (deleteRemain > 0){
            List<String> deleteUuidIdRangeRemain = mdxQueryMapper.getDeleteUuid(deleteRemain);
            batchDelete(deleteUuidIdRangeRemain);
        }
    }

    /**
     * Delete MDX and SQL Query log in batches
     * @param Uuids
     */
    public void batchDelete(List<String> Uuids){
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = txManager.getTransaction(def);
        try{
            // delete mdx_query
            mdxQueryMapper.deleteByUuids(Uuids);
            // delete sql_query
            sqlQueryMapper.deleteByMdxQueryIds(Uuids);
            txManager.commit(status);
        }catch (Exception e){
            txManager.rollback(status);
        }
    }

    @Override
    public List<MdxQuery> selectMdxQueryByPage(SelectMdxQueryEntity selectMdxQueryEntity, int pageNum, int pageSize) {
        return mdxQueryMapper.selectMdxQueryByPage(selectMdxQueryEntity, new RowBounds(pageNum, pageSize));
    }

    @Override
    public BasicStatistics getBasicStatistics(Long startTime, Long endTime, String projectName) {
        BasicStatistics basicStatistics = new BasicStatistics();
        SelectBasicStatistics selectBasicStatistics = new SelectBasicStatistics(startTime, endTime, projectName);
        // 查询用户量
        Integer userNameNum = mdxQueryMapper.getUserNames(selectBasicStatistics);
        basicStatistics.setQueryUsersCount(userNameNum);
        // 查询总次数
        Integer selectNum = mdxQueryMapper.getSelectNum(selectBasicStatistics);
        basicStatistics.setMdxQueryTotalCount(selectNum);
        // 人均查询次数（包含失败的查询）
        basicStatistics.setQueryAvgCount(0F);
        if (userNameNum != null && userNameNum > 0) {
            basicStatistics.setQueryAvgCount((float) selectNum / userNameNum);
        }
        // 查询失败次数
        selectBasicStatistics.setExecuteState(false);
        Integer selectFailedNum = mdxQueryMapper.getSelectNum(selectBasicStatistics);
        basicStatistics.setMdxQueryFailedCount(selectFailedNum);
        // 平均查询时间（只包含成功的，单位秒）
        selectBasicStatistics.setExecuteState(true);
        Long selectSuccessTime = mdxQueryMapper.getSelectSuccessTime(selectBasicStatistics);
        if (selectNum == null) {
            selectNum = 0;
        }
        if (selectFailedNum == null) {
            selectFailedNum = 0;
        }
        int selectSuccessNum = selectNum - selectFailedNum;

        basicStatistics.setQueryAvgTime(0F);
        if (selectSuccessNum > 0) {
            basicStatistics.setQueryAvgTime((float) selectSuccessTime / selectSuccessNum);
        }
        return basicStatistics;
    }
    @Override
    public StatisticsTrend getStatisticsTrend(List<Long> axis, String projectName, List<String> datasetNames) {
        StatisticsTrend statisticsTrend = new StatisticsTrend();

        // 数据集使用次数趋势
        BatchDatasetUsage datasetUsage = new BatchDatasetUsage();
        List<BatchDatasetUsageData> batchDatasetUsageDataArrays = new LinkedList<>();
        long datasetUseSum = 0L;
        for (String datasetName : datasetNames) {
            BatchDatasetUsageData batchDatasetUsageData = new BatchDatasetUsageData();
            batchDatasetUsageData.setDatasetName(datasetName);
            List<Long> data = new LinkedList<>();
            for (int i = 0; i < axis.size() - 1; i++) {
                SelectBasicStatistics selectBasicStatistics = new SelectBasicStatistics(axis.get(i), axis.get(i + 1), projectName, datasetName);
                Long datasetUseNum = mdxQueryMapper.getDataUseNum(selectBasicStatistics);
                if (datasetUseNum == null || datasetUseNum <= 0) {
                    data.add(0L);
                    datasetUseSum += 0L;
                    continue;
                }
                data.add(datasetUseNum);
                datasetUseSum += datasetUseNum;
            }
            batchDatasetUsageData.setData(data);
            batchDatasetUsageDataArrays.add(batchDatasetUsageData);
        }
        datasetUsage.setData(batchDatasetUsageDataArrays);
        datasetUsage.setValue(datasetUseSum);
        statisticsTrend.setDatasetUsage(datasetUsage);

        // 数据集查询失败趋势
        BatchDatasetUsage queryFailed = new BatchDatasetUsage();
        List<BatchDatasetUsageData> queryFailedArrays = new LinkedList<>();
        Long selectFailedSum = 0L;
        for (String datasetName : datasetNames) {
            BatchDatasetUsageData batchDatasetUsageData = new BatchDatasetUsageData();
            batchDatasetUsageData.setDatasetName(datasetName);
            List<Long> data = new LinkedList<>();
            for (int i = 0; i < axis.size() - 1; i++) {
                SelectBasicStatistics selectBasicStatistics = new SelectBasicStatistics(axis.get(i), axis.get(i + 1), projectName, datasetName);
                selectBasicStatistics.setExecuteState(false);
                Long selectFailedNum = mdxQueryMapper.getDataUseNum(selectBasicStatistics);
                if(selectFailedNum==null || selectFailedNum<=0){
                    data.add(0L);
                    continue;
                }
                data.add(selectFailedNum);
                selectFailedSum += selectFailedNum;
            }
            batchDatasetUsageData.setData(data);
            queryFailedArrays.add(batchDatasetUsageData);
        }
        queryFailed.setData(queryFailedArrays);
        queryFailed.setValue(selectFailedSum);
        statisticsTrend.setQueryFailed(queryFailed);

        // 数据集查询平均时间趋势
        BatchDatasetUsage queryAvgTime = new BatchDatasetUsage();
        List<BatchDatasetUsageData> queryAvgTimeArrays = new LinkedList<>();
        for (String datasetName : datasetNames) {
            BatchDatasetUsageData batchDatasetUsageData = new BatchDatasetUsageData();
            batchDatasetUsageData.setDatasetName(datasetName);
            List<Float> data = new LinkedList<>();
            for (int i = 0; i < axis.size() - 1; i++) {
                SelectBasicStatistics selectBasicStatistics = new SelectBasicStatistics(axis.get(i), axis.get(i + 1), projectName, datasetName);
                selectBasicStatistics.setDatasetName(datasetName);
                Long selectSuccessTime = mdxQueryMapper.getSelectSuccessTime(selectBasicStatistics);
                selectBasicStatistics.setExecuteState(true);
                Integer selectNum = mdxQueryMapper.getSelectNum(selectBasicStatistics);
                Float avgTime = 0F;
                if (selectNum != null && selectNum > 0 && selectSuccessTime != null && selectSuccessTime > 0) {
                    avgTime = (float) selectSuccessTime / selectNum;
                }
                data.add(avgTime);
            }
            batchDatasetUsageData.setData(data);
            queryAvgTimeArrays.add(batchDatasetUsageData);
        }
        queryAvgTime.setData(queryAvgTimeArrays);
        queryAvgTime.setValue(0L);
        statisticsTrend.setQueryAvgTime(queryAvgTime);
        return statisticsTrend;
    }
    @Override
    public QueryCostStatistics getQueryCostStatistics(List<Long> axis, String projectName) {
        QueryCostStatistics queryCostStatistics = new QueryCostStatistics();
        queryCostStatistics.setAxis(axis);
        List<QueryCostStatisticsData> queryCostStatisticsDataArray = new LinkedList<>();
        for (int i = 0; i < selectTime.length - 1; i++) {
            QueryCostStatisticsData queryCostStatisticsData = new QueryCostStatisticsData();
            queryCostStatisticsData.setMin(selectTime[i]);
            queryCostStatisticsData.setMax(selectTime[i + 1]);
            List<Long> data = new LinkedList<>();
            for (int j = 0; j < axis.size() - 1; j++) {
                SelectBasicStatistics selectBasicStatistics = new SelectBasicStatistics(axis.get(j), axis.get(j + 1), projectName, selectTime[i], selectTime[i + 1]);
                Long selectNum = mdxQueryMapper.getSpecificTimeUseNum(selectBasicStatistics);
                data.add(selectNum);
            }
            queryCostStatisticsData.setData(data);
            queryCostStatisticsDataArray.add(queryCostStatisticsData);
        }
        queryCostStatistics.setData(queryCostStatisticsDataArray);
        return queryCostStatistics;
    }
    @Override
    public List<Map<String, String>> getRankingStatistics(Long startTime, Long endTime, Long count, String direction, String projectName) {
        SelectBasicStatistics selectBasicStatistics = new SelectBasicStatistics(startTime, endTime, count, projectName);
        if ("DESC".equalsIgnoreCase(direction)) {
            return mdxQueryMapper.getRankingStatisticsDesc(selectBasicStatistics);
        }
        return mdxQueryMapper.getRankingStatisticsAsc(selectBasicStatistics);
    }
    @Override
    public List<String> getNodes(String projectName) {
        return mdxQueryMapper.getNodes(projectName).stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
