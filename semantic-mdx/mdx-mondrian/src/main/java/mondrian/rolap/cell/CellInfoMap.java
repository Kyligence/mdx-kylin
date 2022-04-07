package mondrian.rolap.cell;

import mondrian.rolap.CellKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link CellInfoContainer} which uses a {@link Map} to
 * store CellInfo Objects.
 *
 * <p>Note that the CellKey point instance variable is the same
 * Object (NOT a copy) that is used and modified during
 * the recursive calls to executeStripe - the
 * <code>create</code> method relies on this fact.
 */
public class CellInfoMap implements CellInfoContainer {

    private final Map<CellKey, CellInfo> cellInfoMap;

    private final CellKey point;

    /**
     * Creates a CellInfoMap
     *
     * @param point Cell position
     */
    public CellInfoMap(CellKey point) {
        this.point = point;
        this.cellInfoMap = new HashMap<>();
    }

    @Override
    public int size() {
        return this.cellInfoMap.size();
    }

    @Override
    public void trimToSize() {
        // empty
    }

    @Override
    public void clear() {
        this.cellInfoMap.clear();
    }

    @Override
    public CellInfo create(int[] pos) {
        CellKey key = this.point.copy();
        CellInfo ci = this.cellInfoMap.get(key);
        if (ci == null) {
            ci = new CellInfo(0);
            this.cellInfoMap.put(key, ci);
        }
        return ci;
    }

    @Override
    public CellInfo lookup(int[] pos) {
        CellKey key = CellKey.Generator.newCellKey(pos);
        return this.cellInfoMap.get(key);
    }
}
