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

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.web.rewriter.utils.ExpFinder;
import io.kylin.mdx.web.rewriter.utils.ExpVisitor;
import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.Exp;
import mondrian.olap.Id;
import mondrian.olap.Literal;
import mondrian.olap.Syntax;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 改写示意：
 * #Generate(
 * #  Hierarchize({[KYLIN_CAL_DT].[YEAR_BEG_DT].[All]}) AS [XL_Filter_Set_0],
 * #  TopCount(
 * #    Filter(
 * #      Except(DrilldownLevel([XL_Filter_Set_0].Current AS [XL_Filter_HelperSet_0], , 0), [XL_Filter_HelperSet_0]),
 * #      Not IsEmpty([Measures].[_COUNT_])
 * #    ),
 * #    2,
 * #    [Measures].[_COUNT_])
 * #)
 * #  ->
 * #Hierarchize({DrillDownLevelTop({[KYLIN_CAL_DT].[YEAR_BEG_DT].[All]},2,,[Measures].[_COUNT_])})
 */
public class GenerateFunctionTranslator implements ExpTranslator {

    @Override
    public Exp translate(Exp exp, Object... args) {
        UnresolvedFunCall generate = (UnresolvedFunCall) exp;
        try {
            return translate(generate);
        } catch (Exception e) {
            throw new SemanticException("Unsupported pattern in generate function!", e);
        }
    }

    public Exp translate(UnresolvedFunCall generate) {
        // :Generate.[0]:AS.[0]:Hierarchize.[0]:{}.[0] -> Set
        UnresolvedFunCall members = (UnresolvedFunCall) new ExpVisitor(generate).argFun(0).argFun(0).argFun(0).execute();
        // :Generate.[1]:TopCount.[1] -> Literal
        Literal count = (Literal) new ExpVisitor(generate).argFun(1).arg(1).execute();
        // :Generate.[1]:TopCount.[2] -> Id
        Id measure = (Id) new ExpVisitor(generate).argFun(1).arg(2).execute();
        // :Generate.[1]:TopCount.[0]:Filter.[0].Except -> Literal
        UnresolvedFunCall except = (UnresolvedFunCall) new ExpVisitor(generate).argFun(1).argFun(0).argFun(0).execute();

        AtomicInteger drilldownLevelDeep = new AtomicInteger();
        ExpFinder.traversalAndApply(except, exp -> {
            if (exp instanceof UnresolvedFunCall &&
                    ((UnresolvedFunCall) exp).getFunName().equalsIgnoreCase("DrilldownLevel")) {
                drilldownLevelDeep.incrementAndGet();
            }
            return false;
        }, true);

        UnresolvedFunCall membersFunCall = members;
        while (drilldownLevelDeep.get() > 1) {
            membersFunCall = new UnresolvedFunCall("{}", Syntax.Braces, new Exp[]{
                    new UnresolvedFunCall("DrilldownLevel", new Exp[]{
                            membersFunCall
                    })
            });
            drilldownLevelDeep.decrementAndGet();
        }

        // "Hierarchize({DrilldownLevelTop({$ID1},$ID2,,$ID3)})";
        return new UnresolvedFunCall(
                "Hierarchize",
                new Exp[]{
                        new UnresolvedFunCall("{}", Syntax.Braces, new Exp[]{
                                new UnresolvedFunCall("DrilldownLevelTop", new Exp[]{
                                        membersFunCall,
                                        count,
                                        new UnresolvedFunCall("", Syntax.Empty, new Exp[0]),
                                        measure
                                })
                        })
                });
    }

}
