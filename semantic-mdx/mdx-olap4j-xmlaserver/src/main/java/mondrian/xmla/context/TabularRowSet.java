package mondrian.xmla.context;

import mondrian.xmla.RowsetDefinition;
import mondrian.xmla.SaxWriter;
import mondrian.xmla.XmlaUtil;
import org.xml.sax.SAXException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mondrian.xmla.XmlaConstants.*;
import static mondrian.xmla.constants.XmlaHandlerConstants.NS_XML_SQL;
import static mondrian.xmla.constants.XmlaHandlerConstants.XSD_STRING;

public class TabularRowSet implements QueryResult {
    private final List<Column> columns = new ArrayList<>();
    private final List<Object[]> rows;
    private int totalCount;

    /**
     * Creates a TabularRowSet based upon a SQL statement result.
     *
     * <p>Does not close the ResultSet, on success or failure. Client
     * must do it.
     *
     * @param rs         Result set
     * @param totalCount Total number of rows. If >= 0, writes the
     *                   "totalCount" attribute into the XMLA response.
     * @throws SQLException on error
     */
    public TabularRowSet(ResultSet rs, int totalCount) throws SQLException {
        this.totalCount = totalCount;
        ResultSetMetaData md = rs.getMetaData();
        int columnCount = md.getColumnCount();

        // populate column defs
        for (int i = 0; i < columnCount; i++) {
            columns.add(new Column(md.getColumnLabel(i + 1), md.getColumnType(i + 1), md.getScale(i + 1)));
        }

        // Populate data; assume that SqlStatement is already positioned
        // on first row (or isDone() is true), and assume that the
        // number of rows returned is limited.
        rows = new ArrayList<Object[]>();
        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getObject(i + 1);
            }
            rows.add(row);
        }
    }

    /**
     * Alternate constructor for advanced drill-through.
     *
     * @param tableFieldMap Map from table name to a list of the names of
     *                      the fields in the table
     * @param tableList     List of table names
     */
    public TabularRowSet(Map<String, List<String>> tableFieldMap, List<String> tableList) {
        for (String tableName : tableList) {
            List<String> fieldNames = tableFieldMap.get(tableName);
            for (String fieldName : fieldNames) {
                // don't know the real type
                columns.add(new Column(tableName + "." + fieldName, Types.VARCHAR, 0));
            }
        }

        rows = new ArrayList<>();
        Object[] row = new Object[columns.size()];
        for (int k = 0; k < row.length; k++) {
            row[k] = k;
        }
        rows.add(row);
    }

    @Override
    public void close() {
        // no resources to close
    }

    @Override
    public void unparse(SaxWriter writer) throws SAXException {
        // write total count row if enabled
        if (totalCount >= 0) {
            String countStr = Integer.toString(totalCount);
            writer.startElement("row");
            for (Column column : columns) {
                writer.startElement(column.encodedName);
                writer.characters(countStr);
                writer.endElement();
            }
            writer.endElement(); // row
        }

        for (Object[] row : rows) {
            writer.startElement("row");
            for (int i = 0; i < row.length; i++) {
                writer.startElement(columns.get(i).encodedName,
                        new Object[]{"xsi:type", columns.get(i).xsdType});
                Object value = row[i];
                if (value == null) {
                    writer.characters("null");
                } else {
                    String valueString = value.toString();
                    if (value instanceof Number) {
                        valueString = XmlaUtil.normalizeNumericString(valueString);
                    }
                    writer.characters(valueString);
                }
                writer.endElement();
            }
            writer.endElement(); // row
        }
    }

    /**
     * Writes the tabular drillthrough schema
     *
     * @param writer Writer
     */
    @Override
    public void metadata(SaxWriter writer) {
        writer.startElement("xsd:schema", "xmlns:xsd", NS_XSD, "targetNamespace", NS_XMLA_ROWSET, "xmlns",
                NS_XMLA_ROWSET, "xmlns:xsi", NS_XSI, "xmlns:sql", NS_XML_SQL, "elementFormDefault", "qualified");

        { // <root>
            writer.startElement("xsd:element", "name", "root");
            writer.startElement("xsd:complexType");
            writer.startElement("xsd:sequence");
            writer.element("xsd:element", "maxOccurs", "unbounded", "minOccurs", 0, "name", "row", "type", "row");
            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
            writer.endElement(); // xsd:element name=root
        }

        { // xsd:simpleType name="uuid"
            writer.startElement("xsd:simpleType", "name", "uuid");
            writer.startElement("xsd:restriction", "base", XSD_STRING);
            writer.element("xsd:pattern", "value", RowsetDefinition.UUID_PATTERN);
            writer.endElement(); // xsd:restriction
            writer.endElement(); // xsd:simpleType
        }

        { // xsd:complexType name="row"
            writer.startElement("xsd:complexType", "name", "row");
            writer.startElement("xsd:sequence");
            for (Column column : columns) {
                writer.element("xsd:element", "minOccurs", 0, "name", column.encodedName, "sql:field", column.name,
                        "type", column.xsdType);
            }

            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
        }
        writer.endElement(); // xsd:schema
    }

    public void metadata_PowerBI(SaxWriter writer) {
        writer.startElement("xsd:schema", "xmlns:xsd", NS_XSD, "targetNamespace", NS_XMLA_ROWSET, "xmlns",
                NS_XMLA_ROWSET, "xmlns:xsi", NS_XSI, "xmlns:sql", NS_XML_SQL, "elementFormDefault", "qualified");

        { // <root>
            writer.startElement("xsd:element", "name", "root");
            writer.startElement("xsd:complexType");
            writer.startElement("xsd:sequence");
            writer.element("xsd:element", "maxOccurs", "unbounded", "minOccurs", 0, "name", "row", "type", "row");
            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
            writer.endElement(); // xsd:element name=root
        }
//
//            { // xsd:simpleType name="uuid"
//                writer.startElement("xsd:simpleType", "name", "uuid");
//                writer.startElement("xsd:restriction", "base", XSD_STRING);
//                writer.element("xsd:pattern", "value", RowsetDefinition.UUID_PATTERN);
//                writer.endElement(); // xsd:restriction
//                writer.endElement(); // xsd:simpleType
//            }

        { // xsd:complexType name="row"
            writer.startElement("xsd:complexType", "name", "row");
            writer.startElement("xsd:sequence");
            for (Column column : columns) {
                writer.element("xsd:element", "minOccurs", 0, "name", column.encodedName, "sql:field", column.name,
                        "type", column.xsdType);
            }

            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
        }
        writer.endElement(); // xsd:schema
    }
}
