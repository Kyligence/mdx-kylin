package mondrian.xmla.handler;

import mondrian.xmla.Enumeration;
import mondrian.xmla.*;
import mondrian.xmla.context.ConnectionFactory;
import mondrian.xmla.context.QueryResult;
import mondrian.xmla.handler.chunk.ByteArrayDataChunk;
import mondrian.xmla.handler.chunk.CommonResultDataChunk;
import mondrian.xmla.impl.DefaultXmlaRequest;
import org.olap4j.metadata.Property;
import org.olap4j.metadata.XmlaConstants;
import org.olap4j.xmla.server.impl.Util;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static mondrian.xmla.XmlaConstants.*;
import static mondrian.xmla.utils.XmlaHandlerUtils.*;

public class CommonXmlaHandler extends XmlaHandler {

    /**
     * Creates an <code>XmlaHandler</code>.
     *
     * <p>The connection factory may be null, as long as you override
     * {@link #getConnection(String, String, String, Properties)}.
     *
     * @param connectionFactory Connection factory
     * @param prefix            XML Namespace. Typical value is "xmla", but a value of
     */
    public CommonXmlaHandler(ConnectionFactory connectionFactory, String prefix) {
        super(connectionFactory, prefix);
    }

    @Override
    protected void discover(XmlaRequest request, XmlaResponse response) throws XmlaException {
        if ("MDSCHEMA_SETS".equals(request.getRequestType())) {
            if (request.getRestrictions().containsKey(Property.StandardMemberProperty.CATALOG_NAME.name())) {
                XmlaRequestContext.getContext().tableauFlag = true;
            }
        }

        Rowset rowset = getRequestedRowset(request, request.getRequestType());

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

        writeRootDiscover(writer);
        if (rowset instanceof RowsetDefinition.DiscoverXmlMetadataRowset) {
            RowsetDefinition.Column[] columns = ((RowsetDefinition.DiscoverXmlMetadataRowset) rowset).outputColumns;
            writeSchemaColumnsDiscover(writer, rowset, columns, content);
        } else {
            writeSchemaDiscover(writer, rowset, content);
        }
        unparseRowsetDiscover(writer, rowset, content, response);

        response.getResult().body = new ByteArrayDataChunk(osBuf.toByteArray());
    }

    @Override
    protected void execute(XmlaRequest request, XmlaResponse response) throws XmlaException {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        if (context.clientType.equals(XmlaRequestContext.ClientType.POWERBI_DESKTOP)) {
            DMVParser dmvParser = new DMVParser(request.getStatement());
            if (dmvParser.find()) {
                executeDMV(request, response, dmvParser);
                return;
            }
        }

        XmlaConstants.Content content = getContentExecute(request);

        if (XmlaRequestContext.ClientType.MICRO_STRATEGY.equals(context.clientType)) {
            completeCatalogName(request);
        }
        // Handle execute
        if (request.isCancel()) {
            cancelExecuteQuery(request);
        }

        QueryResult result;
        if (request.isDrillThrough()) {
            result = executeDrillThroughQuery(request);
        } else {
            result = executeQuery(request);
        }

        boolean rowset = responseRowset(request);
        String rootXmlns = result == null ? NS_XMLA_EMPTY : rowset ? NS_XMLA_ROWSET : NS_XMLA_MDDATASET;

        CommonResultDataChunk dataChunk = new CommonResultDataChunk();
        dataChunk.rootXmlns = rootXmlns;
        dataChunk.rowset = rowset;
        dataChunk.content = content;
        dataChunk.queryResult = result;

        response.getResult().body = dataChunk;
    }

