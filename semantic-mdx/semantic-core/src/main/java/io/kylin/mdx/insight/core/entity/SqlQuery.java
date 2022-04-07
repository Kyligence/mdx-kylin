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


package io.kylin.mdx.insight.core.entity;

import io.kylin.mdx.insight.common.SemanticException;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@Data
@Table(name = "sql_query")
@NoArgsConstructor
public class SqlQuery {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    @Column(name = "mdx_query_id")
    private String mdxQueryId;

    @Column(name = "sql_text")
    private String sqlText;

    @Column(name = "sql_execution_time")
    private Long sqlExecutionTime;

    @Column(name = "sql_cache_used")
    private Boolean sqlCacheUsed;

    @Column(name = "exec_status")
    private Boolean execStatus;

    @Column(name = "ke_query_id")
    private String keQueryId;

    @Column(name = "reserved_field_1")
    private String reserved1;

    @Column(name = "reserved_field_2")
    private String reserved2;

    @Column(name = "reserved_field_3")
    private String reserved3;

    @Column(name = "reserved_field_4")
    private String reserved4;

    @Column(name = "reserved_field_5")
    private String reserved5;

    public SqlQuery(Map<String, Object> mapSqlQuery) throws SemanticException {
        if (mapSqlQuery == null) {
            return;
        }

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(this.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                Method setter = property.getWriteMethod();
                if (setter != null) {
                    setter.invoke(this, mapSqlQuery.get(property.getName()));
                }
            }
        } catch (IntrospectionException e) {
            throw new SemanticException(e);
        } catch (IllegalAccessException e) {
            throw new SemanticException(e);
        } catch (InvocationTargetException e) {
            throw new SemanticException(e);
        }
    }

    public SqlQuery(String mdxQueryId, Boolean execStatus) {
        this.mdxQueryId = mdxQueryId;
        this.execStatus = execStatus;
    }
}
