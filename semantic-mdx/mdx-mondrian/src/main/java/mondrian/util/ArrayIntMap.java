package mondrian.util;

import java.util.Collection;
import java.util.function.Function;

public abstract class ArrayIntMap<V> {
    public abstract boolean containsKey(int key);

    public abstract V get(int key);

    public abstract V put(int key, V value);

    public abstract V remove(int key);

    public abstract int size();

    public abstract V getByIndex(int index);

    public abstract void addKeysTo(Collection<Integer> collection);

    public abstract <R> R[] values(Function<V, R> mapper);

    public ArrayIntMap<V> unmodifiable() {
        return this;
    }
}
