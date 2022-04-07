package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.MemberCalc;
import mondrian.calc.impl.GenericCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Evaluator;
import mondrian.olap.FunTable;
import mondrian.olap.Member;

import java.util.function.Function;

public class MemberPropertyFunDef extends FunDefBase {

    private final MemberProperty property;

    protected MemberPropertyFunDef(String name, MemberProperty property) {
        super(name, property.getDescription(), property.funFlag);
        this.property = property;
    }

    public static void define(FunTable.Builder builder) {
        for (MemberProperty memberProperty : MemberProperty.values()) {
            for (String propertyName : memberProperty.properties) {
                builder.define(new MemberPropertyFunDef(propertyName, memberProperty));
            }
        }
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        return new GenericCalc(call, new Calc[]{memberCalc}) {
            public Object evaluate(Evaluator evaluator) {
                return property.evaluator.apply(memberCalc.evaluateMember(evaluator));
            }
        };
    }

    private enum MemberProperty {
        CAPTION("pSm", Member::getCaption, new String[]{"Caption", "MemberCaption", "Member_Caption"}),
        NAME("pSm", Member::getName, new String[]{"Name", "MemberName", "Member_Name"}),
        UNIQUE_NAME("pSm", Member::getUniqueName, new String[]{"UniqueName", "Unique_Name", "MemberUniqueName", "Member_Unique_Name"}),
        PARENT_UNIQUE_NAME("pSm", Member::getParentUniqueName, new String[]{"ParentUniqueName", "Parent_Unique_Name", "MemberParentUniqueName", "Member_Parent_Unique_Name"}),
        VALUE("pvm", Member::getValue, new String[]{"MemberValue", "Member_Value"}),
        DATA_MEMBER("pmm", Member::getDataMember, new String[]{"DataMember", "Data_Member"});

        public final String[] properties;

        public final String funFlag;

        public final Function<Member, Object> evaluator;

        MemberProperty(String funFlag, Function<Member, Object> evaluator, String[] properties) {
            this.funFlag = funFlag;
            this.evaluator = evaluator;
            this.properties = properties;
        }

        public String getDescription() {
            return "Returns the " + this.toString() + " of a member.";
        }

    }

}
