/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2012-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.parser;

import lombok.Getter;
import mondrian.olap.*;
import mondrian.server.Statement;

import java.util.List;

/**
 * Implementation of the factory that makes parse tree nodes.
 */
public class QueryPartFactoryImpl
    implements MdxParserValidator.QueryPartFactory
{

    @Getter
    private boolean skipResolve;

    public QueryPartFactoryImpl(){
    }

    /**
     * @param skipResolve 是否跳过语法解析阶段
     */
    public QueryPartFactoryImpl(boolean skipResolve) {
        this.skipResolve = skipResolve;
    }

    public Query makeQuery(
        Statement statement,
        Formula[] formulas,
        QueryAxis[] axes,
        String cube,
        boolean nonVisual,
        Query query,
        Exp slicer,
        QueryPart[] cellProps,
        boolean strictValidation)
    {
        final QueryAxis slicerAxis =
            slicer == null
                ? null
                : new QueryAxis(
                    false, slicer, AxisOrdinal.StandardAxisOrdinal.SLICER,
                    QueryAxis.SubtotalVisibility.Undefined, new Id[0]);
        if (cube == null) {
            return new SubQuery(statement, formulas, axes, nonVisual, query,
                    slicerAxis, cellProps, strictValidation);
        } else {
            return new Query(statement, cube, null, formulas, axes, slicerAxis, cellProps,
                    new Parameter[0], strictValidation, skipResolve);
        }
    }

    public DrillThrough makeDrillThrough(
        Query query,
        int maxRowCount,
        int firstRowOrdinal,
        List<Exp> returnList)
    {
        return new DrillThrough(
            query, maxRowCount, firstRowOrdinal, returnList);
    }

    public Explain makeExplain(
        QueryPart query)
    {
        return new Explain(query);
    }
}

// End QueryPartFactoryImpl.java
