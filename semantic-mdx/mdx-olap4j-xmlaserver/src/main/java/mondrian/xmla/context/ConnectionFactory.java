package mondrian.xmla.context;

import mondrian.xmla.XmlaRequest;
import org.olap4j.OlapConnection;
import org.olap4j.PreparedOlapStatement;

import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * Creates an olap4j connection for responding to XMLA requests.
 *
 * <p>A typical implementation will probably just use a
 * {@link javax.sql.DataSource} or a connect string, but it is important
 * that the connection is assigned to the correct catalog, schema and role
 * consistent with the client's XMLA context.
 */
public interface ConnectionFactory {
    /**
     * Creates a connection.
     *
     * <p>The implementation passes the properties to the underlying driver.
     *
     * @param catalog  The name of the catalog to use.
     * @param schema   The name of the schema to use.
     * @param roleName The name of the role to use, or NULL.
     * @param props    Properties to be passed to the underlying native driver.
     * @return An OlapConnection object.
     * @throws SQLException on error
     */
    OlapConnection getConnection(String catalog, String schema, String roleName, Properties props)
            throws SQLException;

    /**
     * Returns a map of property name-value pairs with which to populate
     * the response to the DISCOVER_DATASOURCES request.
     *
     * <p>Properties correspond to the columns of that request:
     * ""DataSourceName", et cetera.</p>
     *
     * <p>Returns null if there is no pre-configured response; in
     * which case, the driver will have to connect to get a response.</p>
     *
     * @return Column names and values for the DISCOVER_DATASOURCES
     * response
     */
    Map<String, Object> getPreConfiguredDiscoverDatasourcesResponse();

    /**
     * Called at the start of processing of a request.
     *
     * @param request    XMLA Request
     * @param connection Connection
     */
    Request startRequest(XmlaRequest request, OlapConnection connection);

    /**
     * Called at the end of processing of a request.
     *
     * @param request Request object returned by
     *                {@link #startRequest(XmlaRequest, org.olap4j.OlapConnection)}
     */
    void endRequest(Request request);

    /**
     * Creates a callback for extra functionality.
     */
    XmlaExtra getExtra();

    void putStatementWithSession(String sessionId, PreparedOlapStatement statement);

    void removeStatementWithSession(String sessionId);

    void cancelStatementBySession(String sessionId);

}
