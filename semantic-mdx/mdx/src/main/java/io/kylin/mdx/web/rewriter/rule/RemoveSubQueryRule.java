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



package io.kylin.mdx.web.rewriter.rule;

import io.kylin.mdx.web.rewriter.SimpleValidator;
import io.kylin.mdx.web.rewriter.rule.subquery.AxisScanner;
import io.kylin.mdx.web.rewriter.rule.subquery.AxisUtils;
import io.kylin.mdx.web.rewriter.rule.subquery.ExpLoader;
import io.kylin.mdx.web.rewriter.rule.subquery.RuleContext;
import io.kylin.mdx.web.rewriter.utils.ExpFinder;
import io.kylin.mdx.web.rewriter.utils.ExpUtils;
import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.*;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 子查询语句改写为非子查询语句
 * 支持子查询部分如下形式:
 * #   SELECT ({${MEMBERS}},...) ON AXIS
 * #   SELECT Function(........) ON AXIS
 * #   WHERE (MEMBER,MEMBER,...)
 * 缺陷:
 * #   基于静态读取 Id 识别 hierarchy，无法识别动态生成的 member
 *
 * @author hui.wang
 */
public class RemoveSubQueryRule extends AbstractSubQueryRule {

    /**
     * 独立维度函数，该函数可以进行改写
     */
    private static final String[] HIERARCHY_FUN_NAMES = new String[]{
            "Hierarchize", "NonEmpty", "AllMembers", "{}"
    };

    private final SimpleValidator validator;

    public RemoveSubQueryRule() {
        this.validator = new SimpleValidator();
    }

    public RemoveSubQueryRule(SimpleValidator validator) {
        this.validator = validator;
    }

    /**
     * 子查询存在 Axis 即认为匹配
     */
    @Override
    public boolean isMatches(SubQuery subQuery) {
        if (subQuery.getQuery() instanceof SubQuery) {
            if (isMatches((SubQuery) subQuery.getQuery())) {
                return true;
            }
        }
        return subQuery.getQuery().getAxes().length > 0 || subQuery.getQuery().getSlicerAxis() != null;
    }

    /**
     * 子查询语句改写，处理流程：
     * # 扫描 子查询
     * # 处理 SELECT
     * # 处理 WHERE
     * # 处理 WITH
     * # 处理 FROM
     * 注意：处理部分流程严格按照改写预期安排，谨慎更改顺序
     */
    @Override
    public boolean doRewrite(SubQuery query) {
        RuleContext context = new RuleContext();
        scanSubQuery(context, query);
        doQueryAxes(context, query);
        doSlicerAxis(context, query);
        doFormula(context, query);
        doSubcube(context, query);
        return context.rewritten;
    }

    /**
     * 读取子查询中的全部 member
     */
    private void scanSubQuery(RuleContext context, SubQuery subQuery) {
        final Query query = subQuery.getQuery();
        // 尝试继续提取下一级查询语句
        if (query instanceof SubQuery) {
            scanSubQuery(context, (SubQuery) query);
        }
        // 提取子查询的 query-axis
        List<QueryAxis> axes = new ArrayList<>(Arrays.asList(query.getAxes()));
        AxisScanner scanner = new AxisScanner(context);
        axes.removeIf(scanner::scanAxis);
        query.setAxes(axes.toArray(new QueryAxis[0]));
        // 提取子查询的 slicer-axis
        if (query.getSlicerAxis() != null && scanner.scanAxis(query.getSlicerAxis())) {
            query.setSlicerAxis(null);
        }
    }

    /**
     * 生成 WITH 并改写 SELECT
     */
    private void doQueryAxes(RuleContext context, SubQuery query) {
        // 与 query-axis 同 hierarchy 的部分构造 WITH SET
        for (QueryAxis axis : query.getAxes()) {
            doQueryAxis(context, axis);
        }
        // 剩余部分构造 WITH MEMBER
        doWithMember(context);
    }

    /**
     * 直接匹配第一个 AddCalculatedMembers/DrilldownLevel/DrilldownMember
     * 然后迭代访问该 FunCall，直到找到第一个和 formulaMap 中 Id 属于同一个 Hierarchy，记录该 Id
     */
    private void doQueryAxis(RuleContext context, QueryAxis axis) {
        ExpFinder.predicateAndApply(axis.getExp(),
                // 检测当前遍历的节点是否可以进行改写
                subExp -> {
                    if (subExp instanceof UnresolvedFunCall) {
                        UnresolvedFunCall subFun = (UnresolvedFunCall) subExp;
                        for (String funName : HIERARCHY_FUN_NAMES) {
                            if (funName.equalsIgnoreCase(subFun.getFunName())) {
                                return true;
                            }
                        }
                    }
                    return false;
                },
                // 可以在该函数上进行改写，例如：Hierarchize
                new ExpFinder.BaseExpConsumer<Exp>() {
                    @Override
                    public void accept(Exp exp) {
                        // 匹配是否存在同 hierarchy
                        final AtomicReference<String> matchId = new AtomicReference<>();
                        boolean isMatches = ExpFinder.traversalAndApply(exp, arg -> {
                            if (arg instanceof Id) {
                                String hierarchy = ExpUtils.getHierarchyName((Id) arg, false);
                                if (hierarchy != null && context.contains(hierarchy)) {
                                    matchId.set(hierarchy);
                                    return true;
                                }
                            }
                            return false;
                        }, true);
                        if (isMatches) {
                            String hierarchy = matchId.get();
                            while (context.contains(hierarchy)) {
                                Id dimId = context.formulaNaming.newSetName(axis);
                                String expStr = AxisUtils.makeSetExp(context, hierarchy);
                                Formula formula = new Formula(dimId, validator.parseExpression(expStr));
                                ExpLoader.translate((UnresolvedFunCall) exp, formula);
                                context.newSetFormulas
                                        .computeIfAbsent(hierarchy, k -> new ArrayList<>())
                                        .add(formula);
                            }
                        }
                        // 跳过子节点执行
                        skip();
                    }
                }
        );
    }

