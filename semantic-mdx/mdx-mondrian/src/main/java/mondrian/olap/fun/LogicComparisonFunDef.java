package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.impl.AbstractBooleanCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Evaluator;
import mondrian.olap.FunTable;
import mondrian.rolap.RolapMeasure;

public abstract class LogicComparisonFunDef extends FunDefBase {

    protected LogicComparisonFunDef(String name, String description, String flags) {
        super(name, description, flags);
    }

    public static void define(FunTable.Builder builder) {
        // <Value Expression> = <Value Expression>
        //    Merged by ibSS and ibnn, support String and Numeric Expression
        builder.define(
                new FunDefBase(
                        "=",
                        "Returns whether two expressions are equal.",
                        "ibvv") {
                    @Override
                    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
                        final Calc calc0 = compiler.compile(call.getArg(0));
                        final Calc calc1 = compiler.compile(call.getArg(1));
                        return new AbstractBooleanCalc(call, new Calc[]{calc0, calc1}) {
                            public boolean evaluateBoolean(Evaluator evaluator) {
                                return evaluateEquals(calc0.evaluate(evaluator), calc1.evaluate(evaluator), evaluator);
                            }
                        };
                    }
                }
        );
        // <Value Expression> <> <Value Expression>
        //    Merged by ibSS and ibnn, support String and Numeric Expression
        builder.define(
                new FunDefBase(
                        "<>",
                        "Returns whether two expressions are not equal.",
                        "ibvv") {
                    @Override
                    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
                        final Calc calc0 = compiler.compile(call.getArg(0));
                        final Calc calc1 = compiler.compile(call.getArg(1));
                        return new AbstractBooleanCalc(call, new Calc[]{calc0, calc1}) {
                            public boolean evaluateBoolean(Evaluator evaluator) {
                                return !evaluateEquals(calc0.evaluate(evaluator), calc1.evaluate(evaluator), evaluator);
                            }
                        };
                    }
                }
        );
    }

    public static boolean evaluateEquals(Object obj0, Object obj1, Evaluator evaluator) {
        if (obj0 instanceof RolapMeasure) {
            obj0 = evaluateMeasure(evaluator, obj0);
        }
        if (obj1 instanceof RolapMeasure) {
            obj1 = evaluateMeasure(evaluator, obj1);
        }
        if (obj0 == null && obj1 == null) {
            return true;
        }
        if (obj0 == null || obj1 == null) {
            return false;
        }
        if (obj0 instanceof Number && obj1 instanceof Number) {
            double d0 = ((Number) obj0).doubleValue();
            double d1 = ((Number) obj1).doubleValue();
            if (d0 == DoubleNull && d1 == DoubleNull) {
                return true;
            }
            if (Double.isNaN(d0) && Double.isNaN(d1)) {
                return true;
            }
            if (Double.isNaN(d0) || Double.isNaN(d1) ||
                    d0 == DoubleNull || d1 == DoubleNull) {
                return false;
            }
            return Math.abs(d0 - d1) < (1E-15);
        }
        return obj0.equals(obj1);
    }

    private static Object evaluateMeasure(Evaluator evaluator, Object object) {
        final int savepoint = evaluator.savepoint();
        evaluator.setNonEmpty(false);
        try {
            evaluator.setContext((RolapMeasure) object);
            return evaluator.evaluateCurrent();
        } finally {
            evaluator.restore(savepoint);
        }
    }

}
