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

import io.kylin.mdx.insight.core.entity.SqlQuery;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SqlQueryMapper extends Mapper<SqlQuery> {

    int insertOneReturnId(SqlQuery sqlQuery);

    /**
     *
     * @param sqlQuery
     * @param rowBounds 分页实体，包含每页大小及页码
     * @return
     */
    List<SqlQuery> selectSqlQueryByPage(SqlQuery sqlQuery, RowBounds rowBounds);

    int deleteByMdxQueryIds(@Param("uuids")List<String> uuids);
}