    /**
     * 生成 WITH MEMBER 语句
     */
    private void doWithMember(RuleContext context) {
        if (context.allMembers.isEmpty()) {
            return;
        }
        List<String> hierarchys = new ArrayList<>(context.allMembers.keySet());
        for (String hierarchy : hierarchys) {
            if (context.allMembers.get(hierarchy).size() == 1) {
                continue;
            }
            context.newMbrFormulas.put(hierarchy,
                    new Formula(
                            context.formulaNaming.newMemberName(validator, hierarchy),
                            validator.parseExpression(AxisUtils.makeMbrExp(context, hierarchy)),
                            new MemberProperty[0]));
            context.allMembers.remove(hierarchy);
        }
    }

    /**
     * 基于 formulas 中的 MEMBER 单独构建 slicer
     * 构造过程:
     * #    slicer 不为空 -> 检查其中 member 是否可以合并到 with 部分, 会去重
     * #    合并 with 中聚合的 member 以及 独立 member 到 slicer, 同样会去重
     */
    private void doSlicerAxis(RuleContext context, SubQuery subQuery) {
        List<Formula> withMembers = new ArrayList<>(context.newMbrFormulas.values());
        if (withMembers.isEmpty() && context.allMembers.isEmpty()) {
            return;
        }
        QueryAxis slicer = subQuery.getSlicerAxis();
        UnresolvedFunCall tuples;
        if (slicer == null) {
            tuples = new UnresolvedFunCall("()", Syntax.Parentheses, new Exp[0]);
            slicer = new QueryAxis(false, tuples,
                    AxisOrdinal.StandardAxisOrdinal.SLICER,
                    QueryAxis.SubtotalVisibility.Undefined);
            subQuery.setSlicerAxis(slicer);
        } else {
            tuples = (UnresolvedFunCall) slicer.getExp();
            List<Exp> expList = new ArrayList<>(Arrays.asList(tuples.getArgs()));
            expList.removeIf(exp -> {
                if (exp instanceof Id) {
                    String hierarchy = ExpUtils.getHierarchyName((Id) exp);
                    if (hierarchy != null) {
                        Formula formula = context.newMbrFormulas.get(hierarchy);
                        if (formula != null && formula.isMember()) {
                            // 当前 slicer 中 id 可以聚合到 with member 部分
                            UnresolvedFunCall aggregateFunc = (UnresolvedFunCall) formula.getExpression();
                            UnresolvedFunCall setExpression = (UnresolvedFunCall) aggregateFunc.getArg(0);
                            List<Exp> mbrList = new ArrayList<>(Arrays.asList(setExpression.getArgs()));
                            if (!mbrList.contains(exp)) {
                                mbrList.add(exp);
                                setExpression.setArgs(mbrList.toArray(new Exp[0]));
                            }
                            return true;
                        }
                    }
                }
                return false;
            });
            tuples.setArgs(expList.toArray(new Exp[0]));
        }
        List<Exp> expList = new ArrayList<>(Arrays.asList(tuples.getArgs()));
        for (Formula formula : withMembers) {
            expList.add(formula.getIdentifier());
        }
        context.allMembers.values().forEach(ids -> expList.add(ids.get(0)));
        tuples.setArgs(expList.stream().distinct().toArray(Exp[]::new));
    }

    /**
     * 补充新的 Formula 到 Query
     */
    private void doFormula(RuleContext context, SubQuery subQuery) {
        List<Formula> formulas = new ArrayList<>();
        if (ArrayUtils.getLength(subQuery.getFormulas()) > 0) {
            formulas.addAll(Arrays.asList(subQuery.getFormulas()));
        }
        for (List<Formula> setFormulas : context.newSetFormulas.values()) {
            formulas.addAll(setFormulas);
        }
        formulas.addAll(context.newMbrFormulas.values());
        subQuery.setFormulas(formulas.toArray(new Formula[0]));
    }

    /**
     * 判断子查询是否已经被消去, 未彻底消去不应该移除子查询, 但是均返回 true
     */
    private void doSubcube(RuleContext context, SubQuery query) {
        if (ExpUtils.isNonAxis(query.getQuery())) {
            // 表示子查询被消去, 参考 SubQuery::toString
            query.setCubeName(query.getCubeName());
        }
        context.rewritten = true;
    }

}
