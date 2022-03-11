/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2010-2014 Pentaho and others
// All Rights Reserved.
*/
package mondrian.rolap.agg;

import mondrian.olap.Util;
import mondrian.rolap.CellKey;
import mondrian.rolap.SqlStatement;
import mondrian.spi.SegmentBody;
import mondrian.util.Pair;

import java.sql.Timestamp;
import java.util.BitSet;
import java.util.List;
import java.util.SortedSet;

/**
 * Implementation of {@link SegmentDataset} that stores
 * values of type {@code Timestamp}.
 *
 * <p>The storage requirements are as follows. Table requires 1 word per
 * cell.</p>
 *
 * @author jhyde
 */
class DenseTimestampSegmentDataset extends DenseNativeSegmentDataset {
    final Timestamp[] values; // length == m[0] * ... * m[axes.length-1]

    /**
     * Creates a DenseTimestampSegmentDataset.
     *
     * @param axes Segment axes, containing actual column values
     * @param size Number of coordinates
     */
    DenseTimestampSegmentDataset(SegmentAxis[] axes, int size) {
        this(axes, new Timestamp[size], Util.bitSetBetween(0, size));
    }

    /**
     * Creates a populated DenseIntSegmentDataset.
     *
     * @param axes Segment axes, containing actual column values
     * @param values Cell values; not copied
     * @param nullIndicators Null indicators
     */
    DenseTimestampSegmentDataset(
        SegmentAxis[] axes,
        Timestamp[] values,
        BitSet nullIndicators)
    {
        super(axes, nullIndicators);
        this.values = values;
    }

    public Timestamp getTimestamp(CellKey key) {
        int offset = key.getOffset(axisMultipliers);
        return values[offset];
    }

    public Object getObject(CellKey pos) {
        int offset = pos.getOffset(axisMultipliers);
        return getObject(offset);
    }

    protected Timestamp getObject(int offset) {
        final Timestamp value = values[offset];
        if (value == null && isNull(offset)) {
            return null;
        }
        return value;
    }

    public boolean exists(CellKey pos) {
        return true;
    }

    public void populateFrom(int[] pos, SegmentDataset data, CellKey key) {
        final int offset = getOffset(pos);
        final Timestamp value = values[offset] = data.getTimestamp(key);
        if (value != null || !data.isNull(key)) {
            nullValues.clear(offset);
        }
    }

    public void populateFrom(
        int[] pos, SegmentLoader.RowList rowList, int column)
    {
        int offset = getOffset(pos);
        final Timestamp value = values[offset] = rowList.getTimestamp(column);
        if (value != null || !rowList.isNull(column)) {
            nullValues.clear(offset);
        }
    }

    @Override
    public void putValue(int[] pos, Object value) {
        int offset = getOffset(pos);
        values[offset] = (Timestamp)value;
        if (value != null) {
            nullValues.clear(offset);
        }
    }

    public SqlStatement.Type getType() {
        return SqlStatement.Type.INT;
    }

    public void put(CellKey key, Timestamp value) {
        int offset = key.getOffset(axisMultipliers);
        values[offset] = value;
    }

    public void put(int[] ordinals, Timestamp value) {
        int offset = getOffset(ordinals);
        values[offset] = value;
    }

    void set(int k, Timestamp o) {
        values[k] = o;
    }

    protected int getSize() {
        return values.length;
    }

    public SegmentBody createSegmentBody(
        List<Pair<SortedSet<Comparable>, Boolean>> axes)
    {
        return new DenseTimestampSegmentBody(
            nullValues,
            values,
            axes);
    }
}

// End DenseTimestampSegmentDataset.java
