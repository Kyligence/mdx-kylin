/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2010-2010 Pentaho
// All Rights Reserved.
*/
package mondrian.olap.fun;

import io.kylin.mdx.rolap.cache.CacheManager;
import io.kylin.mdx.rolap.cache.HierarchyMemberTree;
import mondrian.calc.*;
import mondrian.calc.impl.AbstractMemberCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.resource.MondrianResource;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapCubeHierarchy;

import java.sql.SQLException;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;

/**
 * Definition of the <code>StrToMember</code> MDX function.
 *
 * <p>Syntax:
 * <blockquote><code>StrToMember(&lt;String Expression&gt;)
 * </code></blockquote>
 */
class StrToMemberFunDef extends FunDefBase {
    public static final FunDef INSTANCE = new StrToMemberFunDef();

    private StrToMemberFunDef() {
        super(
            "StrToMember",
            "Returns a member from a unique name String in MDX format.",
            "fmS");
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final StringCalc memberNameCalc =
            compiler.compileString(call.getArg(0));
        return new AbstractMemberCalc(call, new Calc[] {memberNameCalc}) {
            public Member evaluateMember(Evaluator evaluator) {
                String memberName =
                    memberNameCalc.evaluateString(evaluator);
                if (memberName == null) {
                    throw newEvalException(
                        MondrianResource.instance().NullValue.ex());
                }
                return parseMember(evaluator, memberName, null);
            }
        };
    }

    private Member parseMember2(Evaluator evaluator, String memberName, Hierarchy hierarchy) {
        int hierarchyNameLen = 2;
        String[] memberNameParts = memberName.split(".");
        StringJoiner dotJoiner = new StringJoiner(".");
        for (int i = 0; i < hierarchyNameLen; i++) {
            dotJoiner.add(memberNameParts[i]);
        }
        String targetHierarchyName = dotJoiner.toString();
        if (hierarchy == null) {
            Cube cube = evaluator.getCube();
            assert cube instanceof RolapCube;
            for (RolapCubeHierarchy currentHierarchy : ((RolapCube) cube).getHierarchyList()) {
                if (currentHierarchy.getUniqueName().contentEquals(targetHierarchyName)) {
                    hierarchy = currentHierarchy;
                    break;
                }
            }
        }
        Member result = null;
        try {
            HierarchyMemberTree memberTree = CacheManager.getCacheManager().getHierarchyCache().getMemberTree((RolapCubeHierarchy) hierarchy);
            result = memberTree.getMember(memberName);
            if (result.isNull()) {
                result = null;
            }
        } catch (ExecutionException | SQLException e) {
            throw new MondrianException(e);
        }
        return result;
    }
}

// End StrToMemberFunDef.java
