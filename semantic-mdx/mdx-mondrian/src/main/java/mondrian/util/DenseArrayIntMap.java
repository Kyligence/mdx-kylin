package mondrian.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public class DenseArrayIntMap<V> extends ArrayIntMap<V> {
    private final int[] keys;
    private final V[] values;

    DenseArrayIntMap(int[] keys, V[] values) {
        assert keys.length == values.length;
        this.keys = keys;
        this.values = values;
    }

    @Override
    public boolean containsKey(int key) {
        return Arrays.binarySearch(keys, key) >= 0;
    }

    @Override
    public V get(int key) {
        int position = Arrays.binarySearch(keys, key);
        return values[position];
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
        return keys.length;
    }

    @Override
    public V getByIndex(int index) {
        return values[index];
    }

    @Override
    public void addKeysTo(Collection<Integer> collection) {
        for (int key : keys) {
            collection.add(key);
        }
    }

    @Override
    public <R> R[] values(Function<V, R> mapper) {
        R[] mappedValues = (R[])new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            mappedValues[i] = mapper.apply(values[i]);
        }
        return mappedValues;
    }
}
