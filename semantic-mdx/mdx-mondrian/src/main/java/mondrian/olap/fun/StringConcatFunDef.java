package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.StringCalc;
import mondrian.calc.impl.AbstractStringCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Evaluator;

public class StringConcatFunDef extends FunDefBase {
    StringConcatFunDef(String name, String description, String flags) {
        super(name, description, flags);
    }

    @Override
    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final StringCalc calc0 = compiler.compileString(call.getArg(0));
        final StringCalc calc1 = compiler.compileString(call.getArg(1));
        return new AbstractStringCalc(call, new Calc[] {calc0, calc1}) {
            public String evaluateString(Evaluator evaluator) {
                final String s0 = calc0.evaluateString(evaluator);
                final String s1 = calc1.evaluateString(evaluator);
                return s0 + s1;
            }
        };
    }
}
