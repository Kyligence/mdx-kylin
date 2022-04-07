package mondrian.xmla.handler.chunk;

import io.kylin.mdx.insight.common.util.io.CountOutputStream;
import mondrian.xmla.*;
import mondrian.xmla.context.QueryResult;
import org.olap4j.metadata.XmlaConstants;

import java.io.OutputStream;
import java.sql.SQLException;

import static mondrian.xmla.XmlaConstants.*;
import static mondrian.xmla.constants.XmlaSchemaConstants.*;

public class CommonResultDataChunk implements XmlaDataChunk {

    // 输入参数

    public boolean compact;

    public String encoding;

    public Enumeration.ResponseMimeType responseMimeType;

    // 输出参数

    public String rootXmlns;

    public boolean rowset;

    public XmlaConstants.Content content;

    public QueryResult queryResult;

    // 内部参数

    private long count;

    @Override
    public long count() {
        return count;
    }

    @Override
    public void write(OutputStream os) {
        CountOutputStream outputStream = new CountOutputStream(os);
        SaxWriter writer = XmlaResult.newSaxWriter(outputStream, encoding, responseMimeType, compact);
        try {
            // 写 ExecuteResponse -> return -> root
            writeRootExecute(writer);
            // 写 xsd:schema
            writeSchemaExecute(writer);
            // 写 OlapInfo + Axes + CellData
            unparseResultExecute(writer);
            // 封闭 XML, 不应该放在 finally 中执行
            writeRootExecuteFinal(writer);
        } finally {
            if (queryResult != null) {
                try {
                    queryResult.close();
                } catch (SQLException ignored) {
                }
            }
            count += outputStream.getCount();
        }
    }

    protected void writeRootExecute(SaxWriter writer) {
        writer.startDocument();
        writer.startElement("ExecuteResponse", "xmlns", NS_XMLA);
        writer.startElement("return");
        writer.startElement("root", "xmlns", rootXmlns, "xmlns:xsi", NS_XSI,
                "xmlns:xsd", NS_XSD, "xmlns:msxmla", NS_XMLA_MS, "xmlns:EX", NS_XMLA_EX);
    }

    protected void writeSchemaExecute(SaxWriter writer) {
        switch (content) {
            case Data:
            case Schema:
            case SchemaData:
                if (queryResult != null) {
                    if (this instanceof PowerBIResultDataChunk) {
                        queryResult.metadata_PowerBI(writer);
                    } else {
                        queryResult.metadata(writer);
                    }
                } else {
                    if (rowset) {
                        if (writer.isCompact()) {
                            writer.verbatim(COMPACT_EMPTY_ROW_SET_XML_SCHEMA);
                        } else {
                            writer.verbatim(PRETTY_EMPTY_ROW_SET_XML_SCHEMA);
                        }
                    } else {
                        if (writer.isCompact()) {
                            writer.verbatim(COMPACT_EMPTY_MD_DATA_SET_XML_SCHEMA);
                        } else {
                            writer.verbatim(PRETTY_EMPTY_MD_DATA_SET_XML_SCHEMA);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    protected void unparseResultExecute(SaxWriter writer) {
        try {
            switch (content) {
                case Data:
                case SchemaData:
                case DataOmitDefaultSlicer:
                case DataIncludeDefaultSlicer:
                    if (queryResult != null) {
                        long start = System.currentTimeMillis();
                        queryResult.unparse(writer);
                        long end = System.currentTimeMillis();
                        XmlaRequestContext context = XmlaRequestContext.getContext();
                        if (context != null) {
                            context.runningStatistics.unparseMultiDimDatasetTime = end - start;
                        }
                    }
                    break;
                default:
                    break;
            }
        } catch (XmlaException xex) {
            throw xex;
        } catch (Throwable t) {
            throw new XmlaException(SERVER_FAULT_FC, HSB_EXECUTE_UNPARSE_CODE, HSB_EXECUTE_UNPARSE_FAULT_FS, t);
        }
    }

    protected void writeRootExecuteFinal(SaxWriter writer) {
        // keep the tags balanced, even if there's an error
        try {
            writer.endElement();
            writer.endElement();
            writer.endElement();
        } catch (Throwable e) {
            // Ignore any errors balancing the tags. The original exception
            // is more important.
        }
        writer.endDocument();
    }

}
