package mondrian.xmla.context;

import mondrian.xmla.PropertyDefinition;
import mondrian.xmla.RowsetDefinition;
import mondrian.xmla.XmlaRequestContext;
import org.olap4j.*;
import org.olap4j.impl.Olap4jUtil;
import org.olap4j.metadata.*;
import org.olap4j.xmla.server.impl.Util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link XmlaExtra}.
 * Connections based on mondrian's olap4j driver can do better.
 */
public class XmlaExtraImpl implements XmlaExtra {
    public XmlaExtraImpl() {
    }

    public ResultSet executeDrillthrough(OlapStatement olapStatement, String mdx, boolean advanced,
                                         String tabFields, int[] rowCountSlot) throws SQLException {
        return olapStatement.executeQuery(mdx);
    }

    public void setPreferList(OlapConnection connection) {
        // ignore
    }

    public Date getSchemaLoadDate(Schema schema) {
        return new Date();
    }

    public int getLevelCardinality(Level level) throws OlapException {
        return level.getCardinality();
    }

    public void getSchemaFunctionList(List<FunctionDefinition> funDefs, Schema schema,
                                      Util.Predicate1<String> functionFilter) {
        // no function definitions
    }

    public int getHierarchyCardinality(Hierarchy hierarchy) throws OlapException {
        int cardinality = 0;
        for (Level level : hierarchy.getLevels()) {
            cardinality += level.getCardinality();
        }
        return cardinality;
    }

    public int getHierarchyStructure(Hierarchy hierarchy) {
        return 0;
    }

    public boolean isHierarchyParentChild(Hierarchy hierarchy) {
        return false;
    }

    public int getMeasureAggregator(Member member) {
        return RowsetDefinition.MdschemaMeasuresRowset.MDMEASURE_AGGR_UNKNOWN;
    }

    public void checkMemberOrdinal(Member member) throws OlapException {
        // nothing to do
    }

    public boolean shouldReturnCellProperty(CellSet cellSet, Property cellProperty, boolean evenEmpty) {
        return true;
    }

    public List<String> getSchemaRoleNames(Schema schema) {
        return Collections.emptyList();
    }

    public String getSchemaId(Schema schema) {
        return schema.getName();
    }

    public String getCubeType(Cube cube) {
        return RowsetDefinition.MdschemaCubesRowset.MD_CUBTYPE_CUBE;
    }

    public boolean isLevelUnique(Level level) {
        return false;
    }

    public List<Property> getLevelProperties(Level level) {
        return level.getProperties();
    }

    public boolean isPropertyInternal(Property property) {
        return property instanceof Property.StandardMemberProperty
                && ((Property.StandardMemberProperty) property).isInternal()
                || property instanceof Property.StandardCellProperty
                && ((Property.StandardCellProperty) property).isInternal();
    }

    public List<Map<String, Object>> getDataSources(OlapConnection connection) throws OlapException {
        Database olapDb = connection.getOlapDatabase();
        final String modes = createCsv(olapDb.getAuthenticationModes());
        final String providerTypes = createCsv(olapDb.getProviderTypes());
        return Collections.singletonList(Olap4jUtil.mapOf("DataSourceName", (Object) olapDb.getName(),
                "DataSourceDescription", olapDb.getDescription(), "URL", olapDb.getURL(), "DataSourceInfo",
                olapDb.getDataSourceInfo(), "ProviderName", olapDb.getProviderName(), "ProviderType", providerTypes,
                "AuthenticationMode", modes));
    }

    public Map<String, Object> getAnnotationMap(MetadataElement element) throws SQLException {
        return Collections.emptyMap();
    }

    public String getHierarchyName(Hierarchy hierarchy) {
        return hierarchy.getName();
    }

    public boolean isTotalCountEnabled() {
        return false;
    }

    public String getPropertyValue(PropertyDefinition propertyDefinition) {
        if (propertyDefinition.name().equals("Catalog")) {
            String currentCatalog = XmlaRequestContext.getContext().currentCatalog;
            if (Objects.nonNull(currentCatalog)) {
                return currentCatalog;
            }

        }
        return propertyDefinition.value;
    }

    public List<String> getKeywords() {
        return Collections.emptyList();
    }

    public boolean canDrillThrough(Cell cell) {
        return false;
    }

    public int getDrillThroughCount(Cell cell) {
        return -1;
    }

    public void flushSchemaCache(OlapConnection conn) throws OlapException {
        // no op.
    }

    public Object getMemberKey(Member m) throws OlapException {
        return m.getPropertyValue(Property.StandardMemberProperty.MEMBER_KEY);
    }

    public Object getOrderKey(Member m) throws OlapException {
        return m.getOrdinal();
    }

    private static String createCsv(Iterable<? extends Object> iterable) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object o : iterable) {
            if (!first) {
                sb.append(',');
            }
            sb.append(o);
            first = false;
        }
        return sb.toString();
    }

}
