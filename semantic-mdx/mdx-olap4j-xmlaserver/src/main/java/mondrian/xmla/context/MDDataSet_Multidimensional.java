package mondrian.xmla.context;

import mondrian.xmla.RowsetDefinition;
import mondrian.xmla.SaxWriter;
import mondrian.xmla.XmlaRequestContext;
import mondrian.xmla.XmlaUtil;
import org.apache.log4j.Logger;
import org.olap4j.*;
import org.olap4j.metadata.*;
import org.olap4j.metadata.Property.StandardCellProperty;
import org.olap4j.metadata.Property.StandardMemberProperty;
import org.olap4j.xmla.server.impl.CompositeList;
import org.olap4j.xmla.server.impl.Pair;
import org.olap4j.xmla.server.impl.Util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import static mondrian.xmla.constants.XmlaSchemaConstants.COMPACT_MD_DATA_SET_XML_SCHEMA;
import static mondrian.xmla.constants.XmlaSchemaConstants.PRETTY_MD_DATA_SET_XML_SCHEMA;

public class MDDataSet_Multidimensional extends MDDataSet {

    private static final Logger LOGGER = Logger.getLogger(MDDataSet_Multidimensional.class);

    private static final Integer DEFAULT_CARDINALITY = 1000;

    protected List<Hierarchy> slicerAxisHierarchies;
    protected final boolean omitDefaultSlicerInfo;
    protected final boolean json;
    protected XmlaUtil.ElementNameEncoder encoder = XmlaUtil.ElementNameEncoder.INSTANCE;
    protected XmlaExtra extra;

    public MDDataSet_Multidimensional(CellSet cellSet, OlapConnection connection, XmlaExtra extra,
                                      boolean omitDefaultSlicerInfo, boolean json) {
        super(cellSet, connection);
        this.extra = extra;
        this.omitDefaultSlicerInfo = omitDefaultSlicerInfo;
        this.json = json;
    }

    @Override
    public void unparse(SaxWriter writer) throws OlapException {
        olapInfo(writer);
        axes(writer);
        cellData(writer);
    }

    @Override
    public void metadata(SaxWriter writer) {
        if (writer.isCompact()) {
            writer.verbatim(COMPACT_MD_DATA_SET_XML_SCHEMA);
        } else {
            writer.verbatim(PRETTY_MD_DATA_SET_XML_SCHEMA);
        }
    }

    private void olapInfo(SaxWriter writer) throws OlapException {
        // What are all of the cube's hierachies
        Cube cube = cellSet.getMetaData().getCube();
        SimpleDateFormat timeDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = timeDate.format(new Date());
        currentTime = currentTime.replace(' ', 'T');

        writer.startElement("OlapInfo");
        writer.startElement("CubeInfo");
        writer.startElement("Cube");
        writer.textElement("CubeName", cube.getName());
        writer.textElement("LastDataUpdate", currentTime);
        writer.textElement("LastSchemaUpdate", currentTime);

        writer.endElement();
        writer.endElement(); // CubeInfo

        // create AxesInfo for axes
        // -----------
        writer.startSequence("AxesInfo", "AxisInfo");
        final List<CellSetAxis> axes = cellSet.getAxes();
        List<Hierarchy> axisHierarchyList = new ArrayList<>();
        for (int i = 0; i < axes.size(); i++) {
            List<Hierarchy> hiers = axisInfo(writer, axes.get(i), "Axis" + i);
            axisHierarchyList.addAll(hiers);
        }
        ///////////////////////////////////////////////
        // create AxesInfo for slicer axes
        //
        List<Hierarchy> hierarchies;
        CellSetAxis slicerAxis = cellSet.getFilterAxis();
        if (omitDefaultSlicerInfo) {
            hierarchies = axisInfo(writer, slicerAxis, "SlicerAxis");
        } else {
            hierarchies = getUnseenDimensionHierarchies(cube.getDimensions(), axisHierarchyList);

            writer.startElement("AxisInfo", "name", "SlicerAxis");
            writeHierarchyInfo(writer, hierarchies, getProps(slicerAxis.getAxisMetaData()));
            writer.endElement(); // AxisInfo
        }
        slicerAxisHierarchies = hierarchies;
        //
        ///////////////////////////////////////////////

        writer.endSequence(); // AxesInfo

        // -----------
        writer.startElement("CellInfo");
        cellProperty(writer, StandardCellProperty.VALUE, true, "Value");
        cellProperty(writer, StandardCellProperty.FORMATTED_VALUE, true, "FmtValue");
        cellProperty(writer, StandardCellProperty.FORMAT_STRING, true, "FormatString");
        cellProperty(writer, StandardCellProperty.LANGUAGE, false, "Language");
        cellProperty(writer, StandardCellProperty.BACK_COLOR, false, "BackColor");
        cellProperty(writer, StandardCellProperty.FORE_COLOR, false, "ForeColor");
        cellProperty(writer, StandardCellProperty.FONT_FLAGS, false, "FontFlags");
        writer.endElement(); // CellInfo
        // -----------
        writer.endElement(); // OlapInfo
    }

