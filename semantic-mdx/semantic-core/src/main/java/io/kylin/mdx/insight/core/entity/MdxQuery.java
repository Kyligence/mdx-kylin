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
@Table(name = "mdx_query")
@NoArgsConstructor
public class MdxQuery {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    @Column(name = "mdx_query_id")
    private String mdxQueryId;

    @Column(name = "mdx_text")
    private String mdxText;

    @Column(name = "start")
    private Long start;

    @Column(name = "total_execution_time")
    private Long totalExecutionTime;

    @Column(name = "username")
    private String username;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "project")
    private String project;

    @Column(name = "application")
    private String application;

    @Column(name = "mdx_cache_used")
    private Boolean mdxCacheUsed;

    @Column(name = "before_connection")
    private Long beforeConnectionTime;

    @Column(name = "connection")
    private Long connectionTime;

    @Column(name = "hierarchy_load")
    private Long hierarchyLoadTime;

    @Column(name = "olaplayout_construction")
    private Long olapLayoutTime;

    @Column(name = "aggregationqueries_construction")
    private Long aggQueriesConstructionTime;

    @Column(name = "aggregationqueries_execution")
    private Long aggQueriesExecutionTime;

    @Column(name = "otherresult_construction")
    private Long otherResultConstructionTime;

    @Column(name = "network_package")
    private Integer networkPackage;

    @Column(name = "timeout")
    private Boolean timeout;

    @Column(name = "message")
    private String message;

    @Column(name = "calculate_axes")
    private Long calculateAxes;

    @Column(name = "calculate_cell")
    private Long calculateCell;

    @Column(name = "calculate_cellrequest_num")
    private Long calculateCellRequestNum;

    @Column(name = "create_rolapresult")
    private Long createRolapResult;

    @Column(name = "create_multidimensional_dataset")
    private Long createMultiDimensionalDataset;

    @Column(name = "marshall_soap_message")
    private Long marshallSoapMessage;

    @Column(name = "dataset_name")
    private String datasetName;

    @Column(name = "gateway")
    private Boolean gateway;

    @Column(name = "other_used")
    private Boolean otherUsed;

    @Column(name = "node")
    private String node;

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

    public MdxQuery(Map<String, Object> mapMdxQuery) throws SemanticException {
        if (mapMdxQuery == null) {
            return;
        }

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(this.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                Method setter = property.getWriteMethod();
                if (setter != null) {
                    setter.invoke(this, mapMdxQuery.get(property.getName()));
                }
            }
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            throw new SemanticException(e);
        }
    }

}
