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
import io.kylin.mdx.insight.core.dao.SqlQueryMapper;
import io.kylin.mdx.insight.core.entity.SqlQuery;
import io.kylin.mdx.insight.core.service.SqlQueryService;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SqlQueryServiceImpl implements SqlQueryService {

    @Autowired
    private SqlQueryMapper sqlQueryMapper;

    @Override
    public String insertSqlQuery(SqlQuery sqlQuery) throws SemanticException {
        int r = sqlQueryMapper.insertOneReturnId(sqlQuery);
        if (r > 0 && sqlQuery.getId() != null) {
            return SemanticConstants.RESP_SUC + ":" + sqlQuery.getId();
        } else {
            throw new SemanticException(Utils.formatStr("Inserting SQL_QUERY record gets a failure, r:%d, " +
                    "mdx_query_id:%s, sql_text:%s", r, sqlQuery.getMdxQueryId(), sqlQuery.getSqlText()));
        }
    }
    @Override
    public List<SqlQuery> selectSqlQueryByPage(Integer pageNum, Integer pageSize, String mdxQueryId, Boolean status) {
        SqlQuery sqlQuery = new SqlQuery(mdxQueryId, status);
        return sqlQueryMapper.selectSqlQueryByPage(sqlQuery, new RowBounds(pageNum, pageSize));
    }

}
