package mondrian.xmla.context;

import mondrian.xmla.RowsetDefinition;
import mondrian.xmla.SaxWriter;
import mondrian.xmla.XmlaRequestContext;
import mondrian.xmla.handler.CellColumnHandler;
import mondrian.xmla.handler.ColumnHandler;
import mondrian.xmla.handler.MemberColumnHandler;
import org.olap4j.*;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Property;
import org.olap4j.metadata.Property.StandardMemberProperty;
import org.olap4j.xmla.server.impl.Util;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static mondrian.xmla.XmlaConstants.*;
import static mondrian.xmla.constants.XmlaHandlerConstants.NS_XML_SQL;
import static mondrian.xmla.constants.XmlaHandlerConstants.XSD_STRING;

public class MDDataSet_Tabular extends MDDataSet {
    protected final boolean empty;
    protected final int[] pos;
    protected final List<Integer> posList;
    protected final int axisCount;
    protected int cellOrdinal;

    private static final List<Property> MemberCaptionIdArray = Collections
            .singletonList(StandardMemberProperty.MEMBER_CAPTION);

    protected final Member[] members;
    protected final ColumnHandler[] columnHandlers;

    public MDDataSet_Tabular(CellSet cellSet, OlapConnection connection) {
        super(cellSet, connection);
        final List<CellSetAxis> axes = cellSet.getAxes();
        axisCount = axes.size();
        pos = new int[axisCount];
        posList = new IntList(pos);

        // Count dimensions, and deduce list of levels which appear on
        // non-COLUMNS axes.
        boolean empty = false;
        int dimensionCount = 0;
        for (int i = axes.size() - 1; i > 0; i--) {
            CellSetAxis axis = axes.get(i);
            if (axis.getPositions().size() == 0) {
                // If any axis is empty, the whole data set is empty.
                empty = true;
                continue;
            }
            dimensionCount += axis.getPositions().get(0).getMembers().size();
        }
        this.empty = empty;

        // Build a list of the lowest level used on each non-COLUMNS axis.
        Level[] levels = new Level[dimensionCount];
        List<ColumnHandler> columnHandlerList = new ArrayList<ColumnHandler>();
        int memberOrdinal = 0;
        if (!empty) {
            for (int i = axes.size() - 1; i > 0; i--) {
                final CellSetAxis axis = axes.get(i);
                final int z0 = memberOrdinal; // save ordinal so can rewind
                final List<Position> positions = axis.getPositions();
                int jj = 0;
                for (Position position : positions) {
                    memberOrdinal = z0; // rewind to start
                    for (Member member : position.getMembers()) {
                        if (jj == 0 || member.getLevel().getDepth() > levels[memberOrdinal].getDepth()) {
                            levels[memberOrdinal] = member.getLevel();
                        }
                        memberOrdinal++;
                    }
                    jj++;
                }

                // Now we know the lowest levels on this axis, add
                // properties.
                List<Property> dimProps = axis.getAxisMetaData().getProperties();
                if (dimProps.size() == 0) {
                    dimProps = MemberCaptionIdArray;
                }
                for (int j = z0; j < memberOrdinal; j++) {
                    Level level = levels[j];
                    for (int k = 0; k <= level.getDepth(); k++) {
                        final Level level2 = level.getHierarchy().getLevels().get(k);
                        if (level2.getLevelType() == Level.Type.ALL) {
                            continue;
                        }
                        XmlaRequestContext context = XmlaRequestContext.getContext();
                        if (context != null && XmlaRequestContext.ClientType.POWERBI.equals(context.clientType)) {
                            for (Property dimProp : dimProps) {
                                columnHandlerList.add(new MemberColumnHandler(dimProp, level2, j, XmlaRequestContext.ClientType.POWERBI));
                            }
                        } else {
                            for (Property dimProp : dimProps) {
                                columnHandlerList.add(new MemberColumnHandler(dimProp, level2, j));
                            }
                        }
                    }
                }
            }
        }
        this.members = new Member[memberOrdinal + 1];

        // Deduce the list of column headings.
        if (axes.size() > 0) {
            CellSetAxis columnsAxis = axes.get(0);
            for (Position position : columnsAxis.getPositions()) {
                List<Property> dimProps = position.getMembers().get(0).getProperties();
                String name = null;
                int j = 0;
                for (Member member : position.getMembers()) {
                    if (!member.getUniqueName().startsWith("[Measures]")) {
                        continue;
                    }
                    if (j == 0) {
                        name = member.getUniqueName();
                    } else {
                        name = name + "." + member.getUniqueName();
                    }
                    j++;
                }
                XmlaRequestContext context = XmlaRequestContext.getContext();
                if (Objects.nonNull(context) && XmlaRequestContext.ClientType.POWERBI.equals(context.clientType)) {
                    columnHandlerList.add(new CellColumnHandler(name, dimProps.get(0)));
                } else {
                    columnHandlerList.add(new CellColumnHandler(name));
                }
            }
        }

        this.columnHandlers = columnHandlerList.toArray(new ColumnHandler[columnHandlerList.size()]);
    }

