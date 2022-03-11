package mondrian.rolap;

import mondrian.spi.CellFormatter;

import java.util.Locale;

/**
 * Formatter to convert values into formatted strings.
 *
 * <p>Every Cell has a value, a format string (or CellFormatter) and a
 * formatted value string.
 * There are a wide range of possible values (pick a Double, any
 * Double - its a value). Because there are lots of possible values,
 * there are also lots of possible formatted value strings. On the
 * other hand, there are only a very small number of format strings
 * and CellFormatter's. These formatters are to be cached
 * in a synchronized HashMaps in order to limit how many copies
 * need to be kept around.
 *
 * <p>
 * There are two implementations of the ValueFormatter interface:<ul>
 * <li>{@link CellFormatterValueFormatter}, which formats using a
 * user-registered {@link CellFormatter}; and
 * <li> {@link FormatValueFormatter}, which takes the {@link Locale} object.
 * </ul>
 */
public interface ValueFormatter {
    /**
     * Formats a value according to a format string.
     *
     * @param value Value
     * @param formatString Format string
     * @return Formatted value
     */
    String format(Object value, String formatString);

    /**
     * Formatter that always returns the empty string.
     */
    public static final ValueFormatter EMPTY = new ValueFormatter() {
        public String format(Object value, String formatString) {
            return "";
        }
    };
}
