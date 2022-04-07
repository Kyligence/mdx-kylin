package mondrian.xmla.handler;

import mondrian.xmla.XmlaHandler;
import mondrian.xmla.XmlaRequest;
import mondrian.xmla.XmlaRequestContext;
import mondrian.xmla.context.ConnectionFactory;
import org.olap4j.OlapConnection;

import java.util.Map;

public class XmlaHandlerFactory {

    public static XmlaHandler createHandler(ConnectionFactory connectionFactory, String prefix) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        if (context != null && XmlaRequestContext.ClientType.POWERBI.equals(context.clientType)) {
            return new PowerBIXmlaHandler(connectionFactory, prefix);
        } else {
            return new CommonXmlaHandler(connectionFactory, prefix);
        }
    }

    public static XmlaHandler createHandler(ConnectionFactory connectionFactory, String prefix, OlapConnection connection) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        if (context != null && XmlaRequestContext.ClientType.POWERBI.equals(context.clientType)) {
            return new PowerBIXmlaHandler(
                    connectionFactory,
                    prefix) {
                @Override
                public OlapConnection getConnection(
                        XmlaRequest request,
                        Map<String, String> propMap) {
                    return connection;
                }
            };
        } else {
            return new CommonXmlaHandler(
                    connectionFactory,
                    prefix) {
                @Override
                public OlapConnection getConnection(
                        XmlaRequest request,
                        Map<String, String> propMap) {
                    return connection;
                }
            };
        }
    }

}