    protected static List<Hierarchy> getUnseenDimensionHierarchies(List<Dimension> dimensions, List<Hierarchy> allHierarchies) {
        // The slicer axes contains the default hierarchy
        // of each dimension not seen on another axis.
        List<Dimension> unseenDimensionList = new ArrayList<>(dimensions);
        for (Hierarchy hier1 : allHierarchies) {
            unseenDimensionList.remove(hier1.getDimension());
        }
        List<Hierarchy> hierarchies = new ArrayList<>();
        for (Dimension dimension : unseenDimensionList) {
            hierarchies.addAll(dimension.getHierarchies());
        }
        return hierarchies;
    }

    private void cellProperty(SaxWriter writer, StandardCellProperty cellProperty, boolean evenEmpty,
                              String elementName) {
        if (extra.shouldReturnCellProperty(cellSet, cellProperty, evenEmpty)) {
            writer.element(elementName, "name", cellProperty.getName());
        }
    }

    private List<Hierarchy> axisInfo(SaxWriter writer, CellSetAxis axis, String axisName) {
        writer.startElement("AxisInfo", "name", axisName);

        List<Hierarchy> hierarchies = getAxisHierarchies(axis);
        List<Property> props = getProps(axis.getAxisMetaData());

        writeHierarchyInfo(writer, hierarchies, props);

        writer.endElement(); // AxisInfo

        return hierarchies;
    }

    protected static List<Hierarchy> getAxisHierarchies(CellSetAxis axis) {
        List<Hierarchy> hierarchies;
        Iterator<org.olap4j.Position> it = axis.getPositions().iterator();
        if (it.hasNext()) {
            final org.olap4j.Position position = it.next();
            hierarchies = new ArrayList<Hierarchy>();
            for (Member member : position.getMembers()) {
                hierarchies.add(member.getHierarchy());
            }
        } else {
            hierarchies = axis.getAxisMetaData().getHierarchies();
        }
        return hierarchies;
    }

    private void writeHierarchyInfo(SaxWriter writer, List<Hierarchy> hierarchies, List<Property> props) {
        writer.startSequence(null, "HierarchyInfo");
        if (hierarchies == null) {
            writer.endSequence(); // "HierarchyInfo"
            return;
        }
        for (Hierarchy hierarchy : hierarchies) {
            if (hierarchy == null) {
                continue;
            }
            String hierarchyName = hierarchy.getUniqueName();
            writer.startElement("HierarchyInfo", "name", hierarchyName);
            for (final Property prop : props) {
                final String encodedProp = encoder.encode(prop.getName());
                final Object[] attributes = getAttributes(prop, hierarchy);
                writer.element(encodedProp, attributes);
            }
            writer.endElement(); // HierarchyInfo
        }
        writer.endSequence(); // "HierarchyInfo"
    }

    protected Object[] getAttributes(Property prop, Hierarchy hierarchy) {
        Property longProp = longProps.get(prop.getName());
        if (longProp == null) {
            longProp = prop;
        }
        List<Object> values = new ArrayList<Object>();
        values.add("name");
        values.add(hierarchy.getUniqueName() + "." + Util.quoteMdxIdentifier(longProp.getName()));
        if (longProp == prop) {
            // Adding type attribute to the optional properties
            values.add("type");
            values.add(getXsdType(longProp));
        }
        return values.toArray();
    }

