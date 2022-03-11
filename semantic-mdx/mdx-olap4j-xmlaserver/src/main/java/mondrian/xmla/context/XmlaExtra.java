package mondrian.xmla.context;

import mondrian.xmla.PropertyDefinition;
import org.olap4j.*;
import org.olap4j.metadata.*;
import org.olap4j.xmla.server.impl.Util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Extra support for XMLA server. If a connection provides this interface,
 * the XMLA server will call methods in this interface instead of relying
 * on the core olap4j interface.
 *
 * <p>The {@link XmlaExtraImpl} class provides
 * a default implementation that uses the olap4j interface exclusively.
 */
public interface XmlaExtra {

    ResultSet executeDrillthrough(OlapStatement olapStatement, String mdx, boolean advanced, String tabFields,
                                  int[] rowCountSlot) throws SQLException;

    void setPreferList(OlapConnection connection);

    Date getSchemaLoadDate(Schema schema);

    int getLevelCardinality(Level level) throws OlapException;

    void getSchemaFunctionList(List<FunctionDefinition> funDefs, Schema schema,
                               Util.Predicate1<String> functionFilter);

    int getHierarchyCardinality(Hierarchy hierarchy) throws OlapException;

    int getHierarchyStructure(Hierarchy hierarchy);

    boolean isHierarchyParentChild(Hierarchy hierarchy);

    int getMeasureAggregator(Member member);

    void checkMemberOrdinal(Member member) throws OlapException;

    /**
     * Returns whether we should return a cell property in the XMLA result.
     *
     * @param cellSet      Cell set
     * @param cellProperty Cell property definition
     * @param evenEmpty    Whether to return even if cell has no properties
     * @return Whether to return cell property in XMLA result
     */
    boolean shouldReturnCellProperty(CellSet cellSet, Property cellProperty, boolean evenEmpty);

    /**
     * Returns a list of names of roles in the given schema to which the
     * current user belongs.
     *
     * @param schema Schema
     * @return List of roles
     */
    List<String> getSchemaRoleNames(Schema schema);

    /**
     * Returns the unique ID of a schema.
     */
    String getSchemaId(Schema schema);

    String getCubeType(Cube cube);

    boolean isLevelUnique(Level level);

    /**
     * Returns the defined properties of a level. (Not including system
     * properties that every level has.)
     *
     * @param level Level
     * @return Defined properties
     */
    List<Property> getLevelProperties(Level level);

    boolean isPropertyInternal(Property property);

    /**
     * Returns a list of the data sources in this server. One element
     * per data source, each element a map whose keys are the XMLA fields
     * describing a data source: "DataSourceName", "DataSourceDescription",
     * "URL", etc. Unrecognized fields are ignored.
     *
     * @param connection Connection
     * @return List of data source definitions
     * @throws OlapException on error
     */
    List<Map<String, Object>> getDataSources(OlapConnection connection) throws OlapException;

    /**
     * Returns a map containing annotations on this element.
     *
     * @param element Element
     * @return Annotation map, never null
     */
    Map<String, Object> getAnnotationMap(MetadataElement element) throws SQLException;

    /**
     * Returns how the name of a hierarchy is to be printed in the
     * response to an XMLA metadata request.
     *
     * @param hierarchy Hierarchy
     * @return Formatted hierarchy name
     */
    String getHierarchyName(Hierarchy hierarchy);

    /**
     * Returns whether the first row in the result of an XML/A drill-through
     * request will be filled with the total count of rows in underlying
     * database.
     */
    boolean isTotalCountEnabled();

    /**
     * Returns the value of a property.
     *
     * @param propertyDefinition Property definition
     * @return Value of the property
     */
    String getPropertyValue(PropertyDefinition propertyDefinition);

    /**
     * Returns a list of MDX keywords.
     *
     * @return list of MDX keywords
     */
    List<String> getKeywords();

    /**
     * Returns a boolean indicating if the specified
     * cell can be drilled on.
     */
    boolean canDrillThrough(Cell cell);

    /**
     * Returns the number of rows returned by a
     * drillthrough on the specified cell. Will also
     * return -1 if it cannot determine the cardinality.
     */
    int getDrillThroughCount(Cell cell);

    /**
     * Makes the connection send a command to the server
     * to flush all caches.
     */
    void flushSchemaCache(OlapConnection conn) throws OlapException;

    /**
     * Returns the key for a given member.
     */
    Object getMemberKey(Member m) throws OlapException;

    /**
     * Returns the ordering key for a given member.
     */
    Object getOrderKey(Member m) throws OlapException;

    class FunctionDefinition {
        public final String functionName;
        public final String description;
        public final String parameterList;
        public final int returnType;
        public final int origin;
        public final String interfaceName;
        public final String caption;

        public FunctionDefinition(String functionName, String description, String parameterList, int returnType,
                                  int origin, String interfaceName, String caption) {
            this.functionName = functionName;
            this.description = description;
            this.parameterList = parameterList;
            this.returnType = returnType;
            this.origin = origin;
            this.interfaceName = interfaceName;
            this.caption = caption;
        }
    }
}
