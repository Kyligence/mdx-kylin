/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2006-2011 Pentaho
// All Rights Reserved.
*/
package mondrian.olap.fun;

import mondrian.calc.*;
import mondrian.calc.impl.AbstractMemberCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.rolap.RolapHierarchy;
import mondrian.rolap.RolapMember;
import mondrian.rolap.RolapUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Definition of the <code>&lt;Hierarchy&gt;.CurrentMember</code> MDX
 * builtin function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class HierarchyCurrentMemberFunDef extends FunDefBase {
    static final HierarchyCurrentMemberFunDef instance =
            new HierarchyCurrentMemberFunDef();

    private HierarchyCurrentMemberFunDef() {
        super(
            "CurrentMember",
            "Returns the current member along a hierarchy during an iteration.",
            "pmh");
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final HierarchyCalc hierarchyCalc =
            compiler.compileHierarchy(call.getArg(0));
        final Hierarchy hierarchy = hierarchyCalc.getType().getHierarchy();
        if (hierarchy != null) {
            return new FixedCalcImpl(call, hierarchy);
        } else {
            return new CalcImpl(call, hierarchyCalc);
        }
    }

    /**
     * Compiled implementation of the Hierarchy.CurrentMember function that
     * evaluates the hierarchy expression first.
     */
    public static class CalcImpl extends AbstractMemberCalc {
        private final HierarchyCalc hierarchyCalc;

        public CalcImpl(Exp exp, HierarchyCalc hierarchyCalc) {
            super(exp, new Calc[] {hierarchyCalc});
            this.hierarchyCalc = hierarchyCalc;
        }

        protected String getName() {
            return "CurrentMember";
        }

        public Member evaluateMember(Evaluator evaluator) {
            Hierarchy hierarchy = hierarchyCalc.evaluateHierarchy(evaluator);
            Member member = evaluator.getContext(hierarchy);
            List<List<Member>> aggregationList = evaluator.getAggregationList(member, false);
            if (aggregationList == null) {
                return evaluator.getContext(hierarchy);
            } else {
                return generateVisualTotalMember(member, aggregationList, evaluator);
            }
        }

        public boolean dependsOn(Hierarchy hierarchy) {
            return hierarchyCalc.getType().usesHierarchy(hierarchy, false);
        }
    }

    /**
     * Compiled implementation of the Hierarchy.CurrentMember function that
     * uses a fixed hierarchy.
     */
    public static class FixedCalcImpl extends AbstractMemberCalc {
        // getContext works faster if we give RolapHierarchy rather than
        // Hierarchy
        private final RolapHierarchy hierarchy;

        public FixedCalcImpl(Exp exp, Hierarchy hierarchy) {
            super(exp, new Calc[] {});
            assert hierarchy != null;
            this.hierarchy = (RolapHierarchy) hierarchy;
        }

        protected String getName() {
            return "CurrentMemberFixed";
        }

        public Member evaluateMember(Evaluator evaluator) {
            Member member = evaluator.getContext(hierarchy);
            List<List<Member>> aggregationList = evaluator.getAggregationList(member, false);
            if (aggregationList == null) {
                return evaluator.getContext(hierarchy);
            } else {
                return generateVisualTotalMember(member, aggregationList, evaluator);
            }
        }

        public boolean dependsOn(Hierarchy hierarchy) {
            return this.hierarchy == hierarchy;
        }

        public void collectArguments(Map<String, Object> arguments) {
            arguments.put("hierarchy", hierarchy);
            super.collectArguments(arguments);
        }
    }

    private static Member generateVisualTotalMember(Member member,
                                                    List<List<Member>> aggregationList,
                                                    Evaluator evaluator) {
        if (member instanceof VisualTotalsFunDef.VisualTotalMember) {
            return member;
        }

        if (aggregationList.isEmpty()) {
            return member;
        }

        List<Member> aggregationMembers = aggregationList.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Validator validator = evaluator.getQuery().createValidator();

        return new VisualTotalsFunDef.VisualTotalMember(
                (RolapMember)member,
                member.getName(),
                RolapUtil.makeAggregateExpr(aggregationMembers).accept(validator),
                aggregationList.get(0));
    }
}

// End HierarchyCurrentMemberFunDef.java