    private String getXsdType(Property property) {
        Datatype datatype = property.getDatatype();
        switch (datatype) {
            case UNSIGNED_INTEGER:
                return RowsetDefinition.Type.UnsignedInteger.columnType;
            case BOOLEAN:
                return RowsetDefinition.Type.Boolean.columnType;
            default:
                return RowsetDefinition.Type.String.columnType;
        }
    }

    // The following two methods are extracted from axes().
    // These codes seem a little strange, I can hardly understand them.
    // The memberMap contains the mappings from hierarchy name to slicerMembers index,
    // I don't know why not just use Hierarchy.equals() instead of it.
    protected static Map<String, Integer> createMemberMap(List<Position> slicerPositions) {
        Map<String, Integer> memberMap = new HashMap<String, Integer>();
        if (slicerPositions != null && slicerPositions.size() > 0) {
            final Position pos0 = slicerPositions.get(0);
            int i = 0;
            for (Member member : pos0.getMembers()) {
                memberMap.put(member.getHierarchy().getName(), i++);
            }
        }
        return memberMap;
    }

    protected static Pair<Member, Integer> getSlicerMember(
            Hierarchy hierarchy, Map<String, Integer> memberMap, List<Member> slicerMembers) throws OlapException {
        // Find which member is on the slicer.
        // If it's not explicitly there, use the default member.
        Member member = hierarchy.getDefaultMember();
        final Integer indexPosition = memberMap.get(hierarchy.getName());
        for (Member slicerMember : slicerMembers) {
            if (slicerMember.getHierarchy().equals(hierarchy)) {
                member = slicerMember;
                break;
            }
        }
        return new Pair<>(member, indexPosition);
    }

    private void axes(SaxWriter writer) throws OlapException {
        writer.startSequence("Axes", "Axis");
        //axis(writer, result.getSlicerAxis(), "SlicerAxis");
        final List<CellSetAxis> axes = cellSet.getAxes();
        for (int i = 0; i < axes.size(); i++) {
            final CellSetAxis axis = axes.get(i);
            final List<Property> props = getProps(axis.getAxisMetaData());
            axis(writer, axis, props, "Axis" + i);
        }

        ////////////////////////////////////////////
        // now generate SlicerAxis information
        //
        if (omitDefaultSlicerInfo) {
            CellSetAxis slicerAxis = cellSet.getFilterAxis();
            // We always write a slicer axis. There are two 'empty' cases:
            // zero positions (which happens when the WHERE clause evalutes
            // to an empty set) or one position containing a tuple of zero
            // members (which happens when there is no WHERE clause) and we
            // need to be able to distinguish between the two.
            axis(writer, slicerAxis, getProps(slicerAxis.getAxisMetaData()), "SlicerAxis");
        } else {
            List<Hierarchy> hierarchies = slicerAxisHierarchies;
            writer.startElement("Axis", "name", "SlicerAxis");
            writer.startSequence("Tuples", "Tuple");
            writer.startSequence("Tuple", "Member");

            CellSetAxis slicerAxis = cellSet.getFilterAxis();
            final List<Position> slicerPositions = slicerAxis.getPositions();
            Map<String, Integer> memberMap = createMemberMap(slicerPositions);

            final List<Member> slicerMembers = slicerPositions.isEmpty() ? Collections.<Member>emptyList()
                    : slicerPositions.get(0).getMembers();
            for (Hierarchy hierarchy : hierarchies) {
                Pair<Member, Integer> memberAndIndexPosition = getSlicerMember(hierarchy, memberMap, slicerMembers);
                Member member = memberAndIndexPosition.getLeft();
                Integer indexPosition = memberAndIndexPosition.getRight();
                Member positionMember = indexPosition == null ? null : slicerMembers.get(indexPosition);

                if (member != null) {
                    if (positionMember != null) {
                        writeMember(writer, positionMember, null, slicerPositions.get(0), indexPosition,
                                getProps(slicerAxis.getAxisMetaData()));
                    } else {
                        slicerAxis(writer, member, getProps(slicerAxis.getAxisMetaData()));
                    }
                } else {
                    LOGGER.warn("Can not create SlicerAxis: " + "null default member for Hierarchy "
                            + hierarchy.getUniqueName());
                }
            }
            writer.endSequence(); // Tuple
            writer.endSequence(); // Tuples
            writer.endElement(); // Axis
        }

        //
        ////////////////////////////////////////////

        writer.endSequence(); // Axes
    }

