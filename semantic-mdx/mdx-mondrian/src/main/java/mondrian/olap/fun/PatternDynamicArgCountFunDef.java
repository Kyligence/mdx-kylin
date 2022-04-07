package mondrian.olap.fun;

import mondrian.olap.Exp;
import mondrian.olap.FunDef;
import mondrian.olap.Syntax;
import mondrian.olap.Util;
import mondrian.olap.Validator;
import mondrian.olap.type.StringType;
import mondrian.olap.type.Type;

import java.util.List;

public class PatternDynamicArgCountFunDef extends FunDefBase {
    static final Resolver RESOLVER;

    static {
        RESOLVER = new PatternDynamicArgCountFunDefResolver();
    }

    private PatternDynamicArgCountFunDef(String name,
                                     String signature,
                                     String description,
                                     Syntax syntax,
                                     int returnCategory,
                                     int[] paramCategories,
                                     Type type) {
        super(name, signature, description, syntax, returnCategory, paramCategories);
        Util.discard(type);
    }

    private static class PatternDynamicArgCountFunDefResolver extends ResolverBase {
        private PatternDynamicArgCountFunDefResolver() {
            super("PatternDynamicArgCount",
                    "PatternDynamicArgCount(<Exp>, <String>, <String>)",
                    "Indicator for function expression with uncertain argument count in pattern matching.",
                    Syntax.Function);
        }

        @Override
        public FunDef resolve(Exp[] args, Validator validator, List<Conversion> conversions) {
            if (args.length != 3) {
                return null;
            }

            Exp funName = args[1];
            Exp syntax = args[2];
            Exp argExp = args[0];
            if (!(funName.getType() instanceof StringType)) {
                return null;
            }

            return new PatternDynamicArgCountFunDef(
                    getName(),
                    getSignature(),
                    getDescription(),
                    getSyntax(),
                    argExp.getCategory(),
                    new int[] {argExp.getCategory(), funName.getCategory(), syntax.getCategory()},
                    argExp.getType());
        }
    }
}
