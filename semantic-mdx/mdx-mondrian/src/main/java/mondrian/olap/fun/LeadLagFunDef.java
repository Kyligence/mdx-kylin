/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2006-2009 Pentaho
// All Rights Reserved.
*/
package mondrian.olap.fun;

import io.kylin.mdx.rolap.cache.CacheManager;
import io.kylin.mdx.rolap.cache.HierarchyCache;
import io.kylin.mdx.rolap.cache.HierarchyMemberTree;
import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.IntegerCalc;
import mondrian.calc.MemberCalc;
import mondrian.calc.impl.AbstractMemberCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Evaluator;
import mondrian.olap.FunDef;
import mondrian.olap.Member;
import mondrian.olap.MondrianException;
import mondrian.rolap.RolapMember;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

/**
 * Definition of the <code>Lead</code> and <code>Lag</code> MDX functions.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class LeadLagFunDef extends FunDefBase {
    static final ReflectiveMultiResolver LagResolver =
        new ReflectiveMultiResolver(
            "Lag",
            "<Member>.Lag(<Numeric Expression>)",
            "Returns a member further along the specified member's dimension.",
            new String[]{"mmmn"},
            LeadLagFunDef.class);

    static final ReflectiveMultiResolver LeadResolver =
        new ReflectiveMultiResolver(
            "Lead",
            "<Member>.Lead(<Numeric Expression>)",
            "Returns a member further along the specified member's dimension.",
            new String[]{"mmmn"},
            LeadLagFunDef.class);

    public LeadLagFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final MemberCalc memberCalc =
                compiler.compileMember(call.getArg(0));
        final IntegerCalc integerCalc =
                compiler.compileInteger(call.getArg(1));
        final boolean lag = call.getFunName().equals("Lag");
        return new AbstractMemberCalc(
            call,
            new Calc[] {memberCalc, integerCalc})
        {
            public Member evaluateMember(Evaluator evaluator) {
                Member member = memberCalc.evaluateMember(evaluator);
                int n = integerCalc.evaluateInteger(evaluator);
                if (lag) {
                    if (n == Integer.MIN_VALUE) {
                        // Bump up lagValue by one, otherwise -n (used
                        // in the getLeadMember call below) is out of
                        // range because Integer.MAX_VALUE ==
                        // -(Integer.MIN_VALUE + 1).
                        n += 1;
                    }

                    n = -n;
                }
                assert member instanceof RolapMember;
                HierarchyCache hierarchyCache = CacheManager.getCacheManager().getHierarchyCache();
                if (hierarchyCache.isCacheEnabled()) {
                    HierarchyMemberTree memberTree = null;
                    try {
                        memberTree = hierarchyCache.getMemberTree(((RolapMember) member).getHierarchy());
                    } catch (ExecutionException | SQLException e) {
                        throw new MondrianException(e);
                    }
                    return memberTree.getLeadMember(member.getUniqueName(), n);
                } else {
                    return evaluator.getSchemaReader().getLeadMember(member, n);
                }
            }
        };
    }
}

// End LeadLagFunDef.java
