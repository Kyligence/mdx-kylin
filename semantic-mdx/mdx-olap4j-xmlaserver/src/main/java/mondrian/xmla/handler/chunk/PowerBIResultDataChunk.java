package mondrian.xmla.handler.chunk;

import mondrian.xmla.SaxWriter;
import org.olap4j.metadata.XmlaConstants;

import static mondrian.xmla.XmlaConstants.*;

public class PowerBIResultDataChunk extends CommonResultDataChunk {

    @Override
    protected void writeRootExecute(SaxWriter writer) {
        writer.startDocument();
        String prefix = "m";
        writer.startElement(prefix + ":ExecuteResponse", "xmlns:" + prefix, NS_XMLA);
        writer.startElement(prefix + ":return");
        writer.startElement("root", "xmlns", rootXmlns, "xmlns:xsi", NS_XSI,
                "xmlns:xsd", NS_XSD, "xmlns:EX", NS_XMLA_EX);
    }

    @Override
    protected void writeSchemaExecute(SaxWriter writer) {
        if (content != XmlaConstants.Content.Data) {
            super.writeSchemaExecute(writer);
        }
    }

}
