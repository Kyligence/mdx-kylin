/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.olap4j;

import mondrian.olap.Cube;
import mondrian.olap.Exp;
import mondrian.olap.Member;
import mondrian.rolap.DrillThroughResultSet;
import mondrian.rolap.RolapCell;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapCubeHierarchy;
import mondrian.rolap.RolapStoredMeasure;
import mondrian.rolap.SqlStatement;

import org.apache.log4j.Logger;

import org.olap4j.*;
import org.olap4j.metadata.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link Cell}
 * for the Mondrian OLAP engine.
 *
 * @author jhyde
 * @since May 24, 2007
 */
class MondrianOlap4jCell implements Cell {
    private final int[] coordinates;
    private final MondrianOlap4jCellSet olap4jCellSet;
    private final RolapCell cell;

    /**
     * Creates a MondrianOlap4jCell.
     *
     * @param coordinates Coordinates
     * @param olap4jCellSet Cell set
     * @param cell Cell in native Mondrian representation
     */
    MondrianOlap4jCell(
        int[] coordinates,
        MondrianOlap4jCellSet olap4jCellSet,
        RolapCell cell)
    {
        assert coordinates != null;
        assert olap4jCellSet != null;
        assert cell != null;
        this.coordinates = coordinates;
        this.olap4jCellSet = olap4jCellSet;
        this.cell = cell;
    }

    public CellSet getCellSet() {
        return olap4jCellSet;
    }

    public int getOrdinal() {
        return (Integer) cell.getPropertyValue(
            mondrian.olap.Property.CELL_ORDINAL.name);
    }

    public List<Integer> getCoordinateList() {
        ArrayList<Integer> list = new ArrayList<Integer>(coordinates.length);
        for (int coordinate : coordinates) {
            list.add(coordinate);
        }
        return list;
    }

    public Object getPropertyValue(Property property) {
        // We assume that mondrian properties have the same name as olap4j
        // properties.
        return cell.getPropertyValue(property.getName());
    }

    public boolean isEmpty() {
        // FIXME
        return cell.isNull();
    }

    public boolean isError() {
        return cell.isError();
    }

    public boolean isNull() {
        return cell.isNull();
    }

    public double getDoubleValue() throws OlapException {
        Object o = cell.getValue();
        if (o instanceof Number) {
            Number number = (Number) o;
            return number.doubleValue();
        }
        throw olap4jCellSet.olap4jStatement.olap4jConnection.helper
            .createException(this, "not a number");
    }

    public String getErrorText() {
        Object o = cell.getValue();
        if (o instanceof Throwable) {
            return ((Throwable) o).getMessage();
        } else {
            return null;
        }
    }

    public Object getValue() {
        return cell.getValue();
    }

    public String getFormattedValue() {
        return cell.getFormattedValue();
    }

    public boolean canDrillThrough() {
        return cell.canDrillThrough();
    }

    public int getDrillThroughCount() {
        return cell.getDrillThroughCount();
    }

    public ResultSet drillThrough() throws OlapException {
        return drillThroughInternal(
            -1,
            -1,
            null,
            true,
            null,
            null);
    }

    /**
     * Executes drill-through on this cell.
     *
     * <p>Not a part of the public API. Package-protected because this method
     * also implements the DRILLTHROUGH statement.
     *
     * @param maxRowCount Maximum number of rows to retrieve, <= 0 if unlimited
     * @param firstRowOrdinal Ordinal of row to skip to (1-based), or 0 to
     *   start from beginning
     * @param fields            List of fields to return, expressed as MDX
     *                          expressions.
     * @param extendedContext   If true, add non-constraining columns to the
     *                          query for levels below each current member.
     *                          This additional context makes the drill-through
     *                          queries easier for humans to understand.
     * @param logger Logger. If not null and debug is enabled, log SQL here
     * @param rowCountSlot Slot into which the number of fact rows is written
     * @return Result set
     * @throws OlapException on error
     */
    ResultSet drillThroughInternal(
        int maxRowCount,
        int firstRowOrdinal,
        List<Exp> fields,
        boolean extendedContext,
        Logger logger,
        int[] rowCountSlot)
        throws OlapException
    {
        if (!cell.canDrillThrough()) {
            return null;
        }
        if (rowCountSlot != null) {
            rowCountSlot[0] = cell.getDrillThroughCount();
        }
        final SqlStatement sqlStmt =
            cell.drillThroughInternal(
                maxRowCount, firstRowOrdinal, fields, extendedContext,
                logger);

        Cube cube = olap4jCellSet.query.getCube();
        if (cube instanceof RolapCube) {
            RolapCubeHierarchy measureHierarchy = ((RolapCube)cube).getMeasuresHierarchy();
            Member measureMember = cell.getContextMember(measureHierarchy);
            if (measureMember instanceof RolapStoredMeasure) {
                try {
                    return new DrillThroughResultSet(sqlStmt.getWrappedResultSet(), (RolapStoredMeasure)measureMember);
                } catch (SQLException ignored) {
                    // Ignore this, since the ResultSet#getMetaData method will be called in the future.
                    // See: mondrian.xmla.XmlaHandler.TabularRowSet.TabularRowSet(java.sql.ResultSet, int).
                }
            }
        }
        return sqlStmt.getWrappedResultSet();
    }

    public void setValue(
        Object newValue,
        AllocationPolicy allocationPolicy,
        Object... allocationArgs)
        throws OlapException
    {
        Scenario scenario =
            olap4jCellSet.olap4jStatement.olap4jConnection.getScenario();
        cell.setValue(scenario, newValue, allocationPolicy, allocationArgs);
    }
}

// End MondrianOlap4jCell.java
