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


package io.kylin.mdx.insight.core.dao;

import io.kylin.mdx.insight.core.entity.MdxQuery;
import io.kylin.mdx.insight.core.entity.SelectBasicStatistics;
import io.kylin.mdx.insight.core.entity.SelectMdxQueryEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface MdxQueryMapper extends Mapper<MdxQuery> {

    int insertOneReturnId(MdxQuery mdxQuery);

    int countRecords();

    /**
     * @param search    查询实体
     * @param rowBounds 分页实体，包含每页大小及页码
     * @return
     */
    List<MdxQuery> selectMdxQueryByPage(SelectMdxQueryEntity search, RowBounds rowBounds);

    /**
     * @param selectBasicStatistics
     * @return 获取查询用户数量
     */
    Integer getUserNames(SelectBasicStatistics selectBasicStatistics);

    /**
     * @param selectBasicStatistics
     * @return 获取查询次数
     */
    Integer getSelectNum(SelectBasicStatistics selectBasicStatistics);

    /**
     * @param selectBasicStatistics
     * @return 获取查询成功的总时间
     */
    Long getSelectSuccessTime(SelectBasicStatistics selectBasicStatistics);

    /**
     * @param selectBasicStatistics
     * @return 获取数据集的使用次数
     */
    Long getDataUseNum(SelectBasicStatistics selectBasicStatistics);

    /**
     * @param selectBasicStatistics
     * @return 获取特定时间段内数据集的使用次数
     */
    Long getSpecificTimeUseNum(SelectBasicStatistics selectBasicStatistics);

    /**
     * @param selectBasicStatistics
     * @return 获取由大到小的前n个数据集及使用次数
     */
    List<Map<String,String>> getRankingStatisticsDesc(SelectBasicStatistics selectBasicStatistics);

    /**
     * @param selectBasicStatistics
     * @return 获取由小到大的前n个数据集及使用次数
     */
    List<Map<String,String>> getRankingStatisticsAsc(SelectBasicStatistics selectBasicStatistics);

    /**
     * @return 所有节点信息
     */
    List<String> getNodes(@Param("projectName") String projectName);

    List<String> getDeleteUuid(@Param("batch") int batch);

    /**
     * 依据uuids列表删除query信息
     * @param uuids
     * @return
     */
    int deleteByUuids(@Param("uuids")List<String> uuids);

}
