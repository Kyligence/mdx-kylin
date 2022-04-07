/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2006-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.rolap.agg;

import mondrian.rolap.*;
import mondrian.spi.Dialect;

import java.util.Collection;

/**
 * A constraint which requires a column to have a particular value.
 *
 * @author jhyde
 * @since Nov 2, 2006
 */
public class ValueColumnPredicate
    extends AbstractColumnPredicate
    implements Comparable
{
    private final Comparable value;

    /**
     * Creates a column constraint.
     *
     * @param constrainedColumn Constrained column
     * @param value Value to constraint the column to. (We require that it is
     *   {@link Comparable} because we will sort the values in order to
     *   generate deterministic SQL.)
     */
    public ValueColumnPredicate(
        PredicateColumn constrainedColumn,
        Comparable value)
    {
        super(constrainedColumn);
        assert value != null;
        assert ! (value instanceof StarColumnPredicate);
        this.value = value;
    }

    /**
     * Returns the value which the column is compared to.
     *
     * @return Value to compare column to
     */
    public Comparable getValue() {
        return value;
    }

    public String toString() {
        return String.valueOf(value);
    }

    public boolean equalConstraint(StarPredicate that) {
        return that instanceof ValueColumnPredicate
            && getConstrainedColumnBitKey().equals(
                that.getConstrainedColumnBitKey())
            && this.value.equals(((ValueColumnPredicate) that).value);
    }

    public int compareTo(Object o) {
        ValueColumnPredicate that = (ValueColumnPredicate) o;
        int columnBitKeyComp =
            getConstrainedColumnBitKey().compareTo(
                that.getConstrainedColumnBitKey());

        // First compare the column bitkeys.
        if (columnBitKeyComp != 0) {
            return columnBitKeyComp;
        }

        if (this.value != null
            && that.value != null
            && this.value.getClass() == that.value.getClass())
        {
            return this.value.compareTo(that.value);
        } else {
            String thisComp = String.valueOf(this.value);
            String thatComp = String.valueOf(that.value);
            return thisComp.compareTo(thatComp);
        }
    }

    public boolean equals(Object other) {
        if (!(other instanceof ValueColumnPredicate)) {
            return false;
        }
        final ValueColumnPredicate that = (ValueColumnPredicate) other;

        // First compare the column bitkeys.
        if (!getConstrainedColumnBitKey().equals(
                that.getConstrainedColumnBitKey()))
        {
            return false;
        }

        if (value != null) {
            return value.equals(that.getValue());
        } else {
            return null == that.getValue();
        }
    }

    public int hashCode() {
        int hashCode = getConstrainedColumnBitKey().hashCode();

        if (value != null) {
            hashCode = hashCode ^ value.hashCode();
        }

        return hashCode;
    }

    public void values(Collection<Object> collection) {
        collection.add(value);
    }

    public boolean evaluate(Object value) {
        return this.value.equals(value);
    }

    public void describe(StringBuilder buf) {
        buf.append(value);
    }

    public Overlap intersect(StarColumnPredicate predicate) {
        throw new UnsupportedOperationException();
    }

    public boolean mightIntersect(StarPredicate other) {
        return ((StarColumnPredicate) other).evaluate(value);
    }

    public StarColumnPredicate minus(StarPredicate predicate) {
        assert predicate != null;
        if (((StarColumnPredicate) predicate).evaluate(value)) {
            return Predicates.wildcard(constrainedColumn, false);
        } else {
            return this;
        }
    }

    public void toSql(Dialect dialect, StringBuilder buf) {
        int length = buf.length();
        final RolapSchema.PhysColumn column = getColumn().physColumn;
        String expr = column.toSql();
        buf.append(expr);
        Object key = getValue();
        if (RolapUtil.sqlNullValue.toString().equals(key.toString())) {
            buf.append(" is null");
        } else {
            if (column.getDatatype() == Dialect.Datatype.Boolean) {
                buf.append(RolapUtil.OP_BOOLEAN);
            } else {
                buf.append(" = ");
            }
            try {
                dialect.quote(buf, key, column.getDatatype());
            } catch (NumberFormatException e) {
                // Illegal value cannot be matched.
                buf.setLength(length);
                dialect.quoteBooleanLiteral(buf, false);
            }
        }
    }

    public BitKey checkInList(BitKey inListLHSBitKey) {
        // ValueColumn predicate by itself is not using IN list; when it is
        // one of the children to an OR predicate, then using IN list
        // is helpful. The later is checked by passing in a bitmap that
        // represent the LHS or the IN list, i.e. the column that is
        // constrained by the OR.
        BitKey inListRHSBitKey = inListLHSBitKey.copy();

        if (!getConstrainedColumnBitKey().equals(inListLHSBitKey)
            || value == RolapUtil.sqlNullValue)
        {
            inListRHSBitKey.clear();
        }

        return inListRHSBitKey;
    }

    public void toInListSql(Dialect dialect, StringBuilder buf) {
        dialect.quote(buf, value, getColumn().physColumn.getDatatype());
    }
}

// End ValueColumnPredicate.java
