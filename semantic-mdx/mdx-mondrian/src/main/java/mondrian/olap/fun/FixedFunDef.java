package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.impl.GenericCalc;
import mondrian.mdx.DimensionExpr;
import mondrian.mdx.HierarchyExpr;
import mondrian.mdx.LevelExpr;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FixedFunDef extends FunDefBase {

    static final FixedFunDef.ResolverImpl Resolver = new FixedFunDef.ResolverImpl();

    public FixedFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final Exp[] args = call.getArgs();
        Exp lastArg = args[args.length - 1];
        Calc calc = compiler.compileScalar(lastArg, false);
        final Set<Dimension> fixedScopeDimensions = new HashSet<>(4);
        final Set<Hierarchy> fixedScopeHierarchies = new HashSet<>(4);
        final Set<Level> fixedScopeLevels = new HashSet<>(4);
        setScopedDimOrHierarchyOrLevel(args, fixedScopeDimensions, fixedScopeHierarchies, fixedScopeLevels);

        return new GenericCalc(call) {
            public Object evaluate(Evaluator evaluator) {
                final int savepoint = evaluator.savepoint();
                try {
                    resetEvaluatorContext(evaluator, fixedScopeDimensions, fixedScopeHierarchies, fixedScopeLevels);
                    return calc.evaluate(evaluator);
                } finally {
                    evaluator.restore(savepoint);
                }
            }

            private void resetEvaluatorContext(Evaluator evaluator, Set<Dimension> fixedScopeDimensions, Set<Hierarchy> fixedScopeHierarchy, Set<Level> fixedScopeLevels) {
                Member[] currentMembers = evaluator.getMembers();
                for (Member currentMember : currentMembers) {
                    Dimension currentDim = currentMember.getDimension();
                    Hierarchy currentHierarchy = currentMember.getHierarchy();
                    Level currentLevel = currentMember.getLevel();
                    if (currentMember.isAll()
                            || fixedScopeDimensions.contains(currentDim)
                            || fixedScopeHierarchy.contains(currentHierarchy)
                            || fixedScopeLevels.contains(currentLevel)
                            || currentMember.isMeasure()) {
                        continue;
                    }
                    Member allMember = currentHierarchy.getAllMember();
                    evaluator.setContext(allMember);
                }
            }

            public Calc[] getCalcs() {
                return new Calc[]{calc};
            }
        };
    }

    private void setScopedDimOrHierarchyOrLevel(Exp[] args, Set<Dimension> fixedScopeDimensions, Set<Hierarchy> fixedScopeHierarchies, Set<Level> fixedScopeLevels) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i] instanceof DimensionExpr) {
                fixedScopeDimensions.add(((DimensionExpr) args[i]).getDimension());
            }
            if (args[i] instanceof HierarchyExpr) {
                fixedScopeHierarchies.add(((HierarchyExpr) args[i]).getHierarchy());
            }
            if (args[i] instanceof LevelExpr) {
                fixedScopeLevels.add(((LevelExpr) args[i]).getLevel());
            }
        }

    }

    private static class ResolverImpl extends ResolverBase {
        public ResolverImpl() {
            super(
                    "Fixed",
                    "Fixed([(<Dimension>|<Hierarchy>|<Level>),], <Measure Expression>)",
                    "Evaluates measures with fixed scope dimensions.",
                    Syntax.Function);
        }

        public FunDef resolve(
                Exp[] args,
                Validator validator,
                List<Conversion> conversions) {
            if (args.length < 1) {
                return null;
            }

            // check whether last arg is measure
            Exp lastArg = args[args.length - 1];
            if (lastArg.getCategory() != Category.Member) {
                return null;
            }

            // check args type before last
            for (int i = 0; i < args.length - 1; i++) {
                if (args[i].getCategory() == Category.Dimension
                        || args[i].getCategory() == Category.Hierarchy
                        || args[i].getCategory() == Category.Level
                ) {
                    continue;
                }
                return null;
            }

            int returnType = Category.Value;
            FunDef dummy = createDummyFunDef(this, returnType, args);
            return new FixedFunDef(dummy);
        }
    }
}
