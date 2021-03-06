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


package io.kylin.mdx.insight.server.support;

import io.kylin.mdx.insight.core.model.acl.AclDataset;
import io.kylin.mdx.insight.core.model.acl.AclDependColumn;
import io.kylin.mdx.insight.core.model.acl.AclNamedSet;
import io.kylin.mdx.web.rewriter.SimpleValidator;
import io.kylin.mdx.web.rewriter.utils.ExpFinder;
import io.kylin.mdx.web.rewriter.utils.ExpUtils;
import mondrian.olap.Exp;
import mondrian.olap.Id;
import mondrian.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class DependResolver {

    private final SimpleValidator validator = new SimpleValidator();

    /**
     * 解析一个表达式依赖的实体
     *
     * @param dataset      数据集模型
     * @param dependColumn 表达式
     * @return 实体依赖结果
     */
    public DependResult resolve(AclDataset dataset, AclDependColumn dependColumn) {
        Exp exp = validator.parseExpression(dependColumn.getExpression());
        DependResult result = new DependResult();
        ExpFinder.traversalAndApply(exp, new ExpFinder.Consumer2FunctionAdapter<>(e -> {
            if (!(e instanceof Id)) {
                return;
            }
            Id id = (Id) e;
            if (ExpUtils.isMeasures(id)) {
                // 判断顺序: 计算度量 -> 普通度量
                String measureName = ExpUtils.getSimpleMeasureName(id);
                AclDependColumn measure = dataset.getCalculateMeasure(measureName);
                if (measure == null) {
                    measure = dataset.getMeasureByAlias(measureName);
                }
                if (measure != null) {
                    result.depends.add(measure);
                }
            } else {
                if (!(id.getElement(0) instanceof Id.NameSegment)) {
                    return;
                }
                String dimensionName = ((Id.NameSegment) id.getElement(0)).getName();
                AclNamedSet namedSet = dataset.getNamedSet(dimensionName);
                if (namedSet != null) {
                    // 命名集
                    result.depends.add(namedSet);
                } else {
                    // 普通维度
                    String dimColName = ExpUtils.getSimpleHierarchyName(id);
                    if (dimColName != null) {
                        result.dimNames.add(new Pair<>(dimensionName, dimColName));
                    }
                }
            }
        }, false), true);
        return result;
    }

    public static class DependResult {

        private final List<AclDependColumn> depends = new ArrayList<>();

        private final List<Pair<String, String>> dimNames = new ArrayList<>();

        public List<AclDependColumn> getDepends() {
            return depends;
        }

        public List<Pair<String, String>> getDimNames() {
            return dimNames;
        }

    }

}
