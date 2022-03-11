package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.DoubleCalc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.impl.AbstractDoubleCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Evaluator;
import mondrian.olap.FunDef;

/**
 * @author he.liu
 */
public class InfixPlusNumericFunDef extends FunDefBase {

    static final ReflectiveMultiResolver numericPlusResolver =
            new ReflectiveMultiResolver(
                    "+",
                    "<Member> + <Member> / <Tuple> + <Tuple> / <Numeric> + <Numeric>",
                    "Subtracts two measures.",
                    new String[]   {"inmm", "intt", "innn"},
                    InfixPlusNumericFunDef.class);

    public InfixPlusNumericFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
        final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
        return new AbstractDoubleCalc(call, new Calc[] {calc0, calc1}) {
            public double evaluateDouble(Evaluator evaluator) {
                final double v0 = calc0.evaluateDouble(evaluator);
                final double v1 = calc1.evaluateDouble(evaluator);
                if (v0 == DoubleNull) {
                    if (v1 == DoubleNull) {
                        return DoubleNull;
                    } else {
                        return v1;
                    }
                } else {
                    if (v1 == DoubleNull) {
                        return v0;
                    } else {
                        return v0 + v1;
                    }
                }
            }
        };
    }
}