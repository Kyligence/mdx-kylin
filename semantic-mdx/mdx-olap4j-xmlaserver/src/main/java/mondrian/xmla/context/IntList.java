package mondrian.xmla.context;

import java.util.AbstractList;

public class IntList extends AbstractList<Integer> {
    private final int[] ints;

    IntList(int[] ints) {
        this.ints = ints;
    }

    public Integer get(int index) {
        return ints[index];
    }

    public int size() {
        return ints.length;
    }
}
