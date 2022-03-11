package mondrian.rolap;

import mondrian.calc.TupleCursor;
import mondrian.calc.TupleList;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.*;
import mondrian.rolap.sql.CrossJoinArg;
import mondrian.rolap.sql.DescendantsCrossJoinArg;
import mondrian.rolap.sql.SqlQuery;
import mondrian.rolap.sql.TupleConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RolapNativeDescendants extends RolapNativeSet {
    public RolapNativeDescendants() {
        super.setEnabled(
                MondrianProperties.instance().EnableNativeFilter.get());
    }

    protected class DescendantsEvaluator extends SetEvaluator {
        public DescendantsEvaluator(CrossJoinArg[] args,
                                    SchemaReader schemaReader,
                                    TupleConstraint constraint) {
            super(args, schemaReader, constraint);
        }

        @Override
        protected TupleList executeList(TupleReader tr) {
            List<RolapMember> currentMember = ((DescendantsCrossJoinArg) args[0]).getOriginalMembers();

            TupleList allResults = super.executeList(tr);
            TupleCursor cursor = allResults.tupleCursor();
            TupleList result = new UnaryTupleList();
            while (cursor.forward()) {
                List<Member> member = cursor.current();
                if (member.get(0).isChildOrEqualTo(currentMember.get(0))
                        && member.get(0).getDepth() > currentMember.get(0).getDepth()) {
                    result.add(member);
                }
            }
            return result;
        }
    }

    @Override
    protected boolean restrictMemberTypes() {
        return true;
    }

    NativeEvaluator createEvaluator(
            RolapEvaluator evaluator,
            FunDef fun,
            Exp[] args,
            RolapNative from,
            Util.Function3<CrossJoinArg[], SchemaReader, TupleConstraint,
                    NativeEvaluator> createEvaluator) {
        if (!fun.getName().equalsIgnoreCase("Descendants")) {
            return null;
        }

        final List<RolapMeasureGroup> measureGroupList =
                new ArrayList<RolapMeasureGroup>();
        if (!SqlContextConstraint.checkValidContext(
                evaluator,
                true,
                Collections.<RolapCubeLevel>emptyList(),
                restrictMemberTypes(),
                measureGroupList)) {
            return null;
        }

        CrossJoinArg[] cjArgs =
                crossJoinArgFactory().checkDescendants(evaluator, evaluator.getSchemaReader().getRole(), fun, args);

        if (cjArgs == null ||
                !(cjArgs[0] instanceof DescendantsCrossJoinArg
                && ((DescendantsCrossJoinArg) cjArgs[0]).isLeaves()))
            return null;
        else {
            ((DescendantsCrossJoinArg) cjArgs[0]).pullUpMember();
        }

        if (isPreferInterpreter(cjArgs, false)) {
            return null;
        }

        final SqlQuery sqlQuery =
            SqlQuery.newQuery(evaluator.getDialect(), "NativeFilter");
        RolapNativeSql sql =
            new RolapNativeSql(
                sqlQuery, null, evaluator, cjArgs[0]);

        final int savepoint = evaluator.savepoint();
        try {
            overrideContext(evaluator, cjArgs, sql.getStoredMeasure());

            // Make it a special case of Filter(Descendants)) that has null filter condition
            TupleConstraint constraint = new RolapNativeFilter.FilterDescendantsConstraint(
                    cjArgs, evaluator, measureGroupList, null);

            return createEvaluator.apply(cjArgs, evaluator.getSchemaReader(), constraint);
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    NativeEvaluator createEvaluator(RolapEvaluator evaluator, FunDef fun, Exp[] args) {
        if (!isEnabled()) {
            return null;
        }

        return createEvaluator(evaluator, fun, args, this, DescendantsEvaluator::new);
    }
}
