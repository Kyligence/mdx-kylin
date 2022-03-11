package mondrian.rolap.cell;

import mondrian.util.ObjectPool;

/**
 * Implementation of {@link CellInfoContainer} which uses an
 * {@link ObjectPool} to store {@link CellInfo} Objects.
 *
 * <p>There is an inner interface (<code>CellKeyMaker</code>) and
 * implementations for 0 through 4 axes that convert the Cell
 * position integer array into a long.
 *
 * <p>
 * It should be noted that there is an alternate approach.
 * As the <code>executeStripe</code>
 * method is recursively called, at each call it is known which
 * axis is being iterated across and it is known whether or
 * not the Position object for that axis is a List or just
 * an Iterable. It it is a List, then one knows the real
 * size of the axis. If it is an Iterable, then one has to
 * use one of the MAX_AXIS_SIZE values. Given that this information
 * is available when one recursives down to the next
 * <code>executeStripe</code> call, the Cell ordinal, the position
 * integer array could converted to an <code>long</code>, could
 * be generated on the call stack!! Just a thought for the future.
 */
public class CellInfoPool implements CellInfoContainer {
    /**
     * The maximum number of Members, 2,147,483,647, that can be any given
     * Axis when the number of Axes is 2.
     */
    protected static final long MAX_AXIS_SIZE_2 = 2147483647;
    /**
     * The maximum number of Members, 2,000,000, that can be any given
     * Axis when the number of Axes is 3.
     */
    protected static final long MAX_AXIS_SIZE_3 = 2000000;
    /**
     * The maximum number of Members, 50,000, that can be any given
     * Axis when the number of Axes is 4.
     */
    protected static final long MAX_AXIS_SIZE_4 = 50000;

    /**
     * Implementations of CellKeyMaker convert the Cell
     * position integer array to a <code>long</code>.
     *
     * <p>Generates a long ordinal based upon the values of the integers
     * stored in the cell position array. With this mechanism, the
     * Cell information can be stored using a long key (rather than
     * the array integer of positions) thus saving memory. The trick
     * is to use a 'large number' per axis in order to convert from
     * position array to long key where the 'large number' is greater
     * than the number of members in the axis.
     * The largest 'long' is java.lang.Long.MAX_VALUE which is
     * 9,223,372,036,854,776,000. The product of the maximum number
     * of members per axis must be less than this maximum 'long'
     * value (otherwise one gets hashing collisions).</p>
     *
     * <p>For a single axis, the maximum number of members is equal to
     * the max 'long' number, 9,223,372,036,854,776,000.
     *
     * <p>For two axes, the maximum number of members is the square root
     * of the max 'long' number, 9,223,372,036,854,776,000, which is
     * slightly bigger than 2,147,483,647 (which is the maximum integer).
     *
     * <p>For three axes, the maximum number of members per axis is the
     * cube root of the max 'long' which is about 2,000,000.
     *
     * <p>For four axes the forth root is about 50,000.
     *
     * <p>For five or more axes, the maximum number of members per axis
     * based upon the root of the maximum 'long' number,
     * start getting too small to guarantee that it will be
     * smaller than the number of members on a given axis and so
     * we must resort to the Map-base Cell container.
     */
    interface CellKeyMaker {
        long generate(int[] pos);
    }

    /**
     * For axis of size 0.
     */
    static class Zero implements CellKeyMaker {
        public long generate(int[] pos) {
            return 0;
        }
    }

    /**
     * For axis of size 1.
     */
    static class One implements CellKeyMaker {
        public long generate(int[] pos) {
            return pos[0];
        }
    }

    /**
     * For axis of size 2.
     */
    static class Two implements CellKeyMaker {
        public long generate(int[] pos) {
            long l = pos[0];
            l += (MAX_AXIS_SIZE_2 * (long) pos[1]);
            return l;
        }
    }

    /**
     * For axis of size 3.
     */
    static class Three implements CellKeyMaker {
        public long generate(int[] pos) {
            long l = pos[0];
            l += (MAX_AXIS_SIZE_3 * (long) pos[1]);
            l += (MAX_AXIS_SIZE_3 * MAX_AXIS_SIZE_3 * (long) pos[2]);
            return l;
        }
    }

    /**
     * For axis of size 4.
     */
    static class Four implements CellKeyMaker {
        public long generate(int[] pos) {
            long l = pos[0];
            l += (MAX_AXIS_SIZE_4 * (long) pos[1]);
            l += (MAX_AXIS_SIZE_4 * MAX_AXIS_SIZE_4 * (long) pos[2]);
            l += (MAX_AXIS_SIZE_4 * MAX_AXIS_SIZE_4 * MAX_AXIS_SIZE_4 * (long) pos[3]);
            return l;
        }
    }

    private final ObjectPool<CellInfo> cellInfoPool;

    private final CellKeyMaker cellKeyMaker;

    public CellInfoPool(int axisLength) {
        this.cellInfoPool = new ObjectPool<>();
        this.cellKeyMaker = createCellKeyMaker(axisLength);
    }

    static CellKeyMaker createCellKeyMaker(int axisLength) {
        switch (axisLength) {
            case 0:
                return new Zero();
            case 1:
                return new One();
            case 2:
                return new Two();
            case 3:
                return new Three();
            case 4:
                return new Four();
            default:
                throw new RuntimeException("Creating CellInfoPool with axisLength=" + axisLength);
        }
    }

    @Override
    public int size() {
        return this.cellInfoPool.size();
    }

    @Override
    public void trimToSize() {
        this.cellInfoPool.trimToSize();
    }

    @Override
    public void clear() {
        this.cellInfoPool.clear();
    }

    @Override
    public CellInfo create(int[] pos) {
        long key = this.cellKeyMaker.generate(pos);
        return this.cellInfoPool.add(new CellInfo(key));
    }

    @Override
    public CellInfo lookup(int[] pos) {
        long key = this.cellKeyMaker.generate(pos);
        CellInfo cellInfo = new CellInfo(key);
        if (cellInfoPool.contains(cellInfo)) {
            return this.cellInfoPool.add(cellInfo);
        } else {
            return cellInfo;
        }
    }

}
