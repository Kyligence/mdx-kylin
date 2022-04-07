package mondrian.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class SparseArrayIntMap<V> extends ArrayIntMap<V> {
    private final V[] values;

    SparseArrayIntMap(V[] values) {
        this.values = values;
    }

    @Override
    public boolean containsKey(int key) {
        return values[key] != null;
    }

    @Override
    public V get(int key) {
        return values[key];
    }

    @Override
    public V put(int key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(int key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        int i = 0;
        for (V value : values) {
            if (value != null) {
                i++;
            }
        }
        return i;
    }

    @Override
    public V getByIndex(int index) {
        int i = 0;
        for (V value : values) {
            if (value != null && index == i++) {
                return value;
            }
        }

        return null;
    }

    @Override
    public void addKeysTo(Collection<Integer> collection) {
        int i = 0;
        for (V value : values) {
            if (value != null) {
                collection.add(i);
            }
            i++;
        }
    }

    @Override
    public <R> R[] values(Function<V, R> mapper) {
        List<R> mappedValues = new ArrayList<>();
        for (V value : values) {
            if (value != null) {
                mappedValues.add(mapper.apply(value));
            }
        }

        return mappedValues.toArray((R[])new Object[0]);
    }
}
