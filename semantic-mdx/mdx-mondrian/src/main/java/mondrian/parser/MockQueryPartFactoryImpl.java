

package mondrian.parser;

import mondrian.olap.*;
import mondrian.server.Statement;

import java.util.List;

@Deprecated
public class MockQueryPartFactoryImpl
        implements MdxParserValidator.QueryPartFactory {

    @Override
    public Query makeQuery(Statement statement, Formula[] formulas, QueryAxis[] axes, String cube, boolean nonVisual,
                           Query query, Exp slicer, QueryPart[] cellProps, boolean strictValidation) {
        final QueryAxis slicerAxis =
                slicer == null
                        ? null
                        : new QueryAxis(
                        false, slicer, AxisOrdinal.StandardAxisOrdinal.SLICER,
                        QueryAxis.SubtotalVisibility.Undefined, new Id[0]);
        return new Query(statement, cube, null, formulas, axes, slicerAxis, cellProps,
                new Parameter[0], strictValidation, true);
    }

    @Override
    public DrillThrough makeDrillThrough(Query query, int maxRowCount, int firstRowOrdinal, List<Exp> returnList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Explain makeExplain(QueryPart query) {
        return null;
    }
}
