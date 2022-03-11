package mondrian.xmla.context;

import mondrian.xmla.XmlaUtil;

import java.sql.Types;

import static mondrian.xmla.constants.XmlaHandlerConstants.*;

public class Column {

    public final String name;
    public final String encodedName;
    public final String xsdType;

    public Column(String name, int type, int scale) {
        this.name = name;

        // replace invalid XML element name, like " ", with "_x0020_" in
        // column headers, otherwise will generate a badly-formatted xml
        // doc.
        this.encodedName = XmlaUtil.ElementNameEncoder.INSTANCE.encode(name);
        this.xsdType = sqlToXsdType(type, scale);
    }

    /**
     * Converts a SQL type to XSD type.
     *
     * @param sqlType SQL type
     * @return XSD type
     */
    private static String sqlToXsdType(final int sqlType, final int scale) {
        switch (sqlType) {
            // Integer
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return XSD_INT;
            case Types.NUMERIC:
            case Types.DECIMAL:
                // Oracle reports all numbers as NUMERIC. We check
                // the scale of the column and pick the right XSD type.
                if (scale == 0) {
                    return XSD_INT;
                } else {
                    return XSD_DECIMAL;
                }
            case Types.BIGINT:
                return XSD_INTEGER;
            // Real
            case Types.DOUBLE:
            case Types.FLOAT:
                return XSD_DOUBLE;
            // Date and time
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.DATE:
                return XSD_STRING;
            // Other
            default:
                return XSD_STRING;
        }
    }

}
