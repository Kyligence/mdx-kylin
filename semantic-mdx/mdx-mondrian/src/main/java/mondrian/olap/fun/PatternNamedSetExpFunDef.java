package mondrian.olap.fun;

import mondrian.olap.Exp;
import mondrian.olap.FunDef;
import mondrian.olap.Syntax;
import mondrian.olap.Util;
import mondrian.olap.Validator;
import mondrian.olap.type.SetType;
import mondrian.olap.type.Type;

import java.util.List;

public class PatternNamedSetExpFunDef extends FunDefBase {
    static final Resolver RESOLVER;

    static {
        RESOLVER = new PatternNamedSetExpResolver();
    }

    private PatternNamedSetExpFunDef(String name,
                                     String signature,
                                     String description,
                                     Syntax syntax,
                                     int returnCategory,
                                     int[] paramCategories,
                                     Type type) {
        super(name, signature, description, syntax, returnCategory, paramCategories);
        Util.discard(type);
    }

    private static class PatternNamedSetExpResolver extends ResolverBase {
        private PatternNamedSetExpResolver() {
            super("PatternNamedSetExp",
                    "PatternNamedSetExp(<Set>)",
                    "Indicator for Named Set expressions in pattern matching.",
                    Syntax.Function);
        }

        @Override
        public FunDef resolve(Exp[] args, Validator validator, List<Conversion> conversions) {
            if (args.length != 1) {
                return null;
            }

            Exp exp = args[0];
            if (!(exp.getType() instanceof SetType)) {
                return null;
            }

            return new PatternNamedSetExpFunDef(
                    getName(),
                    getSignature(),
                    getDescription(),
                    getSyntax(),
                    exp.getCategory(),
                    new int[] {exp.getCategory()},
                    exp.getType());
        }
    }
}
