package mondrian.xmla.handler;

import mondrian.xmla.SaxWriter;
import mondrian.xmla.XmlaUtil;
import mondrian.xmla.context.ValueInfo;
import org.olap4j.Cell;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Property;
import org.olap4j.metadata.Property.StandardCellProperty;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Callback to handle one column, representing the combination of a
 * level and a property (e.g. [Store].[Store State].[MEMBER_UNIQUE_NAME])
 * in a flattened dataset.
 */
public class CellColumnHandler extends ColumnHandler {

    public CellColumnHandler(String name) {
        super(name);
    }

    public CellColumnHandler(String name, Property property) {
        super(name, property);
    }

    @Override
    public void metadata(SaxWriter writer) {
        writer.element("xsd:element", "minOccurs", 0, "name", encodedName, "sql:field", name);
    }

    @Override
    public void metadata_PowerBI(SaxWriter writer) {
        writer.element("xsd:element", "minOccurs", 0, "type", getXsdType(property), "name", encodedName, "sql:field", name);
    }

    @Override
    public void write(SaxWriter writer, Cell cell, Member[] members) {
        if (cell.isNull()) {
            return;
        }
        Object value = cell.getValue();
        final String dataType = (String) cell.getPropertyValue(StandardCellProperty.DATATYPE);

        final ValueInfo vi = new ValueInfo(dataType, value);
        final String valueType = vi.valueType;
        value = vi.value;
        boolean isDecimal = vi.isDecimal;
        boolean isDateTime = (dataType != null && dataType.contentEquals("DATETIME"));

        String valueString = value.toString();

        writer.startElement(encodedName, "xsi:type", valueType);
        if (isDecimal) {
            valueString = XmlaUtil.normalizeNumericString(valueString);
        }
        if (isDateTime) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
                Date date = dateFormat.parse(valueString);
                valueString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date);
            } catch (ParseException ex0) {
                valueString = value.toString();
            }
        }
        writer.characters(valueString);
        writer.endElement();
    }
}
