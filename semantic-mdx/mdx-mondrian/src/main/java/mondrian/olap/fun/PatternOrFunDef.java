package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.olap.type.Type;

import java.util.List;

public class PatternOrFunDef extends FunDefBase {

    static final String NAME = "PatternOr";
    private static final String SIGNATURE = "PatternOr(<<Exp>>, <<Exp>>[, <<Exp>>])";
    private static final String DESCRIPTION =
            "Optional pattern list in pattern matching";
    private static final Syntax SYNTAX = Syntax.Function;
    static final PatternOrFunDef.PatternOrFunResolver Resolver = new PatternOrFunDef.PatternOrFunResolver();

    private PatternOrFunDef(
            String name,
            String signature,
            String description,
            Syntax syntax,
            int returnCategory,
            int[] paramCategories,
            Type type) {
        super(
                name, signature, description, syntax,
                returnCategory, paramCategories);
        Util.discard(type);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        throw new UnsupportedOperationException();
    }

    private static class PatternOrFunResolver extends ResolverBase {
        private PatternOrFunResolver() {
            super(NAME, SIGNATURE, DESCRIPTION, SYNTAX);
        }

        public FunDef resolve(
                Exp[] args,
                Validator validator,
                List<Conversion> conversions) {
            if (args.length < 2) {
                return null;
            }
            final Exp exp = args[0];
            final int returnCategory = exp.getCategory();
            final int[] paramCategories = new int[args.length];
            for (int i = 0; i < args.length; i++) {
                paramCategories[i] = args[i].getCategory();
            }
            final Type type = exp.getType();
            return new PatternOrFunDef(
                    NAME, SIGNATURE, DESCRIPTION, SYNTAX,
                    returnCategory, paramCategories, type);
        }

        public boolean requiresExpression(int k) {
            return false;
        }
    }
}
