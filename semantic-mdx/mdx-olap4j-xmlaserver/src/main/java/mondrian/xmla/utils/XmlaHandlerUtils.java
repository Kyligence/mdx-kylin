package mondrian.xmla.utils;

import mondrian.xmla.Enumeration;
import mondrian.xmla.PropertyDefinition;
import mondrian.xmla.XmlaRequest;
import mondrian.xmla.XmlaRequestContext;
import org.olap4j.metadata.XmlaConstants;
import org.olap4j.xmla.server.impl.Util;

import java.util.Objects;

public class XmlaHandlerUtils {

    public static XmlaRequestContext getContextByRequest(XmlaRequest request) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        // set request context properties
        if (Objects.nonNull(request.getProperties())) {
            context.addParameters(request.getProperties());
        }
        if (Objects.isNull(context.currentCatalog)) {
            context.currentCatalog = context.getParameter("Catalog");
        }
        return context;
    }

    public static XmlaConstants.Format getFormat(XmlaRequest request, XmlaConstants.Format defaultValue) {
        final String formatName = request.getProperties().get(PropertyDefinition.Format.name());
        return Util.lookup(XmlaConstants.Format.class, formatName, defaultValue);
    }

    public static XmlaConstants.Content getContent(XmlaRequest request) {
        final String contentName = request.getProperties().get(PropertyDefinition.Content.name());
        return Util.lookup(XmlaConstants.Content.class, contentName, XmlaConstants.Content.DEFAULT);
    }

    public static Enumeration.ResponseMimeType getResponseMimeType(XmlaRequest request) {
        Enumeration.ResponseMimeType mimeType = Enumeration.ResponseMimeType.MAP
                .get(request.getProperties().get(PropertyDefinition.ResponseMimeType.name()));
        if (mimeType == null) {
            mimeType = Enumeration.ResponseMimeType.SOAP;
        }
        return mimeType;
    }

}
