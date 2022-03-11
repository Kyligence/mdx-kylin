package mondrian.xmla.constants;

public class XmlaHandlerConstants {

    // Schema Constants

    public static final String NS_XML_SQL = "urn:schemas-microsoft-com:xml-sql";

    //
    // Some xml schema data types.
    //
    public static final String XSD_BOOLEAN = "xsd:boolean";
    public static final String XSD_STRING = "xsd:string";
    public static final String XSD_UNSIGNED_INT = "xsd:unsignedInt";

    public static final String XSD_BYTE = "xsd:byte";
    public static final byte XSD_BYTE_MAX_INCLUSIVE = 127;
    public static final byte XSD_BYTE_MIN_INCLUSIVE = -128;

    public static final String XSD_SHORT = "xsd:short";
    public static final short XSD_SHORT_MAX_INCLUSIVE = 32767;
    public static final short XSD_SHORT_MIN_INCLUSIVE = -32768;

    public static final String XSD_INT = "xsd:int";
    public static final int XSD_INT_MAX_INCLUSIVE = 2147483647;
    public static final int XSD_INT_MIN_INCLUSIVE = -2147483648;

    public static final String XSD_LONG = "xsd:long";
    public static final long XSD_LONG_MAX_INCLUSIVE = 9223372036854775807L;
    public static final long XSD_LONG_MIN_INCLUSIVE = -9223372036854775808L;

    // xsd:double: IEEE 64-bit floating-point
    public static final String XSD_DOUBLE = "xsd:double";

    public static final String XSD_FLOAT = "xsd:float";

    // xsd:decimal: Decimal numbers (BigDecimal)
    public static final String XSD_DECIMAL = "xsd:decimal";

    // xsd:integer: Signed integers of arbitrary length (BigInteger)
    public static final String XSD_INTEGER = "xsd:integer";

    //xsd:dateTime: Date and time in format:YYYY-mm-ddThh:MM:ss
    public static final String XSD_dateTime = "xsd:dateTime";

    // Parameter Constants

    public static final String SESSION_PROPERTY_KEY = "Session";

    /**
     * Name of property used by JDBC to hold user name.
     */
    public static final String JDBC_USER = "user";

    /**
     * Name of property used by JDBC to hold password.
     */
    public static final String JDBC_PASSWORD = "password";

    /**
     * Name of property used by JDBC to hold locale. It is not hard-wired into
     * DriverManager like "user" and "password", but we do expect any olap4j
     * driver that supports i18n to use this property name.
     */
    public static final String JDBC_LOCALE = "locale";

}