    protected List<Property> getProps(CellSetAxisMetaData queryAxis) {
        if (queryAxis == null) {
            return defaultProps;
        }
        return CompositeList.of(defaultProps, queryAxis.getProperties());
    }

    private void axis(SaxWriter writer, CellSetAxis axis, List<Property> props, String axisName)
            throws OlapException {
        writer.startElement("Axis", "name", axisName);
        writer.startSequence("Tuples", "Tuple");

        List<Position> positions = axis.getPositions();
        Iterator<Position> pit = positions.iterator();
        Position prevPosition = null;
        Position position = pit.hasNext() ? pit.next() : null;
        Position nextPosition = pit.hasNext() ? pit.next() : null;
        while (position != null) {
            writer.startSequence("Tuple", "Member");
            int k = 0;
            List<Member> members = position.getMembers();
            for (Member member : members) {
                writeMember(writer, member, prevPosition, nextPosition, k++, props);

            }
            writer.endSequence(); // Tuple
            prevPosition = position;
            position = nextPosition;
            nextPosition = pit.hasNext() ? pit.next() : null;
        }
        writer.endSequence(); // Tuples
        writer.endElement(); // Axis
    }

    protected Object getMemberPropertyValue(
            Member member, Property property, Position prevPosition, Position nextPosition, int k)
            throws OlapException {
        Property longProp = longProps.get(property.getName());
        if (longProp == null) {
            longProp = property;
        }
        if (longProp == StandardMemberProperty.DISPLAY_INFO) {
            return calculateDisplayInfo(prevPosition, nextPosition, member, k);
        } else if (longProp == StandardMemberProperty.DEPTH) {
            return member.getDepth();
        } else {
            return member.getPropertyValue(longProp);
        }
    }

    private void writeMember(SaxWriter writer, Member member, Position prevPosition, Position nextPosition, int k,
                             List<Property> props) throws OlapException {
        String hierarchyName = member.getHierarchy().getUniqueName();
        writer.startElement("Member", "Hierarchy", hierarchyName);
        for (Property prop : props) {
            Object value = getMemberPropertyValue(member, prop, prevPosition, nextPosition, k);
            if (value != null) {
                writer.textElement(encoder.encode(prop.getName()), value);
            }
        }
        writer.endElement(); // Member
    }

    protected Object getSlicerAxisMemberPropertyValue(Member member, Property property) throws OlapException {
        Object value;
        Property longProp = longProps.get(property.getName());
        if (longProp == null) {
            longProp = property;
        }
        if (longProp == StandardMemberProperty.DISPLAY_INFO) {
            value = calculateDisplayInfoChildrenCount(member);
        } else if (longProp == StandardMemberProperty.DEPTH) {
            value = member.getDepth();
        } else {
            value = member.getPropertyValue(longProp);
        }

        return value;
    }

    private void slicerAxis(SaxWriter writer, Member member, List<Property> props) throws OlapException {
        writer.startElement("Member", "Hierarchy", member.getHierarchy().getName());
        for (Property prop : props) {
            Object value = getSlicerAxisMemberPropertyValue(member, prop);
            if (value != null) {
                writer.textElement(encoder.encode(prop.getName()), value);
            }
        }
        writer.endElement(); // Member
    }

    private int calculateDisplayInfoChildrenCount(Member currentMember) throws OlapException {
        int displayInfo = currentMember.getChildMemberCount();
        if (displayInfo < 0) {
            return MDDataSet_Multidimensional.DEFAULT_CARDINALITY;
        } else {
            return Math.min(displayInfo, 0xFFFF);
        }
    }

