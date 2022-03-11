package mondrian.xmla.handler;

import mondrian.xmla.RowsetDefinition;
import mondrian.xmla.SaxWriter;
import mondrian.xmla.XmlaUtil;
import org.olap4j.Cell;
import org.olap4j.OlapException;
import org.olap4j.metadata.Datatype;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Property;

public abstract class ColumnHandler {

    protected static final ThreadLocal<Integer> columnNum = new ThreadLocal<>();

    public static void resetColumnNum() {
        columnNum.remove();
    }

    protected final String name;
    protected final String encodedName;
    protected final Property property;

    protected ColumnHandler(String name) {
        this.name = name;
        this.encodedName = XmlaUtil.ElementNameEncoder.INSTANCE.encode(name);
        this.property = null;
    }

    protected ColumnHandler(String name, Property property) {
        this.name = name;
        if (columnNum.get() == null) {
            columnNum.set(0);
        } else {
            int num = columnNum.get() + 1;
            columnNum.set(num);
        }
        int c = columnNum.get();
        this.encodedName = "C" + c;
        this.property = property;
    }

    public abstract void write(SaxWriter writer, Cell cell, Member[] members) throws OlapException;

    public abstract void metadata(SaxWriter writer);

    public abstract void metadata_PowerBI(SaxWriter writer);

    public static RowsetDefinition.Type getXsdTypeEnum(Property property) {
        Datatype datatype = property.getDatatype();
        switch (datatype) {
            case UNSIGNED_INTEGER:
                return RowsetDefinition.Type.UnsignedInteger;
            case BOOLEAN:
                return RowsetDefinition.Type.Boolean;
            default:
                return RowsetDefinition.Type.String;
        }
    }

    public static String getXsdType(Property property) {
        return getXsdTypeEnum(property).columnType;
    }

}
