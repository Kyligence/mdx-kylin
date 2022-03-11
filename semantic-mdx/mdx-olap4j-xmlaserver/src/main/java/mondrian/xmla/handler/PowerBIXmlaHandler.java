package mondrian.xmla.handler;

import mondrian.xmla.*;
import mondrian.xmla.context.ConnectionFactory;
import mondrian.xmla.context.QueryResult;
import mondrian.xmla.handler.chunk.ByteArrayDataChunk;
import mondrian.xmla.handler.chunk.PowerBIResultDataChunk;
import org.olap4j.metadata.XmlaConstants;
import org.olap4j.xmla.server.impl.Util;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static mondrian.xmla.XmlaConstants.*;
import static mondrian.xmla.utils.XmlaHandlerUtils.*;

/**
 * 强制条件: ClientType.POWERBI.equals(context.clientType)
 */
public class PowerBIXmlaHandler extends XmlaHandler {

    /**
     * Creates an <code>XmlaHandler</code>.
     *
     * <p>The connection factory may be null, as long as you override
     * {@link #getConnection(String, String, String, Properties)}.
     *
     * @param connectionFactory Connection factory
     * @param prefix            XML Namespace. Typical value is "xmla", but a value of
     */
    public PowerBIXmlaHandler(ConnectionFactory connectionFactory, String prefix) {
        super(connectionFactory, prefix);
    }

    @Override
    protected void discover(XmlaRequest request, XmlaResponse response) throws XmlaException {
        String prefix = "m";
        final RowsetDefinition rowsetDefinition = RowsetDefinition.valueOf(request.getRequestType());
        Rowset rowset = rowsetDefinition.getRowset(request, this);

        XmlaConstants.Format format = getFormat(request, XmlaConstants.Format.Tabular);
        if (format != XmlaConstants.Format.Tabular) {
            throw new XmlaException(CLIENT_FAULT_FC, HSB_DISCOVER_FORMAT_CODE, HSB_DISCOVER_FORMAT_FAULT_FS,
                    new UnsupportedOperationException("<Format>: only 'Tabular' allowed in Discover method " + "type"));
        }
        final XmlaConstants.Content content = getContent(request);

        ByteArrayOutputStream osBuf = new ByteArrayOutputStream();
        final SaxWriter writer = XmlaResult.newSaxWriter(osBuf, "UTF-8",
                Enumeration.ResponseMimeType.SOAP,
                XmlaRequestContext.getContext().compactResult);
        writer.startDocument();

        writer.startElement(prefix + ":DiscoverResponse", "xmlns:" + prefix, NS_XMLA);
        writer.startElement(prefix + ":return");
        writer.startElement("root", "xmlns", NS_XMLA_ROWSET, "xmlns:xsi", NS_XSI, "xmlns:xsd", NS_XSD, "xmlns:EX",
                NS_XMLA_EX);

        switch (content) {
            case Schema:
            case SchemaData:
                rowset.rowsetDefinition.writeRowsetXmlSchema(writer);
                break;
        }

        try {
            switch (content) {
                case Data:
                case SchemaData:
                    rowset.unparse(response, writer);
                    break;
            }
        } catch (XmlaException xex) {
            throw xex;
        } catch (Throwable t) {
            throw new XmlaException(SERVER_FAULT_FC, HSB_DISCOVER_UNPARSE_CODE, HSB_DISCOVER_UNPARSE_FAULT_FS, t);
        } finally {
            // keep the tags balanced, even if there's an error
            try {
                writer.endElement();
                writer.endElement();
                writer.endElement();
            } catch (Throwable e) {
                // Ignore any errors balancing the tags. The original exception
                // is more important.
            }
        }
        writer.endDocument();

        response.getResult().body = new ByteArrayDataChunk(osBuf.toByteArray());
    }

    @Override
    protected void execute(XmlaRequest request, XmlaResponse response) throws XmlaException {
        final Map<String, String> properties = request.getProperties();

        // Default responseMimeType is SOAP.
        Enumeration.ResponseMimeType responseMimeType = getResponseMimeType(request);

        // Default value is SchemaData, or Data for JSON responses.
        final String contentName = properties.get(PropertyDefinition.Content.name());
        XmlaConstants.Content content = Util.lookup(XmlaConstants.Content.class, contentName,
                responseMimeType == Enumeration.ResponseMimeType.JSON ? XmlaConstants.Content.Data : XmlaConstants.Content.DEFAULT);

        // Handle execute
        QueryResult result;
        if (request.isDrillThrough()) {
            result = executeDrillThroughQuery(request);
        } else {
            result = executeQuery(request);
        }

        PowerBIResultDataChunk dataChunk = new PowerBIResultDataChunk();
        dataChunk.rowset = request.isDrillThrough()
                || XmlaConstants.Format.Tabular.name().equals(request.getProperties().get(PropertyDefinition.Format.name()));
        dataChunk.rootXmlns = result == null ? NS_XMLA_EMPTY :
                dataChunk.rowset ? NS_XMLA_ROWSET : NS_XMLA_MDDATASET;
        dataChunk.content = content;
        dataChunk.queryResult = result;

        response.getResult().body = dataChunk;
    }

}
