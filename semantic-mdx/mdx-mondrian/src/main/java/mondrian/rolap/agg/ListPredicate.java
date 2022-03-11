/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.rolap.agg;

import mondrian.olap.Util;
import mondrian.rolap.*;
import mondrian.spi.Dialect;
import org.apache.commons.collections.SetUtils;

import java.util.*;

/**
 * Base class for {@link AndPredicate} and {@link OrPredicate}.
 *
 * @see mondrian.rolap.agg.ListColumnPredicate
 *
 * @author jhyde
 */
public abstract class ListPredicate implements StarPredicate {
    protected final List<StarPredicate> children =
        new ArrayList<StarPredicate>();

    /**
     * Hash map of children predicates, keyed off of the hash code of each
     * child.  Each entry in the map is a list of predicates matching that
     * hash code.
     */
    private Set<StarPredicateWrapper> childrenSet;

    /**
     * Pre-computed hash code for this list column predicate
     */
    private int hashValue;

    protected final List<PredicateColumn> columns;

    private BitKey columnBitKey = null;

    /**
     * Creates a ListPredicate.
     *
     * @param predicateList List of operand predicates
     */
    protected ListPredicate(
        List<StarPredicate> predicateList)
    {
        hashValue = 0;
        // Ensure that columns are sorted by bit-key, for determinacy.
        final SortedSet<PredicateColumn> columnSet =
            new TreeSet<PredicateColumn>(
                PredicateColumn.COMPARATOR);
        for (StarPredicate predicate : predicateList) {
            children.add(predicate);
            columnSet.addAll(predicate.getColumnList());
        }
        columns = new ArrayList<PredicateColumn>(columnSet);
    }

    public List<PredicateColumn> getColumnList() {
        return columns;
    }

    public BitKey getConstrainedColumnBitKey() {
        if (columnBitKey == null) {
            for (StarPredicate predicate : children) {
                if (columnBitKey == null) {
                    columnBitKey =
                        predicate.getConstrainedColumnBitKey().copy();
                } else {
                    columnBitKey =
                        columnBitKey.or(predicate.getConstrainedColumnBitKey());
                }
            }
        }
        return columnBitKey;
    }

    public List<StarPredicate> getChildren() {
        return children;
    }

    public Set<StarPredicateWrapper> getChildrenSet() {
        if (childrenSet == null) {
            initializeChildrenSet();
        }
        return childrenSet;
    }

    private synchronized void initializeChildrenSet() {
        if (childrenSet == null) {
            Set<StarPredicateWrapper> newChildrenSet = new HashSet<>(children.size());
            for (StarPredicate starPredicate : children) {
                newChildrenSet.add(new StarPredicateWrapper(starPredicate));
            }
            childrenSet = newChildrenSet;
        }
    }

    public int hashCode() {
        // Don't use the default list hashcode because we want a hash code
        // that's not order dependent
        if (hashValue == 0) {
            hashValue = 37;
            for (StarPredicate child : children) {
                int childHashCode = child.hashCode();
                if (childHashCode != 0) {
                    hashValue *= childHashCode;
                }
            }
            hashValue ^= children.size();
        }
        return hashValue;
    }

    public boolean equalConstraint(StarPredicate that) {
        if (!(that instanceof ListPredicate)) {
            return false;
        }
        if (!getConstrainedColumnBitKey().equals(that.getConstrainedColumnBitKey())
                || !getOp().equals(((ListPredicate)that).getOp())) {
            return false;
        }

        ListPredicate thatPred = (ListPredicate) that;
        if (getChildren().size() != thatPred.getChildren().size()) {
            return false;
        } else {
            return SetUtils.isEqualSet(getChildrenSet(), thatPred.getChildrenSet());
        }
    }

    public StarPredicate minus(StarPredicate predicate) {
        throw Util.needToImplement(this);
    }

    public void toSql(Dialect dialect, StringBuilder buf) {
        if (children.size() == 1) {
            children.get(0).toSql(dialect, buf);
        } else {
            int k = 0;
            buf.append("(");
            for (StarPredicate child : children) {
                if (k++ > 0) {
                    buf.append(" ").append(getOp()).append(" ");
                }
                child.toSql(dialect, buf);
            }
            buf.append(")");
        }
    }

    protected abstract String getOp();

    public void describe(StringBuilder buf) {
        buf.append(getOp()).append("(");
        int k = 0;
        for (StarPredicate child : children) {
            if (k++ > 0) {
                buf.append(", ");
            }
            buf.append(child);
        }
        buf.append(')');
    }


    public String toString() {
        final StringBuilder buf = new StringBuilder();
        describe(buf);
        return buf.toString();
    }

    private static class StarPredicateWrapper {
        private final StarPredicate starPredicate;

        public StarPredicateWrapper(StarPredicate starPredicate) {
            assert starPredicate != null;
            this.starPredicate = starPredicate;
        }

        public StarPredicate getStarPredicate() {
            return starPredicate;
        }

        @Override
        public int hashCode() {
            return starPredicate.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof StarPredicateWrapper)) {
                return false;
            }
            return starPredicate.equalConstraint(((StarPredicateWrapper)o).getStarPredicate());
        }
    }
}

// End ListPredicate.java
