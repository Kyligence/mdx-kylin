package mondrian.xmla.constants;

import static mondrian.xmla.utils.XmlaSchemaUtils.computeXsd;

public class XmlaSchemaConstants {

    public static final String PRETTY_EMPTY_ROW_SET_XML_SCHEMA = computeXsd(SetType.ROW_SET, true, false);

    public static final String COMPACT_EMPTY_ROW_SET_XML_SCHEMA = computeXsd(SetType.ROW_SET, true, true);

    public static final String PRETTY_MD_DATA_SET_XML_SCHEMA = computeXsd(SetType.MD_DATA_SET, false, false);

    public static final String COMPACT_MD_DATA_SET_XML_SCHEMA = computeXsd(SetType.MD_DATA_SET, false, true);

    public static final String PRETTY_EMPTY_MD_DATA_SET_XML_SCHEMA = computeXsd(SetType.MD_DATA_SET, true, false);

    public static final String COMPACT_EMPTY_MD_DATA_SET_XML_SCHEMA = computeXsd(SetType.MD_DATA_SET, true, true);

}
