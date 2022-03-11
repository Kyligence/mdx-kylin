package mondrian.olap.fun;

import com.google.common.collect.Lists;
import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.impl.GenericCalc;
import mondrian.mdx.HierarchyExpr;
import mondrian.mdx.LevelExpr;
import mondrian.mdx.MemberExpr;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.rolap.agg.AggregationManager;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScopeFunDef extends FunDefBase {

    static final ResolverImpl Resolver = new ResolverImpl();

    private static final Logger LOGGER =
            Logger.getLogger(AggregationManager.class);

    public ScopeFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final Exp[] args = call.getArgs();
        List<ScopedParameter> scopedParameterList = Lists.newArrayList();
        for (Exp exp : args) {
            scopedParameterList.add(resolveScopedParameter(exp));
        }

        return new GenericCalc(call) {
            public Object evaluate(Evaluator evaluator) {
                final int savepoint = evaluator.savepoint();
                try {
                    for (ScopedParameter scopedParameter : scopedParameterList) {
                        if(scopedParameter == null) {
                            return false;
                        }
                        Hierarchy hierarchy = scopedParameter.getHierarchy();
                        if (hierarchy == null) {
                            return false;
                        }
                        Member member = evaluator.getContext(hierarchy);
                        if (!scopedParameter.isScoped(member)) {
                            return false;
                        }
                    }
                    return true;
                } finally {
                    evaluator.restore(savepoint);
                }
            }

            public Calc[] getCalcs() {
                return new Calc[]{};
            }
        };
    }

    private ScopedParameter resolveScopedParameter(Exp exp) {
        ScopedParameter scopedParameter = null;
        if (exp.getCategory() == Category.Member) {
            MemberExpr memberExpr = (MemberExpr) exp;
            scopedParameter = new ScopedMember(memberExpr.getMember());
        }
        if (exp.getCategory() == Category.Set) {
            ResolvedFunCall funCall = (ResolvedFunCall) exp;
            if (funCall.getFunName().equalsIgnoreCase("{}")) {
                Exp[] memberExps = funCall.getArgs();
                List<Member> memberList = Arrays.stream(memberExps)
                        .map(memberExp -> ((MemberExpr)memberExp).getMember())
                        .collect(Collectors.toList());
                scopedParameter = new ScopedSetMembers(memberList);
            }
            if (funCall.getFunName().equalsIgnoreCase("Members")) {
                Exp arg = funCall.getArgs()[0];
                if (arg instanceof HierarchyExpr) {
                    scopedParameter = new ScopedHierarchyMembers(((HierarchyExpr) arg).getHierarchy());
                }
                if (arg instanceof LevelExpr) {
                    scopedParameter = new ScopedLevelMembers(((LevelExpr) arg).getLevel());
                }
            }
            if (funCall.getFunName().equalsIgnoreCase("Children")) {
                Exp arg = funCall.getArgs()[0];
                if (arg instanceof MemberExpr) {
                    scopedParameter = new ScopedChildrenMembers(((MemberExpr) arg).getMember());
                }
            }
            if (funCall.getFunName().equalsIgnoreCase("Except")) {
                scopedParameter = new ScopeExceptMembers(funCall.getArgs()[0], funCall.getArgs()[1]);
                if (!((ScopeExceptMembers)scopedParameter).isValid()) {
                    scopedParameter = null;
                }
            }
        }

        if (scopedParameter == null) {
            LOGGER.warn("can not resolve scope parameter: " + exp);
        }
        return scopedParameter;
    }

    private static class ResolverImpl extends ResolverBase {
        public ResolverImpl() {
            super(
                    "Scope",
                    "Scope([(<Member Expression>|<Set Expression>),])",
                    "return whether or not current member is in scope.",
                    Syntax.Function);
        }

        public FunDef resolve(
                Exp[] args,
                Validator validator,
                List<Conversion> conversions) {
            if (args.length == 0) {
                return null;
            }
            // check args type
            for (int i = 0; i < args.length; i++) {
                if (args[i].getCategory() == Category.Member) {
                    continue;
                }
                if (args[i].getCategory() == Category.Set) {
                    continue;
                }
                if (args[i].getCategory() == Category.Level
                        && args[i] instanceof LevelExpr
                        && ((LevelExpr)args[i]).getLevel().isAll()) {
                    continue;
                }
                return null;
            }

            int returnType = Category.Logical;
            FunDef dummy = createDummyFunDef(this, returnType, args);
            return new ScopeFunDef(dummy);
        }
    }

    interface ScopedParameter {

        Hierarchy getHierarchy();

        boolean isScoped(Member member);
    }

    // like [Measures].[X1], [Date].[Year].&[2009]
    private class ScopedMember implements ScopedParameter {

        private Member scopedMember;

        ScopedMember(Member member) {
            this.scopedMember = member;
        }

        @Override
        public Hierarchy getHierarchy() {
            return scopedMember.getHierarchy();
        }

        @Override
        public boolean isScoped(Member member) {
            return scopedMember.equals(member);
        }
    }

    private class ScopedHierarchyMembers implements ScopedParameter {

        private Hierarchy scopedHierarchy;

        ScopedHierarchyMembers(Hierarchy scopedHierarchy) {
            this.scopedHierarchy = scopedHierarchy;
        }


        @Override
        public Hierarchy getHierarchy() {
            return this.scopedHierarchy;
        }

        @Override
        public boolean isScoped(Member member) {
            return scopedHierarchy.equals(member.getHierarchy());
        }
    }

    private class ScopedLevelMembers implements ScopedParameter {

        private Level scopedLevel;

        ScopedLevelMembers(Level scopedLevel) {
            this.scopedLevel = scopedLevel;
        }


        @Override
        public Hierarchy getHierarchy() {
            return scopedLevel.getHierarchy();
        }

        @Override
        public boolean isScoped(Member member) {
            return scopedLevel.equals(member.getLevel());
        }
    }

    private class ScopedChildrenMembers implements ScopedParameter {

        private Member parentMember;

        ScopedChildrenMembers(Member parentMember) {
            this.parentMember = parentMember;
        }


        @Override
        public Hierarchy getHierarchy() {
            return parentMember.getHierarchy();
        }

        @Override
        public boolean isScoped(Member member) {
            return parentMember.equals(member.getParentMember());
        }
    }

    private class ScopedSetMembers implements ScopedParameter {

        private List<Member> scopedMemberList;

        ScopedSetMembers(List<Member> scopedMemberList) {
            this.scopedMemberList = scopedMemberList;
        }


        @Override
        public Hierarchy getHierarchy() {
            return isEmpty() ? null : scopedMemberList.get(0).getHierarchy();
        }

        @Override
        public boolean isScoped(Member member) {
            return scopedMemberList.contains(member);
        }

        public boolean isEmpty() {
            return scopedMemberList.isEmpty();
        }
    }

    private class ScopeExceptMembers implements ScopedParameter {

        private boolean isValid = false;

        private ScopedParameter include;

        private ScopedParameter exclude;

        public boolean isValid() {
            return isValid;
        }

        public ScopeExceptMembers(Exp includeArg, Exp excludeArg) {
            include = resolveScopedParameter(includeArg);
            exclude = resolveScopedParameter(excludeArg);
            if (include != null && exclude != null) {
                this.isValid = true;
            }
        }

        @Override
        public Hierarchy getHierarchy() {
            return this.include.getHierarchy();
        }

        @Override
        public boolean isScoped(Member member) {
            return include.isScoped(member) && !exclude.isScoped(member);
        }

    }

}
