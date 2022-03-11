package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.DimensionCalc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.impl.AbstractStringCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Dimension;
import mondrian.olap.Evaluator;
import mondrian.olap.FunTable;

import java.util.function.Function;

public class DimensionPropertyFunDef extends FunDefBase {

    private final DimensionProperty property;

    protected DimensionPropertyFunDef(String name, DimensionProperty property) {
        super(name, property.getDescription(), property.funFlag);
        this.property = property;
    }

    public static void define(FunTable.Builder builder) {
        for (DimensionProperty dimensionProperty : DimensionProperty.values()) {
            for (String propertyName : dimensionProperty.properties) {
                builder.define(new DimensionPropertyFunDef(propertyName, dimensionProperty));
            }
        }
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final DimensionCalc dimensionCalc = compiler.compileDimension(call.getArg(0));
        return new AbstractStringCalc(call, new Calc[]{dimensionCalc}) {
            public String evaluateString(Evaluator evaluator) {
                return property.function.apply(dimensionCalc.evaluateDimension(evaluator));
            }
        };
    }

    private enum DimensionProperty {
        CAPTION("pSd", Dimension::getCaption, new String[]{"Caption", "DimensionCaption", "Dimension_Caption"}),
        NAME("pSd", Dimension::getName, new String[]{"Name", "DimensionName", "Dimension_Name"}),
        UNIQUE_NAME("pSd", Dimension::getUniqueName, new String[]{"UniqueName", "Unique_Name", "DimensionUniqueName", "Dimension_Unique_Name"});

        public final String[] properties;

        public final String funFlag;

        public final Function<Dimension, String> function;

        DimensionProperty(String funFlag, Function<Dimension, String> function, String[] properties) {
            this.funFlag = funFlag;
            this.function = function;
            this.properties = properties;
        }

        public String getDescription() {
            return "Returns the " + this.toString() + " of a dimension.";
        }
    }

}