    @Override
    public void metadata(SaxWriter writer) {
        writer.startElement("xsd:schema", "targetNamespace", NS_XMLA_ROWSET,
                "xmlns:sql", NS_XML_SQL, "elementFormDefault", "qualified");

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

        {
            writer.startElement("xsd:complexType", "name", "xmlDocument");
            writer.startElement("xsd:sequence");
            writer.element("xsd:any");
            writer.endElement();
            writer.endElement();
        }

        { // xsd:complexType name="row"
            writer.startElement("xsd:complexType", "name", "row");
            writer.startElement("xsd:sequence");
            for (ColumnHandler columnHandler : columnHandlers) {
                columnHandler.metadata(writer);
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
            for (ColumnHandler columnHandler : columnHandlers) {
                columnHandler.metadata_PowerBI(writer);
            }
            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
        }
        writer.endElement(); // xsd:schema
        //每次查询的行数，在请求结束之后置空，去除对下次查询行数的影响
        ColumnHandler.resetColumnNum();
    }

    public static void metadataFirst(SaxWriter writer, boolean clientIsPowerBI) {
        if (clientIsPowerBI) {
            writer.startElement("xsd:schema", "xmlns:xsd", NS_XSD, "targetNamespace", NS_XMLA_ROWSET, "xmlns",
                    NS_XMLA_ROWSET, "xmlns:xsi", NS_XSI, "xmlns:sql", NS_XML_SQL, "elementFormDefault", "qualified");
        } else {
            writer.startElement("xsd:schema", "targetNamespace", NS_XMLA_ROWSET,
                    "xmlns:sql", NS_XML_SQL, "elementFormDefault", "qualified");
        }

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

        if (!clientIsPowerBI) {
            writer.startElement("xsd:complexType", "name", "xmlDocument");
            writer.startElement("xsd:sequence");
            writer.element("xsd:any");
            writer.endElement();
            writer.endElement();
        }

        {
            writer.startElement("xsd:complexType", "name", "row");
            writer.startElement("xsd:sequence");
        }
    }

    public static void metadataFinal(SaxWriter writer) {
        {
            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
        }

        writer.endElement(); // xsd:schema
    }

    public void unparse(SaxWriter writer) throws SAXException, OlapException {
        if (empty) {
            return;
        }
        cellData(writer);
    }

    private void cellData(SaxWriter writer) throws SAXException, OlapException {
        cellOrdinal = 0;
        iterate(writer);
    }

    /**
     * Iterates over the resust writing tabular rows.
     *
     * @param writer Writer
     */
    private void iterate(SaxWriter writer) throws OlapException {
        if (axisCount == 0) {
            // For MDX like: SELECT FROM Sales
            emitCell(writer, cellSet.getCell(posList));
        } else {
            // throw new SAXException("Too many axes: " + axisCount);
            iterate(writer, axisCount - 1, 0);
        }
    }

    private void iterate(SaxWriter writer, int axis, final int xxx) throws OlapException {
        final List<Position> positions = cellSet.getAxes().get(axis).getPositions();
        int axisLength = axis == 0 ? 1 : positions.size();

        for (int i = 0; i < axisLength; i++) {
            final Position position = positions.get(i);
            int ho = xxx;
            final List<Member> members = position.getMembers();
            for (int j = 0; j < members.size() && ho < this.members.length; j++, ho++) {
                this.members[ho] = position.getMembers().get(j);
            }

            ++cellOrdinal;
            Util.discard(cellOrdinal);

            if (axis >= 2) {
                iterate(writer, axis - 1, ho);
            } else {
                writer.startElement("row");
                pos[axis] = i;
                pos[0] = 0;
                for (ColumnHandler columnHandler : columnHandlers) {
                    if (columnHandler instanceof MemberColumnHandler) {
                        columnHandler.write(writer, null, this.members);
                    } else if (columnHandler instanceof CellColumnHandler) {
                        columnHandler.write(writer, cellSet.getCell(posList), null);
                        pos[0]++;
                    }
                }
                writer.endElement();
            }
        }
    }

    private void emitCell(SaxWriter writer, Cell cell) throws OlapException {
        ++cellOrdinal;
        Util.discard(cellOrdinal);

        // Ignore empty cells.
        final Object cellValue = cell.getValue();
        if (cellValue == null) {
            return;
        }

        writer.startElement("row");
        for (ColumnHandler columnHandler : columnHandlers) {
            columnHandler.write(writer, cell, members);
        }
        writer.endElement();
    }
}
