package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.HierarchyCalc;
import mondrian.calc.impl.AbstractStringCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Evaluator;
import mondrian.olap.FunTable;
import mondrian.olap.Hierarchy;

import java.util.function.Function;

public class HierarchyPropertyFunDef extends FunDefBase {

    private final HierarchyProperty property;

    protected HierarchyPropertyFunDef(String name, HierarchyProperty property) {
        super(name, property.getDescription(), property.funFlag);
        this.property = property;
    }

    public static void define(FunTable.Builder builder) {
        for (HierarchyProperty HierarchyProperty : HierarchyProperty.values()) {
            for (String propertyName : HierarchyProperty.properties) {
                builder.define(new HierarchyPropertyFunDef(propertyName, HierarchyProperty));
            }
        }
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final HierarchyCalc hierarchyCalc = compiler.compileHierarchy(call.getArg(0));
        return new AbstractStringCalc(call, new Calc[]{hierarchyCalc}) {
            public String evaluateString(Evaluator evaluator) {
                return property.function.apply(hierarchyCalc.evaluateHierarchy(evaluator));
            }
        };
    }

    private enum HierarchyProperty {
        CAPTION("pSh", Hierarchy::getCaption, new String[]{"Caption", "HierarchyCaption", "Hierarchy_Caption"}),
        NAME("pSh", Hierarchy::getName, new String[]{"Name", "HierarchyName", "Hierarchy_Name"}),
        UNIQUE_NAME("pSh", Hierarchy::getUniqueName, new String[]{"UniqueName", "Unique_Name", "HierarchyUniqueName", "Hierarchy_Unique_Name"});

        public final String[] properties;

        public final String funFlag;

        public final Function<Hierarchy, String> function;

        HierarchyProperty(String funFlag, Function<Hierarchy, String> function, String[] properties) {
            this.funFlag = funFlag;
            this.function = function;
            this.properties = properties;
        }

        public String getDescription() {
            return "Returns the " + this.toString() + " of a hierarchy.";
        }

    }

}
