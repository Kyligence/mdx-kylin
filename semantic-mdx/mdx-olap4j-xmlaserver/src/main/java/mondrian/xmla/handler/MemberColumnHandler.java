package mondrian.xmla.handler;

import mondrian.xmla.SaxWriter;
import org.olap4j.Cell;
import org.olap4j.OlapException;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Property;
import org.olap4j.xmla.server.impl.Util;

/**
 * Callback to handle one column, representing the combination of a
 * level and a property (e.g. [Store].[Store State].[MEMBER_UNIQUE_NAME])
 * in a flattened dataset.
 */
public class MemberColumnHandler extends ColumnHandler {
    private final Property property;
    private final Level level;
    private final int memberOrdinal;

    public MemberColumnHandler(Property property, Level level, int memberOrdinal) {
        super(level.getUniqueName() + "." + Util.quoteMdxIdentifier(property.getName()));
        this.property = property;
        this.level = level;
        this.memberOrdinal = memberOrdinal;
    }

    public MemberColumnHandler(Property property, Level level, int memberOrdinal, String powerBI) {
        super(level.getUniqueName() + "." + Util.quoteMdxIdentifier(property.getName()), property);
        this.property = property;
        this.level = level;
        this.memberOrdinal = memberOrdinal;
    }

    @Override
    public void metadata(SaxWriter writer) {
        writer.element("xsd:element", "sql:field", name, "name", encodedName, "type", getXsdType(property), "minOccurs", 0);
    }

    @Override
    public void metadata_PowerBI(SaxWriter writer) {
        writer.element("xsd:element", "minOccurs", 0, "name", encodedName, "sql:field", name, "type", getXsdType(property));
    }

    public String write(Member[] members) throws OlapException {
        Member member = members[memberOrdinal];
        final int depth = level.getDepth();
        if (member.getDepth() < depth) {
            // This column deals with a level below the current member.
            // There is no value to write.
            return null;
        }
        while (member.getDepth() > depth) {
            member = member.getParentMember();
        }
        final Object propertyValue = member.getPropertyValue(property);
        if (propertyValue == null) {
            return null;
        }

        return propertyValue.toString();
    }

    @Override
    public void write(SaxWriter writer, Cell cell, Member[] members) throws OlapException {
        Member member = members[memberOrdinal];
        final int depth = level.getDepth();
        if (member.getDepth() < depth) {
            // This column deals with a level below the current member.
            // There is no value to write.
            return;
        }
        while (member.getDepth() > depth) {
            member = member.getParentMember();
        }
        final Object propertyValue = member.getPropertyValue(property);
        if (propertyValue == null) {
            return;
        }

        writer.startElement(encodedName);
        writer.characters(propertyValue.toString());
        writer.endElement();
    }
}
