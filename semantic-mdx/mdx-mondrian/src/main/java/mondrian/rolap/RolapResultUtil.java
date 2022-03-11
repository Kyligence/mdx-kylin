package mondrian.rolap;

import mondrian.calc.TupleCollections;
import mondrian.calc.TupleIterable;
import mondrian.calc.TupleList;
import mondrian.mdx.MemberExpr;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Exp;
import mondrian.olap.Member;
import mondrian.olap.fun.FunUtil;
import mondrian.olap.fun.VisualTotalsFunDef;

import java.util.*;

/**
 * split RolapResult
 */
public class RolapResultUtil {

    /**
     * ValueNotReady is used to describe a cell dependent metric that
     * is not ready and should not be returned as a result.
     */
    private static final ThreadLocal<Boolean> valueNotReadyLocal = ThreadLocal.withInitial(() -> false);

    /**
     * Synchronized Map from Locale to ValueFormatter. It is expected that
     * there will be only a small number of Locale's.
     * Should these be a WeakHashMap?
     */
    private static final Map<Locale, ValueFormatter> valueFormatters = Collections.synchronizedMap(new HashMap<>());

    public static boolean isValueNotReady() {
        return valueNotReadyLocal.get();
    }

    public static void markValueNotReady() {
        valueNotReadyLocal.set(true);
    }

    public static void resetValueNotReady() {
        valueNotReadyLocal.set(false);
    }

    public static ValueFormatter getValueFormatter(Locale locale) {
        ValueFormatter valueFormatter = valueFormatters.get(locale);
        if (valueFormatter == null) {
            valueFormatter = new FormatValueFormatter(locale);
            valueFormatters.put(locale, valueFormatter);
        }
        return valueFormatter;
    }

    public static void processMemberExpr(Object o, List<Member> exprMembers) {
        if (o instanceof VisualTotalsFunDef.VisualTotalMember) {
            VisualTotalsFunDef.VisualTotalMember member = (VisualTotalsFunDef.VisualTotalMember) o;
            Exp exp = member.getExpression();
            processMemberExpr(exp, exprMembers);
        } else if (o instanceof RolapMember) {
            exprMembers.add((Member) o);
        } else if (o instanceof Exp && !(o instanceof MemberExpr)) {
            Exp exp = (Exp) o;
            ResolvedFunCall funCall = (ResolvedFunCall) exp;
            Exp[] exps = funCall.getArgs();
            processMemberExpr(exps, exprMembers);
        } else if (o instanceof Exp[]) {
            Exp[] exps = (Exp[]) o;
            for (Exp exp : exps) {
                processMemberExpr(exp, exprMembers);
            }
        } else if (o instanceof MemberExpr) {
            MemberExpr memberExp = (MemberExpr) o;
            Member member = memberExp.getMember();
            processMemberExpr(member, exprMembers);
        }
    }

    public static TupleList mergeAxes(TupleList axis1, TupleIterable axis2, boolean ordered) {
        if (axis1.isEmpty() && axis2 instanceof TupleList) {
            return (TupleList) axis2;
        }
        Set<List<Member>> set = new HashSet<>();
        TupleList list = TupleCollections.createList(axis2.getArity());
        for (List<Member> tuple : axis1) {
            if (set.add(tuple)) {
                list.add(tuple);
            }
        }
        int halfWay = list.size();
        for (List<Member> tuple : axis2) {
            if (set.add(tuple)) {
                list.add(tuple);
            }
        }

        // if there are unique members on both axes and no order function,
        // sort the list to ensure default order
        if (halfWay > 0 && halfWay < list.size() && !ordered) {
            list = FunUtil.hierarchizeTupleList(list, false);
        }

        return list;
    }

    /**
     * ValueNotReady object, it's can be cast to 0.
     */
    public static class ValueNotReadyObject extends Number implements Comparable {

        public static ValueNotReadyObject INSTANCE = new ValueNotReadyObject();

        private ValueNotReadyObject() {
        }

        @Override
        public int intValue() {
            return 0;
        }

        @Override
        public long longValue() {
            return 0;
        }

        @Override
        public float floatValue() {
            return 0;
        }

        @Override
        public double doubleValue() {
            return 0;
        }

        @Override
        public int compareTo(Object o) {
            return 0;
        }

    }

}
