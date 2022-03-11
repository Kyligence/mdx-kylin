package mondrian.rolap.cell;

/**
 * API for the creation and
 * lookup of {@link CellInfo} objects. There are two implementations,
 * one that uses a Map for storage and the other uses an ObjectPool.
 */
public interface CellInfoContainer {
    /**
     * Returns the number of CellInfo objects in this container.
     *
     * @return the number of CellInfo objects.
     */
    int size();

    /**
     * Reduces the size of the internal data structures needed to
     * support the current entries. This should be called after
     * all CellInfo objects have been added to container.
     */
    void trimToSize();

    /**
     * Removes all CellInfo objects from container. Does not
     * change the size of the internal data structures.
     */
    void clear();

    /**
     * Creates a new CellInfo object, adds it to the container
     * a location <code>pos</code> and returns it.
     *
     * @param pos where to store CellInfo object.
     * @return the newly create CellInfo object.
     */
    CellInfo create(int[] pos);

    /**
     * Gets the CellInfo object at the location <code>pos</code>.
     *
     * @param pos where to find the CellInfo object.
     * @return the CellInfo found or null.
     */
    CellInfo lookup(int[] pos);
}