    private int calculateDisplayInfo(Position prevPosition, Position nextPosition, Member currentMember,
                                     int memberOrdinal) throws OlapException {
        int displayInfo = calculateDisplayInfoChildrenCount(currentMember);

        if (nextPosition != null) {
            String currentUName = currentMember.getUniqueName();
            Member nextMember = nextPosition.getMembers().get(memberOrdinal);
            String nextParentUName = parentUniqueName(nextMember);
            if (currentUName.equals(nextParentUName)) {
                displayInfo |= 0x10000;
            }
        }
        if (prevPosition != null) {
            String currentParentUName = parentUniqueName(currentMember);
            Member prevMember = prevPosition.getMembers().get(memberOrdinal);
            String prevParentUName = parentUniqueName(prevMember);
            if (currentParentUName != null && currentParentUName.equals(prevParentUName)) {
                displayInfo |= 0x20000;
            }
        }
        return displayInfo;
    }

    private String parentUniqueName(Member member) {
        final Member parent = member.getParentMember();
        if (parent == null) {
            return null;
        }
        return parent.getUniqueName();
    }

    private void cellData(SaxWriter writer) {
        writer.startSequence("CellData", "Cell");
        final int axisCount = cellSet.getAxes().size();
        List<Integer> pos = new ArrayList<Integer>();
        for (int i = 0; i < axisCount; i++) {
            pos.add(-1);
        }
        int[] cellOrdinal = new int[]{0};

        int axisOrdinal = axisCount - 1;
        recurse(writer, pos, axisOrdinal, cellOrdinal);

        writer.endSequence(); // CellData
    }

    private void recurse(SaxWriter writer, List<Integer> pos, int axisOrdinal, int[] cellOrdinal) {
        if (axisOrdinal < 0) {
            emitCell(writer, pos, cellOrdinal[0]++);
        } else {
            CellSetAxis axis = cellSet.getAxes().get(axisOrdinal);
            List<Position> positions = axis.getPositions();
            for (int i = 0, n = positions.size(); i < n; i++) {
                pos.set(axisOrdinal, i);
                recurse(writer, pos, axisOrdinal - 1, cellOrdinal);
            }
        }
    }

    private void emitCell(SaxWriter writer, List<Integer> pos, int ordinal) {
        Cell cell = cellSet.getCell(pos);
        if (cell.isNull() && ordinal != 0) {
            // Ignore null cell like MS AS, except for Oth ordinal
            return;
        }

        writer.startElement("Cell", "CellOrdinal", ordinal);
        for (int i = 0; i < cellProps.size(); i++) {
            Property cellPropLong = cellPropLongs.get(i);
            Object value = cell.getPropertyValue(cellPropLong);
            if (value == null) {
                continue;
            }
            if (!extra.shouldReturnCellProperty(cellSet, cellPropLong, true)) {
                continue;
            }

            if (!json && cellPropLong == StandardCellProperty.VALUE) {
                if (cell.isNull()) {
                    // Return cell without value as in case of AS2005
                    continue;
                }
//                    final String dataType = (String) cell.getPropertyValue(StandardCellProperty.DATATYPE);
                final String dataType = determineDatatype(cell);
                final ValueInfo vi = new ValueInfo(dataType, value);
                final String valueType = vi.valueType;
                final String valueString;
                if (vi.isDecimal) {
                    valueString = XmlaUtil.normalizeNumericString(vi.value.toString());
                } else {
                    valueString = vi.value.toString();
                }

                writer.startElement(cellProps.get(i).getName(), "xsi:type", valueType);
                writer.characters(valueString);
                writer.endElement();
            } else {
                XmlaRequestContext context = XmlaRequestContext.getContext();
                if (!context.errorFilled && Objects.nonNull(context.errorMsg) && !context.errorMsg.isEmpty()) {
                    writer.startElement(cellProps.get(0).getName(), "xsi:type", "string");
                    writer.characters(context.errorMsg);
                    writer.endElement();
                }
                writer.textElement(cellProps.get(i).getName(), value);
            }
        }
        writer.endElement(); // Cell
    }

    private String determineDatatype(Cell cell) {
        Object value = cell.getValue();
        if (value instanceof Integer || value instanceof Long) {
            return "Integer";
        } else if (value instanceof Float || value instanceof Double) {
            return "Numeric";
        } else if (value instanceof BigDecimal) {
            BigDecimal bigDecimalValue = (BigDecimal) value;
            return bigDecimalValue.scale() > 0 ? "Numeric" : "Integer";
        } else {
            return "String";
        }
    }
}
