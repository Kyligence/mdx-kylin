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



package io.kylin.mdx.web.rewriter.rule.subquery;

import mondrian.olap.Exp;
import mondrian.olap.Formula;
import mondrian.olap.Id;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 改写过程中统一传递上下文
 */
public class RuleContext {

    // 输出参数

    /**
     * 标记改写成功
     */
    public boolean rewritten;

    // 运行参数

    public final FormulaNaming formulaNaming = new FormulaNaming();

    /**
     * Hierarchy -> Member 映射，包含层次下全部成员 ID，优先合并取交集
     */
    public final Map<String, List<Id>> allMembers = new LinkedHashMap<>();

    /**
     * Hierarchy -> Function 映射，包含维度对应函数，次于 member 执行
     */
    public final Map<String, List<Exp>> allFunctions = new LinkedHashMap<>();

    /**
     * Hierarchy -> Formula 映射, 保持顺序
     * 新增的 Formula 部分，会与 Query 原有的 Formula 部分合并
     */
    public final Map<String, Formula> newMbrFormulas = new LinkedHashMap<>();

    /**
     * Hierarchy -> Formula 映射, 保持顺序
     */
    public final Map<String, List<Formula>> newSetFormulas = new LinkedHashMap<>();

    // 快速方法

    public boolean contains(String hierarchy) {
        return allMembers.containsKey(hierarchy) || allFunctions.containsKey(hierarchy);
    }

}
