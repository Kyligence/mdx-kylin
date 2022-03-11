package mondrian.rolap;

import mondrian.olap.Util;
import mondrian.util.Format;

import java.util.Locale;

/**
 * A FormatValueFormatter takes a {@link Locale}
 * as a parameter and uses it to get the {@link mondrian.util.Format}
 * to be used in formatting an Object value with a
 * given format string.
 */
public class FormatValueFormatter implements ValueFormatter {
    final Locale locale;

    /**
     * Creates a FormatValueFormatter.
     *
     * @param locale Locale
     */
    FormatValueFormatter(Locale locale) {
        this.locale = locale;
    }

    public String format(Object value, String formatString) {
        if (value == Util.nullValue) {
            value = null;
        }
        if (value instanceof Throwable) {
            return "#ERR: " + value.toString();
        }
        Format format = getFormat(formatString);
        return format.format(value);
    }

    private Format getFormat(String formatString) {
        return Format.get(formatString, locale);
    }
}
