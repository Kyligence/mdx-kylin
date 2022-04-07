package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.LevelCalc;
import mondrian.calc.impl.AbstractStringCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Evaluator;
import mondrian.olap.FunTable;
import mondrian.olap.Level;

import java.util.function.Function;

public class LevelPropertyFunDef extends FunDefBase {

    private final LevelProperty property;

    protected LevelPropertyFunDef(String name, LevelProperty property) {
        super(name, property.getDescription(), property.funFlag);
        this.property = property;
    }

    public static void define(FunTable.Builder builder) {
        for (LevelProperty LevelProperty : LevelProperty.values()) {
            for (String propertyName : LevelProperty.properties) {
                builder.define(new LevelPropertyFunDef(propertyName, LevelProperty));
            }
        }
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final LevelCalc levelCalc = compiler.compileLevel(call.getArg(0));
        return new AbstractStringCalc(call, new Calc[]{levelCalc}) {
            public String evaluateString(Evaluator evaluator) {
                return property.function.apply(levelCalc.evaluateLevel(evaluator));
            }
        };
    }

    private enum LevelProperty {
        CAPTION("pSl", Level::getCaption, new String[]{"Caption", "LevelCaption", "Level_Caption"}),
        NAME("pSl", Level::getName, new String[]{"Name", "LevelName", "Level_Name"}),
        UNIQUE_NAME("pSl", Level::getUniqueName, new String[]{"UniqueName", "Unique_Name", "LevelUniqueName", "Level_Unique_Name"});

        public final String[] properties;

        public final String funFlag;

        public final Function<Level, String> function;

        LevelProperty(String funFlag, Function<Level, String> function, String[] properties) {
            this.funFlag = funFlag;
            this.function = function;
            this.properties = properties;
        }

        public String getDescription() {
            return "Returns the " + this.toString() + " of a Level.";
        }

    }

}
