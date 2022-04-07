package mondrian.rolap;

import mondrian.spi.CellFormatter;

/**
 * A CellFormatterValueFormatter uses a user-defined {@link CellFormatter}
 * to format values.
 */
public class CellFormatterValueFormatter implements ValueFormatter {
    final CellFormatter cf;

    /**
     * Creates a CellFormatterValueFormatter
     *
     * @param cf Cell formatter
     */
    CellFormatterValueFormatter(CellFormatter cf) {
        this.cf = cf;
    }
    public String format(Object value, String formatString) {
        return cf.formatCell(value);
    }
}
