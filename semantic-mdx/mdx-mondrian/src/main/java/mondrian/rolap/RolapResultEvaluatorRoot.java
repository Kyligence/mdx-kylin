package mondrian.rolap;

import mondrian.calc.ParameterSlot;
import mondrian.olap.Evaluator;
import mondrian.olap.NamedSet;
import mondrian.olap.Parameter;
import mondrian.olap.Util;
import mondrian.resource.MondrianResource;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension to {@link RolapEvaluatorRoot} which is capable
 * of evaluating named sets.<p/>
 * <p>
 * A given set is only evaluated once each time a query is executed; the
 * result is added to the {@link #namedSetEvaluators} cache on first execution
 * and re-used.<p/>
 *
 * <p>Named sets are always evaluated in the context of the slicer.<p/>
 */
public class RolapResultEvaluatorRoot extends RolapEvaluatorRoot {

    /**
     * Maps the names of sets to their values. Populated on demand.
     */
    private final Map<String, RolapNamedSetEvaluator> namedSetEvaluators = new HashMap<>();

    private final RolapResult result;
    private static final Object CycleSentinel = new Object();
    private static final Object NullSentinel = new Object();

    public RolapResultEvaluatorRoot(RolapResult result) {
        super(result.getExecution());
        this.result = result;
    }

    public RolapResult getResult() {
        return result;
    }

    public Evaluator.NamedSetEvaluator evaluateNamedSet(
            final NamedSet namedSet,
            boolean create) {
        final String name = namedSet.getNameUniqueWithinQuery();
        RolapNamedSetEvaluator value;
        if (namedSet.isDynamic() && !create) {
            value = null;
        } else {
            value = namedSetEvaluators.get(name);
        }
        if (value == null) {
            value = new RolapNamedSetEvaluator(this, namedSet);
            namedSetEvaluators.put(name, value);
        }
        return value;
    }

    public Object getParameterValue(ParameterSlot slot) {
        if (slot.isParameterSet()) {
            return slot.getParameterValue();
        }

        // Look in other places for the value. Which places we look depends
        // on the scope of the parameter.
        Parameter.Scope scope = slot.getParameter().getScope();
        switch (scope) {
            case System:
                // TODO: implement system params

                // fall through
            case Schema:
                // TODO: implement schema params

                // fall through
            case Connection:
                // if it's set in the session, return that value

                // fall through
            case Statement:
                break;

            default:
                throw Util.badValue(scope);
        }

        // Not set in any accessible scope. Evaluate the default value,
        // then cache it.
        Object liftedValue = slot.getCachedDefaultValue();
        Object value;
        if (liftedValue != null) {
            if (liftedValue == CycleSentinel) {
                throw MondrianResource.instance().CycleDuringParameterEvaluation.ex(slot.getParameter().getName());
            }
            if (liftedValue == NullSentinel) {
                value = null;
            } else {
                value = liftedValue;
            }
            return value;
        }
        // Set value to a sentinel, so we can detect cyclic evaluation.
        slot.setCachedDefaultValue(CycleSentinel);
        value = result.evaluateExp(
                slot.getDefaultValueCalc(), result.slicerEvaluator);
        if (value == null) {
            liftedValue = NullSentinel;
        } else {
            liftedValue = value;
        }
        slot.setCachedDefaultValue(liftedValue);
        return value;
    }

}
