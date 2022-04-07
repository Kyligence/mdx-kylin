package mondrian.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ArrayIntMapBuilder<V> extends ArrayIntMap<V> {
    private final List<Integer> keys;
    private final Set<Integer> keysSet;
    private final V[] values;
    private boolean sorted;

    public ArrayIntMapBuilder(int size) {
        this.keys = new ArrayList<>();
        this.keysSet = new HashSet<>();
        this.values = (V[])new Object[size];
    }

    @Override
    public boolean containsKey(int key) {
        return keysSet.contains(key);
    }

    @Override
    public V get(int key) {
        return values[key];
    }

    @Override
    public V put(int key, V value) {
        V previous = values[key];
        values[key] = value;
        sorted = false;
        if (keysSet.add(key)) {
            keys.add(key);
        }
        return previous;
    }

    @Override
    public V remove(int key) {
        V previous = values[key];
        values[key] = null;
        keysSet.remove(key);
        keys.remove(key);
        return previous;
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public V getByIndex(int index) {
        sort();
        return values[keys.get(index)];
    }

    @Override
    public void addKeysTo(Collection<Integer> collection) {
        sort();
        collection.addAll(keys);
    }

    @Override
    public <R> R[] values(Function<V, R> mapper) {
        sort();

        R[] mappedValues = (R[])new Object[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            mappedValues[i] = mapper.apply(values[keys.get(i)]);
        }

        return mappedValues;
    }

    private void sort() {
        if (!sorted) {
            Collections.sort(keys);
            sorted = true;
        }
    }

    @Override
    public ArrayIntMap<V> unmodifiable() {
        return values.length >= (1 << 3) ? toDense() : toSparse();
    }

    private SparseArrayIntMap<V> toSparse() {
        return new SparseArrayIntMap<>(values);
    }

    private DenseArrayIntMap<V> toDense() {
        sort();

        int[] denseKeys = new int[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            denseKeys[i] = keys.get(i);
        }

        V[] denseValues = (V[])new Object[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            denseValues[i] = values[denseKeys[i]];
        }

        return new DenseArrayIntMap<>(denseKeys, denseValues);
    }
}
