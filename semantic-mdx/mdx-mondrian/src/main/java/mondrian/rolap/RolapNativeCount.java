package mondrian.rolap;

import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.rolap.cache.SmartCache;
import mondrian.rolap.cache.SoftSmartCache;
import mondrian.rolap.sql.CrossJoinArg;
import mondrian.rolap.sql.DescendantsCrossJoinArg;
import mondrian.rolap.sql.TupleConstraint;
import mondrian.spi.Dialect;

import javax.sql.DataSource;
import java.util.*;

public class RolapNativeCount extends RolapNativeSet {
    public RolapNativeCount() {
        super.setEnabled(
                MondrianProperties.instance().EnableNativeFilter.get());
    }

    private final SmartCache<Object, Map<List<RolapMember>, Integer>> cache = new SoftSmartCache<>();

    protected class CountEvaluator extends ValueEvaluator {

        public CountEvaluator(
                CrossJoinArg[] args,
                SchemaReader schemaReader,
                TupleConstraint constraint) {
            super(args, schemaReader, constraint);
        }

        //TODO distinguish includeEmpty and not excludeEmpty
        @Override
        Integer executeValue(final SqlDescendantsLeavesAggrNumValuesReader valueReader) {
            // Get current dimension member(s)
            List<RolapMember> currentMember = ((DescendantsCrossJoinArg) args[0]).getOriginalMembers();

            // Fetch data from cache, see RolapNativeSet#executeList()
            List<Object> key = new ArrayList<>();
            key.add(valueReader.getCacheKey());
            key.addAll(Arrays.asList(args));
            Map<List<RolapMember>, Integer> resultMap = cache.get(key);
            // May fail if a new value reader result contains the expected value,
            // but this should not happen.
            if (resultMap != null) {
                resultMap.putIfAbsent(currentMember, 0);
                return resultMap.get(currentMember);
            }

            for (CrossJoinArg arg : args) {
                addLevel(valueReader, arg);
            }

            DataSource dataSource = schemaReader.getDataSource();
            final Dialect dialect = schemaReader.getSchema().getDialect();

            Map<List<RolapMember>, Integer> partialCache = valueReader.readIntValues(
                    dialect, dataSource, "count(*)", currentMember);
            partialCache.putIfAbsent(currentMember, 0);
            cache.put(key, partialCache);

            return partialCache.get(currentMember);
        }
    }

    @Override
    NativeEvaluator createEvaluator(RolapEvaluator evaluator, FunDef fun, Exp[] args) {
        if (!isEnabled()) {
            return null;
        }

        if (!(fun.getName().equalsIgnoreCase("Count")
                && (args[0] instanceof ResolvedFunCall))) {
            return null;
        }

        ResolvedFunCall countTargetFun = (ResolvedFunCall) args[0];
        fun = countTargetFun.getFunDef();
        args = countTargetFun.getArgs();

        // Use native cache instead of Cache function
        if (fun.getName().equalsIgnoreCase("Cache")
                && args.length == 1
                && (args[0] instanceof ResolvedFunCall)) {
            countTargetFun = (ResolvedFunCall) args[0];
            fun = countTargetFun.getFunDef();
            args = countTargetFun.getArgs();
        }

        RolapNative nativeFilter = evaluator
                .getSchemaReader()
                .getSchema()
                .getNativeRegistry()
                .getRolapNative(fun);

        NativeEvaluator nativeEvaluator;
        if (nativeFilter instanceof RolapNativeFilter) {
            nativeEvaluator = ((RolapNativeFilter) nativeFilter)
                    .createEvaluator(evaluator, fun, args, this, CountEvaluator::new);
        } else if (nativeFilter instanceof RolapNativeDescendants) {
            nativeEvaluator = ((RolapNativeDescendants) nativeFilter)
                    .createEvaluator(evaluator, fun, args, this, CountEvaluator::new);
        } else {
            return null;
        }

        if (nativeEvaluator == null
                || nativeEvaluator instanceof CountEvaluator
                && (((CountEvaluator) nativeEvaluator).args[0] == null
                || !(((CountEvaluator) nativeEvaluator).args[0] instanceof DescendantsCrossJoinArg))) {
            return null;
        }
        return nativeEvaluator;
    }

    @Override
    protected boolean restrictMemberTypes() {
        return true;
    }
}