    /**
     * Process EXECUTE requests in simple DMV format (select [{@code fieldName}], [{@code fieldName}] ... from
     * $system.{@code requestType} where [{@code fieldName}] = {@code fieldValue})
     * See <a href="https://docs.microsoft.com/en-us/analysis-services/instances/use-dynamic-management-views-dmvs-to-monitor-analysis-services?view=sql-server-2017"></a>
     */
    private void executeDMV(XmlaRequest request, XmlaResponse response, DMVParser dmvParser) throws XmlaException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Parsing DMV: %s", request.getStatement()));
        }

        //Get requestType from DMV statement
        String requestType = dmvParser.getRequestType().toUpperCase();

        //Create restrictions from parameters and DMV statement
        Map<String, String> parameters = request.getParameters();
        Map<String, DMVParser.DMVRestriction<?>> restrictions = dmvParser.getSelectRestrictions(parameters);

        //Create a new DISCOVER request
        XmlaRequest discoverRequest = new DefaultXmlaRequest(XmlaConstants.Method.DISCOVER, request.getProperties(),
                request.getStatement(), request.isDrillThrough(), requestType,
                restrictions == null ? null : Collections.unmodifiableMap(restrictions),
                parameters, request.getRoleName(), request.getUsername(), request.getPassword(), request.getSessionId());

        Rowset rowset = getRequestedRowset(discoverRequest, requestType);

        Set<String> selectColumnStringSet = dmvParser.getSelectColumns();

        //Find all select target columns
        List<RowsetDefinition.Column> columnsList = new LinkedList<>();
        for (RowsetDefinition.Column column : rowset.rowsetDefinition.columnDefinitions) {
            if (selectColumnStringSet.contains(column.name)) {
                columnsList.add(column);
            }
        }
        RowsetDefinition.Column[] columns = columnsList.toArray(new RowsetDefinition.Column[0]);

        XmlaConstants.Content content = getContentExecute(request);

        boolean responseRowset = responseRowset(request);
        String rootAttributeXmlns = responseRowset ? NS_XMLA_ROWSET : NS_XMLA_MDDATASET;

        ByteArrayOutputStream osBuf = new ByteArrayOutputStream();
        final SaxWriter writer = XmlaResult.newSaxWriter(osBuf, "UTF-8",
                Enumeration.ResponseMimeType.SOAP,
                XmlaRequestContext.getContext().compactResult);

        writeRootExecute(writer, rootAttributeXmlns);
        writeSchemaColumnsDiscover(writer, rowset, columns, content);
        unparseRowsetColumnsDiscover(writer, rowset, columns, content, response);

        response.getResult().body = new ByteArrayDataChunk(osBuf.toByteArray());
    }

    private void completeCatalogName(XmlaRequest request) {
        String mdx = request.getStatement();
        XmlaRequestContext.getContext().currentCatalog = Util.extractCatalogFromMdx(mdx);
    }

    private XmlaConstants.Content getContentExecute(XmlaRequest request) {
        final Map<String, String> properties = request.getProperties();

        // Default responseMimeType is SOAP.
        Enumeration.ResponseMimeType responseMimeType = getResponseMimeType(request);

        // Default value is SchemaData, or Data for JSON responses.
        final String contentName = properties.get(PropertyDefinition.Content.name());
        return Util.lookup(XmlaConstants.Content.class, contentName,
                responseMimeType == Enumeration.ResponseMimeType.JSON ? XmlaConstants.Content.Data : XmlaConstants.Content.DEFAULT);
    }

    private Rowset getRequestedRowset(XmlaRequest request, String requestType) {
        final RowsetDefinition rowsetDefinition = RowsetDefinition.valueOf(requestType);
        return rowsetDefinition.getRowset(request, this);
    }

    private void writeRootExecute(SaxWriter writer, String rootAttributeXmlns) {
        writer.startDocument();
        writer.startElement("ExecuteResponse", "xmlns", NS_XMLA);
        writer.startElement("return");
        writer.startElement("root", "xmlns", rootAttributeXmlns, "xmlns:xsi", NS_XSI,
                "xmlns:xsd", NS_XSD, "xmlns:msxmla", NS_XMLA_MS, "xmlns:EX", NS_XMLA_EX);
    }

    private void writeRootDiscover(SaxWriter writer) {
        writer.startDocument();
        writer.startElement("DiscoverResponse", "xmlns", NS_XMLA, "xmlns:ddl2", NS_XMLA_DDL2, "xmlns:ddl2_2", NS_XMLA_DDL2_2,
                "xmlns:ddl100", NS_XMLA_DDL100, "xmlns:ddl100_100", NS_XMLA_DDL100_100, "xmlns:ddl200", NS_XMLA_DDL200, "xmlns:ddl200_200",
                NS_XMLA_DDL200_200, "xmlns:ddl300", NS_XMLA_DDL300, "xmlns:ddl300_300", NS_XMLA_DDL300_300);
        writer.startElement("return");
        writer.startElement("root", "xmlns", NS_XMLA_ROWSET, "xmlns:xsi", NS_XSI, "xmlns:xsd", NS_XSD,
                "xmlns:msxmla", MS_XMLA, "xmlns:EX", NS_XMLA_EX);
    }

    private void unparseRowsetDiscover(SaxWriter writer, Rowset rowset, XmlaConstants.Content content, XmlaResponse response) throws XmlaException {
        unparseRowsetColumnsDiscover(writer, rowset, rowset.rowsetDefinition.columnDefinitions, content, response);
    }

    private void unparseRowsetColumnsDiscover(SaxWriter writer, Rowset rowset, RowsetDefinition.Column[] columns, XmlaConstants.Content content, XmlaResponse response) throws XmlaException {
        try {
            switch (content) {
                case Data:
                case SchemaData:
                    rowset.unparseColumns(response, writer, columns);
                    break;
                default:
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
    }

    private void cancelExecuteQuery(XmlaRequest request) {
        assert request.isCancel();
        String sessionId = request.getSessionId();

        if (sessionId != null && !sessionId.isEmpty()) {
            connectionFactory.cancelStatementBySession(request.getSessionId());
        }
    }

    private boolean responseRowset(XmlaRequest request) {
        return request.isDrillThrough()
                || XmlaConstants.Format.Tabular.name().equals(request.getProperties().get(PropertyDefinition.Format.name()));
    }

    private void writeSchemaDiscover(SaxWriter writer, Rowset rowset, XmlaConstants.Content content) {
        writeSchemaColumnsDiscover(writer, rowset, rowset.rowsetDefinition.columnDefinitions, content);
    }

    private void writeSchemaColumnsDiscover(SaxWriter writer, Rowset rowset, RowsetDefinition.Column[] columns, XmlaConstants.Content content) {
        switch (content) {
            case Schema:
            case SchemaData:
                rowset.rowsetDefinition.writeRowsetXmlSchemaColumns(writer, columns);
                break;
        }
    }

}
