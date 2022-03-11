package mondrian.xmla.utils;

import mondrian.xmla.RowsetDefinition;
import mondrian.xmla.SaxWriter;
import mondrian.xmla.constants.SetType;
import mondrian.xmla.impl.DefaultSaxWriter;

import java.io.StringWriter;

import static mondrian.xmla.XmlaConstants.*;
import static mondrian.xmla.constants.XmlaHandlerConstants.*;

public class XmlaSchemaUtils {

    public static String computeXsd(SetType setType, boolean empty, boolean compact) {
        final StringWriter sw = new StringWriter();
        SaxWriter writer;
        if (compact) {
            writer = new DefaultSaxWriter(sw, 0, true);
        } else {
            writer = new DefaultSaxWriter(sw, 3);
        }
        if (empty) {
            writeEmptyDatasetXmlSchema(writer, setType);
        } else {
            writeDatasetXmlSchema(writer, setType);
        }
        writer.flush();
        return sw.toString();
    }

    /**
     * Computes the XML Schema for a dataset.
     *
     * @param writer  SAX writer
     * @param settype rowset or dataset?
     * @see RowsetDefinition#writeRowsetXmlSchema(SaxWriter)
     */
    private static void writeDatasetXmlSchema(SaxWriter writer, SetType settype) {
        String setNsXmla = (settype == SetType.ROW_SET) ? NS_XMLA_ROWSET : NS_XMLA_MDDATASET;

        writer.startElement("xsd:schema", "xmlns:xsd", NS_XSD, "targetNamespace", setNsXmla, "xmlns", setNsXmla,
                "xmlns:xsi", NS_XSI, "xmlns:sql", NS_XML_SQL, "elementFormDefault", "qualified");

        // MemberType

        writer.startElement("xsd:complexType", "name", "MemberType");
        writer.startElement("xsd:sequence");
        writer.element("xsd:element", "name", "UName", "type", XSD_STRING);
        writer.element("xsd:element", "name", "Caption", "type", XSD_STRING);
        writer.element("xsd:element", "name", "LName", "type", XSD_STRING);
        writer.element("xsd:element", "name", "LNum", "type", XSD_UNSIGNED_INT);
        writer.element("xsd:element", "name", "DisplayInfo", "type", XSD_UNSIGNED_INT);
        writer.startElement("xsd:sequence", "maxOccurs", "unbounded", "minOccurs", 0);
        writer.element("xsd:any", "processContents", "lax", "maxOccurs", "unbounded");
        writer.endElement(); // xsd:sequence
        writer.endElement(); // xsd:sequence
        writer.element("xsd:attribute", "name", "Hierarchy", "type", XSD_STRING);
        writer.endElement(); // xsd:complexType name="MemberType"

        // PropType

        writer.startElement("xsd:complexType", "name", "PropType");
        writer.element("xsd:attribute", "name", "name", "type", XSD_STRING);
        writer.endElement(); // xsd:complexType name="PropType"

        // TupleType

        writer.startElement("xsd:complexType", "name", "TupleType");
        writer.startElement("xsd:sequence", "maxOccurs", "unbounded");
        writer.element("xsd:element", "name", "Member", "type", "MemberType");
        writer.endElement(); // xsd:sequence
        writer.endElement(); // xsd:complexType name="TupleType"

        // MembersType

        writer.startElement("xsd:complexType", "name", "MembersType");
        writer.startElement("xsd:sequence", "maxOccurs", "unbounded");
        writer.element("xsd:element", "name", "Member", "type", "MemberType");
        writer.endElement(); // xsd:sequence
        writer.element("xsd:attribute", "name", "Hierarchy", "type", XSD_STRING);
        writer.endElement(); // xsd:complexType

        // TuplesType

        writer.startElement("xsd:complexType", "name", "TuplesType");
        writer.startElement("xsd:sequence", "maxOccurs", "unbounded");
        writer.element("xsd:element", "name", "Tuple", "type", "TupleType");
        writer.endElement(); // xsd:sequence
        writer.endElement(); // xsd:complexType

        // CrossProductType

        writer.startElement("xsd:complexType", "name", "CrossProductType");
        writer.startElement("xsd:sequence");
        writer.startElement("xsd:choice", "minOccurs", 0, "maxOccurs", "unbounded");
        writer.element("xsd:element", "name", "Members", "type", "MembersType");
        writer.element("xsd:element", "name", "Tuples", "type", "TuplesType");
        writer.endElement(); // xsd:choice
        writer.endElement(); // xsd:sequence
        writer.element("xsd:attribute", "name", "Size", "type", XSD_UNSIGNED_INT);
        writer.endElement(); // xsd:complexType

        // OlapInfo

        writer.startElement("xsd:complexType", "name", "OlapInfo");
        writer.startElement("xsd:sequence");

        { // <CubeInfo>
            writer.startElement("xsd:element", "name", "CubeInfo");
            writer.startElement("xsd:complexType");
            writer.startElement("xsd:sequence");

            { // <Cube>
                writer.startElement("xsd:element", "name", "Cube", "maxOccurs", "unbounded");
                writer.startElement("xsd:complexType");
                writer.startElement("xsd:sequence");

                writer.element("xsd:element", "name", "CubeName", "type", XSD_STRING);
                writer.element("xsd:element", "name", "LastDataUpdate", "type", XSD_dateTime);
                writer.element("xsd:element", "name", "LastSchemaUpdate", "type", XSD_dateTime);

                writer.endElement(); // xsd:sequence
                writer.endElement(); // xsd:complexType
                writer.endElement(); // xsd:element name=Cube
            }

            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
            writer.endElement(); // xsd:element name=CubeInfo
        }

        { // <AxesInfo>
            writer.startElement("xsd:element", "name", "AxesInfo");
            writer.startElement("xsd:complexType");
            writer.startElement("xsd:sequence");
            { // <AxisInfo>
                writer.startElement("xsd:element", "name", "AxisInfo", "maxOccurs", "unbounded");
                writer.startElement("xsd:complexType");
                writer.startElement("xsd:sequence");

                { // <HierarchyInfo>
                    writer.startElement("xsd:element", "name", "HierarchyInfo", "minOccurs", 0, "maxOccurs",
                            "unbounded");
                    writer.startElement("xsd:complexType");
                    writer.startElement("xsd:sequence");
                    writer.startElement("xsd:sequence", "maxOccurs", "unbounded");
                    writer.element("xsd:element", "name", "UName", "type", "PropType");
                    writer.element("xsd:element", "name", "Caption", "type", "PropType");
                    writer.element("xsd:element", "name", "LName", "type", "PropType");
                    writer.element("xsd:element", "name", "LNum", "type", "PropType");
                    writer.element("xsd:element", "name", "DisplayInfo", "type", "PropType", "minOccurs", 0,
                            "maxOccurs", "unbounded");
//                    if (false) {
//                        writer.element("xsd:element", "name", "PARENT_MEMBER_NAME", "type", "PropType", "minOccurs", 0,
//                                "maxOccurs", "unbounded");
//                    }
                    writer.endElement(); // xsd:sequence

                    // This is the Depth element for JPivot??
                    writer.startElement("xsd:sequence");
                    writer.element("xsd:any", "processContents", "lax", "minOccurs", 0, "maxOccurs", "unbounded");
                    writer.endElement(); // xsd:sequence

                    writer.endElement(); // xsd:sequence
                    writer.element("xsd:attribute", "name", "name", "type", XSD_STRING, "use", "required");
                    writer.endElement(); // xsd:complexType
                    writer.endElement(); // xsd:element name=HierarchyInfo
                }
                writer.endElement(); // xsd:sequence
                writer.element("xsd:attribute", "name", "name", "type", XSD_STRING);
                writer.endElement(); // xsd:complexType
                writer.endElement(); // xsd:element name=AxisInfo
            }
            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
            writer.endElement(); // xsd:element name=AxesInfo
        }

        // CellInfo

        { // <CellInfo>
            writer.startElement("xsd:element", "name", "CellInfo");
            writer.startElement("xsd:complexType");
            writer.startElement("xsd:sequence");
            writer.startElement("xsd:sequence", "minOccurs", 0, "maxOccurs", "unbounded");
            writer.startElement("xsd:choice");
            writer.element("xsd:element", "name", "Value", "type", "PropType");
            writer.element("xsd:element", "name", "FmtValue", "type", "PropType");
            writer.element("xsd:element", "name", "BackColor", "type", "PropType");
            writer.element("xsd:element", "name", "ForeColor", "type", "PropType");
            writer.element("xsd:element", "name", "FontName", "type", "PropType");
            writer.element("xsd:element", "name", "FontSize", "type", "PropType");
            writer.element("xsd:element", "name", "FontFlags", "type", "PropType");
            writer.element("xsd:element", "name", "FormatString", "type", "PropType");
            writer.element("xsd:element", "name", "NonEmptyBehavior", "type", "PropType");
            writer.element("xsd:element", "name", "SolveOrder", "type", "PropType");
            writer.element("xsd:element", "name", "Updateable", "type", "PropType");
            writer.element("xsd:element", "name", "Visible", "type", "PropType");
            writer.element("xsd:element", "name", "Expression", "type", "PropType");
            writer.endElement(); // xsd:choice
            writer.endElement(); // xsd:sequence
            writer.startElement("xsd:sequence", "maxOccurs", "unbounded", "minOccurs", 0);
            writer.element("xsd:any", "processContents", "lax", "maxOccurs", "unbounded");
            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
            writer.endElement(); // xsd:element name=CellInfo
        }

        writer.endElement(); // xsd:sequence
        writer.endElement(); // xsd:complexType

        // Axes

        writer.startElement("xsd:complexType", "name", "Axes");
        writer.startElement("xsd:sequence", "maxOccurs", "unbounded");
        { // <Axis>
            writer.startElement("xsd:element", "name", "Axis");
            writer.startElement("xsd:complexType");
            writer.startElement("xsd:choice", "minOccurs", 0, "maxOccurs", "unbounded");
            writer.element("xsd:element", "name", "CrossProduct", "type", "CrossProductType");
            writer.element("xsd:element", "name", "Tuples", "type", "TuplesType");
            writer.element("xsd:element", "name", "Members", "type", "MembersType");
            writer.endElement(); // xsd:choice
            writer.element("xsd:attribute", "name", "name", "type", XSD_STRING);
            writer.endElement(); // xsd:complexType
        }
        writer.endElement(); // xsd:element
        writer.endElement(); // xsd:sequence
        writer.endElement(); // xsd:complexType

        // CellData

        writer.startElement("xsd:complexType", "name", "CellData");
        writer.startElement("xsd:sequence");
        { // <Cell>
            writer.startElement("xsd:element", "name", "Cell", "minOccurs", 0, "maxOccurs", "unbounded");
            writer.startElement("xsd:complexType");
            writer.startElement("xsd:sequence", "maxOccurs", "unbounded");
            writer.startElement("xsd:choice");
            writer.element("xsd:element", "name", "Value");
            writer.element("xsd:element", "name", "FmtValue", "type", XSD_STRING);
            writer.element("xsd:element", "name", "BackColor", "type", XSD_UNSIGNED_INT);
            writer.element("xsd:element", "name", "ForeColor", "type", XSD_UNSIGNED_INT);
            writer.element("xsd:element", "name", "FontName", "type", XSD_STRING);
            writer.element("xsd:element", "name", "FontSize", "type", "xsd:unsignedShort");
            writer.element("xsd:element", "name", "FontFlags", "type", XSD_UNSIGNED_INT);
            writer.element("xsd:element", "name", "FormatString", "type", XSD_STRING);
            writer.element("xsd:element", "name", "NonEmptyBehavior", "type", "xsd:unsignedShort");
            writer.element("xsd:element", "name", "SolveOrder", "type", XSD_UNSIGNED_INT);
            writer.element("xsd:element", "name", "Updateable", "type", XSD_UNSIGNED_INT);
            writer.element("xsd:element", "name", "Visible", "type", XSD_UNSIGNED_INT);
            writer.element("xsd:element", "name", "Expression", "type", XSD_STRING);
            writer.endElement(); // xsd:choice
            writer.endElement(); // xsd:sequence
            writer.element("xsd:attribute", "name", "CellOrdinal", "type", XSD_UNSIGNED_INT, "use", "required");
            writer.endElement(); // xsd:complexType
            writer.endElement(); // xsd:element name=Cell
        }
        writer.endElement(); // xsd:sequence
        writer.endElement(); // xsd:complexType

        { // <root>
            writer.startElement("xsd:element", "name", "root");
            writer.startElement("xsd:complexType");
            writer.startElement("xsd:sequence", "maxOccurs", "unbounded");
            writer.element("xsd:element", "name", "OlapInfo", "type", "OlapInfo");
            writer.element("xsd:element", "name", "Axes", "type", "Axes");
            writer.element("xsd:element", "name", "CellData", "type", "CellData");
            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
            writer.endElement(); // xsd:element name=root
        }

        writer.endElement(); // xsd:schema
    }

    private static void writeEmptyDatasetXmlSchema(SaxWriter writer, SetType setType) {
        /* Excel
        String setNsXmla = NS_XMLA_ROWSET;
        writer.startElement(
            "xsd:schema",
            "xmlns:xsd", NS_XSD,
            "targetNamespace", setNsXmla,
            "xmlns", setNsXmla,
            "xmlns:xsi", NS_XSI,
            "xmlns:sql", NS_XML_SQL,
            "elementFormDefault", "qualified");

        writer.element(
            "xsd:element",
            "name", "root");

        writer.endElement(); // xsd:schema
        */
    }

}
