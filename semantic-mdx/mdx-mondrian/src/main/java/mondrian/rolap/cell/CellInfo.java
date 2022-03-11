package mondrian.rolap.cell;

import mondrian.rolap.RolapCell;
import mondrian.rolap.ValueFormatter;

/**
 * A CellInfo contains all of the information that a Cell requires.
 * It is placed in the cellInfos map during evaluation and
 * serves as a constructor parameter for {@link RolapCell}.
 *
 * <p>During the evaluation stage they are mutable but after evaluation has
 * finished they are not changed.
 */
public class CellInfo {
    public long key;
    public Object value;
    public String formatString;
    public ValueFormatter valueFormatter;
    public boolean ready;

    /**
     * Creates a CellInfo representing the position of a cell.
     *
     * @param key Ordinal representing the position of a cell
     */
    CellInfo(long key) {
        this(key, null, null, ValueFormatter.EMPTY);
    }

    /**
     * Creates a CellInfo with position, value, format string and formatter
     * of a cell.
     *
     * @param key            Ordinal representing the position of a cell
     * @param value          Value of cell, or null if not yet known
     * @param formatString   Format string of cell, or null
     * @param valueFormatter Formatter for cell, or null
     */
    CellInfo(long key,
             Object value,
             String formatString,
             ValueFormatter valueFormatter) {
        this.key = key;
        this.value = value;
        this.formatString = formatString;
        this.valueFormatter = valueFormatter;
    }

    @Override
    public int hashCode() {
        // Combine the upper 32 bits of the key with the lower 32 bits.
        // We used to use 'key ^ (key >>> 32)' but that was bad, because
        // CellKey.Two encodes (i, j) as
        // (i * Integer.MAX_VALUE + j), which is practically the same as
        // (i << 32, j). If i and j were
        // both k bits long, all of the hashcodes were k bits long too!
        return (int) (key ^ (key >>> 11) ^ (key >>> 24));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CellInfo) {
            CellInfo that = (CellInfo) o;
            return that.key == this.key;
        } else {
            return false;
        }
    }

    /**
     * Returns the formatted value of the Cell
     *
     * @return formatted value of the Cell
     */
    public String getFormatValue() {
        return valueFormatter.format(value, formatString);
    }
}
