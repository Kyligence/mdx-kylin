/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2013-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.rolap;

import lombok.ToString;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes where to find level information in the datasource results
 */
@ToString
public class LevelColumnLayout<T> {
    private final List<T> keys;
    private final T name;
    private final T caption;
    private final OrderKeySource orderBySource;
    private final List<T> orderBys;
    private final T value;
    private final List<T> properties;
    private final List<T> parents;
    private final boolean filterNeeded;

    public LevelColumnLayout(
        List<T> keys,
        T name,
        T caption,
        OrderKeySource orderBySource,
        List<T> orderBys,
        T value,
        List<T> properties,
        List<T> parents,
        boolean filterNeeded)
    {
        this.keys = keys;
        this.name = name;
        this.caption = caption;
        this.orderBySource = orderBySource;
        this.orderBys = orderBys;
        this.value = value;
        this.properties = properties;
        this.parents = parents;
        this.filterNeeded = filterNeeded;
    }

    /**
     * Returns the List of keys for accessing database results
     */
    public List<T> getKeys() {
        return keys;
    }

    /**
     * Returns the key for accessing the name field in database results
     */
    public T getNameKey() {
        return name;
    }

    /**
     * Returns the key for accessing the caption field in database results
     */
    public T getCaptionKey() {
        return caption;
    }

    /**
     * Returns the Order Key Source for database results
     */
    public OrderKeySource getOrderBySource() {
        return orderBySource;
    }

    /**
     * Returns List of keys for orderBy fields in database results
     */
    public List<T> getOrderByKeys() {
        return orderBys;
    }

    public T getValueKey() {
        return value;
    }

    /**
     * Returns the List of keys for accessing property fields in
     * database results
     */
    public List<T> getPropertyKeys() {
        return properties;
    }

    /**
     * Returns the List of keys for accessing parent fields in
     * database results
     */
    public List<T> getParentKeys() {
        return parents;
    }

    /**
     *
     * @return A set of all layouts, including keys, name, captions, orderbys and value
     */
    public Set<T> getAllKeys() {
        Set<T> allKeys = new HashSet<>();
        if (keys != null) {
            allKeys.addAll(keys);
        }
        if (name != null) {
            allKeys.add(name);
        }
        if (caption != null) {
            allKeys.add(caption);
        }
        if (orderBys != null) {
            allKeys.addAll(orderBys);
        }
        if (value != null) {
            allKeys.add(value);
        }
        if (properties != null) {
            allKeys.addAll(properties);
        }
        return allKeys;
    }

    public boolean isFilterNeeded() {
        return filterNeeded;
    }

    public enum OrderKeySource {
        NONE,
        KEY,
        NAME,
        MAPPED
    }
}
// End LevelColumnLayout.java
