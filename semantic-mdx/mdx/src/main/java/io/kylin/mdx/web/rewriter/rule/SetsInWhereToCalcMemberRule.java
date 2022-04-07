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

import io.kylin.mdx.web.rewriter.BaseRewriteRule;
import io.kylin.mdx.web.rewriter.utils.ExpUtils;
import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.*;
import org.olap4j.xmla.server.impl.Pair;

import java.util.List;

public class SetsInWhereToCalcMemberRule extends BaseRewriteRule {

    private static final String QUERY_SCOPE_TEMP_SET_PREFIX = "Query_Temp_Member_";

    @Override
    public Pair<Boolean, Query> rewrite(Query query) {
        QueryAxis whereClause = query.getSlicerAxis();
        if (whereClause == null || whereClause.getSet() == null) {
            return new Pair<>(false, query);
        }
        Exp exp = whereClause.getSet();
        if (!(exp instanceof UnresolvedFunCall)) {
            return new Pair<>(false, query);
        }
        int count = 0;
        for (int i = 0; i < ((UnresolvedFunCall) exp).getArgCount(); i++) {
            Exp slicerExp = ((UnresolvedFunCall) exp).getArg(i);
            if (slicerExp instanceof UnresolvedFunCall &&
                    "Distinct".equalsIgnoreCase(((UnresolvedFunCall) slicerExp).getFunName())) {
                String hierarchyName = scanHierarchyName((UnresolvedFunCall) slicerExp);
                List<Id.Segment> segments = Id.NameSegment.toList(
                        hierarchyName.replaceAll("\\[", "").replaceAll("\\]", "").split("\\."));
                segments.add(new Id.NameSegment(QUERY_SCOPE_TEMP_SET_PREFIX + count, Id.Quoting.QUOTED));
                query.addFormula(new Id(segments),
                        new UnresolvedFunCall("Aggregate", Syntax.Function, new Exp[]{slicerExp}),
                        new MemberProperty[0]);
                ((UnresolvedFunCall) exp).setArg(i, new Id(segments));
                count++;
            }
        }
        return new Pair<>(count != 0, query);
    }

    private String scanHierarchyName(UnresolvedFunCall urfc) {
        String result = "";
        for (Exp arg : urfc.getArgs()) {
            if (arg instanceof UnresolvedFunCall) {
                result = scanHierarchyName((UnresolvedFunCall) arg);
            } else if (arg instanceof Id) {
                result = ExpUtils.getHierarchyName((Id) arg, true);
            }
        }
        return result;
    }

}
