/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.xmla;

import io.kylin.mdx.insight.common.SemanticConfig;
import mondrian.xmla.context.XmlaExtra;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.impl.ArrayNamedListImpl;
import org.olap4j.impl.Olap4jUtil;
import org.olap4j.mdx.*;
import org.olap4j.metadata.*;
import org.olap4j.metadata.XmlaConstants;
import org.olap4j.metadata.Member.TreeOp;
import org.olap4j.xmla.server.impl.Composite;
import org.olap4j.xmla.server.impl.Util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static mondrian.xmla.XmlaConstants.*;
import static mondrian.xmla.constants.XmlaHandlerConstants.XSD_STRING;
import static org.olap4j.xmla.server.impl.Util.filter;

/**
 * <code>RowsetDefinition</code> defines a rowset, including the columns it
 * should contain.
 *
 * <p>See "XML for Analysis Rowsets", page 38 of the XML for Analysis
 * Specification, version 1.1.
 *
 * @author jhyde
 */
public enum RowsetDefinition {
    /**
     * Returns a list of XML for Analysis data sources
     * available on the server or Web Service. (For an
     * example of how these may be published, see
     * "XML for Analysis Implementation Walkthrough"
     * in the XML for Analysis specification.)
     * <p>
     * http://msdn2.microsoft.com/en-us/library/ms126129(SQL.90).aspx
     * <p>
     * <p>
     * restrictions
     * <p>
     * Not supported
     */
    DISCOVER_DATASOURCES(0,
            "Returns a list of XML for Analysis data sources available on the " + "server or Web Service.",
            new Column[]{DiscoverDatasourcesRowset.DataSourceName, DiscoverDatasourcesRowset.DataSourceDescription,
                    DiscoverDatasourcesRowset.URL, DiscoverDatasourcesRowset.DataSourceInfo,
                    DiscoverDatasourcesRowset.ProviderName, DiscoverDatasourcesRowset.ProviderType,
                    DiscoverDatasourcesRowset.AuthenticationMode,},
            // XMLA does not specify a sort order, but olap4j does.
            new Column[]{DiscoverDatasourcesRowset.DataSourceName,}, "06C03D41-F66D-49F3-B1B8-987F7AF4CF18") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DiscoverDatasourcesRowset(request, handler);
        }
    },

    /**
     * Note that SQL Server also returns the data-mining columns.
     * <p>
     * <p>
     * restrictions
     * <p>
     * Not supported
     */
    DISCOVER_SCHEMA_ROWSETS(2,
            "Returns the names, values, and other information of all supported " + "RequestType enumeration values.",
            new Column[]{DiscoverSchemaRowsetsRowset.SchemaName, DiscoverSchemaRowsetsRowset.SchemaGuid,
                    DiscoverSchemaRowsetsRowset.Restrictions, DiscoverSchemaRowsetsRowset.Description,
                    DiscoverSchemaRowsetsRowset.RestrictionsMask},
            null /* not sorted */, "EEA0302B-7922-4992-8991-0E605D0E5593") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DiscoverSchemaRowsetsRowset(request, handler);
        }

        protected void writeRowsetXmlSchemaColumnsRowDef(SaxWriter writer, RowsetDefinition.Column[] columns) {
            writer.startElement("xsd:complexType", "name", "row");
            writer.startElement("xsd:sequence");
            for (Column column : columns) {
                final String name = XmlaUtil.ElementNameEncoder.INSTANCE.encode(column.name);

                if (column == DiscoverSchemaRowsetsRowset.Restrictions) {
                    writer.startElement("xsd:element", "sql:field", column.name, "name", name, "minOccurs", 0,
                            "maxOccurs", "unbounded");
                    writer.startElement("xsd:complexType");
                    writer.startElement("xsd:sequence");
                    writer.element("xsd:element", "name", "Name", "type", "xsd:string", "sql:field", "Name");
                    writer.element("xsd:element", "name", "Type", "type", "xsd:string", "sql:field", "Type");

                    writer.endElement(); // xsd:sequence
                    writer.endElement(); // xsd:complexType
                    writer.endElement(); // xsd:element

                } else {
                    final String xsdType = column.type.columnType;

                    Object[] attrs;
                    if (column.nullable) {
                        if (column.unbounded) {
                            attrs = new Object[]{"sql:field", column.name, "name", name, "type", xsdType, "minOccurs",
                                    0, "maxOccurs", "unbounded"};
                        } else {
                            attrs = new Object[]{"sql:field", column.name, "name", name, "type", xsdType, "minOccurs",
                                    0};
                        }
                    } else {
                        if (column.unbounded) {
                            attrs = new Object[]{"sql:field", column.name, "name", name, "type", xsdType, "maxOccurs",
                                    "unbounded"};
                        } else {
                            attrs = new Object[]{"sql:field", column.name, "name", name, "type", xsdType};
                        }
                    }
                    writer.element("xsd:element", attrs);
                }
            }
            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
        }
    },

    /**
     * restrictions
     * <p>
     * Not supported
     */
    DISCOVER_ENUMERATORS(3,
            "Returns a list of names, data types, and enumeration values for "
                    + "enumerators supported by the provider of a specific data source.",
            new Column[]{DiscoverEnumeratorsRowset.EnumName, DiscoverEnumeratorsRowset.EnumDescription,
                    DiscoverEnumeratorsRowset.EnumType, DiscoverEnumeratorsRowset.ElementName,
                    DiscoverEnumeratorsRowset.ElementDescription, DiscoverEnumeratorsRowset.ElementValue,},
            null /* not sorted */, "55A9E78B-ACCB-45B4-95A6-94C5065617A7") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DiscoverEnumeratorsRowset(request, handler);
        }
    },

    /**
     * restrictions
     * <p>
     * Not supported
     */
    DISCOVER_PROPERTIES(1,
            "Returns a list of information and values about the requested "
                    + "properties that are supported by the specified data source " + "provider.",
            new Column[]{DiscoverPropertiesRowset.PropertyName, DiscoverPropertiesRowset.PropertyDescription,
                    DiscoverPropertiesRowset.PropertyType, DiscoverPropertiesRowset.PropertyAccessType,
                    DiscoverPropertiesRowset.IsRequired, DiscoverPropertiesRowset.Value,},
            null /* not sorted */, "4B40ADFB-8B09-4758-97BB-636E8AE97BCF") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DiscoverPropertiesRowset(request, handler);
        }
    },

    /**
     * restrictions
     * <p>
     * Not supported
     */
    DISCOVER_KEYWORDS(4, "Returns an XML list of keywords reserved by the provider.",
            new Column[]{DiscoverKeywordsRowset.Keyword,}, null /* not sorted */,
            "1426C443-4CDD-4A40-8F45-572FAB9BBAA1") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DiscoverKeywordsRowset(request, handler);
        }
    },

    /**
     * restrictions
     * <p>
     * Not supported
     */
    DISCOVER_LITERALS(5, "Returns information about literals supported by the provider.",
            new Column[]{DiscoverLiteralsRowset.LiteralName, DiscoverLiteralsRowset.LiteralValue,
                    DiscoverLiteralsRowset.LiteralInvalidChars, DiscoverLiteralsRowset.LiteralInvalidStartingChars,
                    DiscoverLiteralsRowset.LiteralMaxLength,},
            null /* not sorted */, "C3EF5ECB-0A07-4665-A140-B075722DBDC2") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DiscoverLiteralsRowset(request, handler);
        }
    },

    /**
     * restrictions
     * <p>
     * Not supported
     */
    DBSCHEMA_CATALOGS(6,
            "Identifies the physical attributes associated with catalogs " + "accessible from the provider.",
            new Column[]{DbschemaCatalogsRowset.CatalogName, DbschemaCatalogsRowset.Description,
                    DbschemaCatalogsRowset.Roles, DbschemaCatalogsRowset.DateModified,},
            new Column[]{DbschemaCatalogsRowset.CatalogName,}, "C8B52211-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DbschemaCatalogsRowset(request, handler);
        }
    },

    /**
     * restrictions
     * <p>
     * Not supported
     * COLUMN_OLAP_TYPE
     */
    DBSCHEMA_COLUMNS(7, null,
            new Column[]{DbschemaColumnsRowset.TableCatalog, DbschemaColumnsRowset.TableSchema,
                    DbschemaColumnsRowset.TableName, DbschemaColumnsRowset.ColumnName,
                    DbschemaColumnsRowset.OrdinalPosition, DbschemaColumnsRowset.ColumnHasDefault,
                    DbschemaColumnsRowset.ColumnFlags, DbschemaColumnsRowset.IsNullable, DbschemaColumnsRowset.DataType,
                    DbschemaColumnsRowset.CharacterMaximumLength, DbschemaColumnsRowset.CharacterOctetLength,
                    DbschemaColumnsRowset.NumericPrecision, DbschemaColumnsRowset.NumericScale,},
            new Column[]{DbschemaColumnsRowset.TableCatalog, DbschemaColumnsRowset.TableSchema,
                    DbschemaColumnsRowset.TableName,},
            "C8B52214-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DbschemaColumnsRowset(request, handler);
        }
    },

    /**
     * restrictions
     * <p>
     * Not supported
     */
    DBSCHEMA_PROVIDER_TYPES(8, null,
            new Column[]{DbschemaProviderTypesRowset.TypeName, DbschemaProviderTypesRowset.DataType,
                    DbschemaProviderTypesRowset.ColumnSize, DbschemaProviderTypesRowset.LiteralPrefix,
                    DbschemaProviderTypesRowset.LiteralSuffix, DbschemaProviderTypesRowset.IsNullable,
                    DbschemaProviderTypesRowset.CaseSensitive, DbschemaProviderTypesRowset.Searchable,
                    DbschemaProviderTypesRowset.UnsignedAttribute, DbschemaProviderTypesRowset.FixedPrecScale,
                    DbschemaProviderTypesRowset.AutoUniqueValue, DbschemaProviderTypesRowset.IsLong,
                    DbschemaProviderTypesRowset.BestMatch,},
            new Column[]{DbschemaProviderTypesRowset.DataType,}, "C8B5222C-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DbschemaProviderTypesRowset(request, handler);
        }
    },
    /** Excel hurts otherwise
     DBSCHEMA_SCHEMATA(
     8, null,
     new Column[] {
     DbschemaSchemataRowset.CatalogName,
     DbschemaSchemataRowset.SchemaName,
     DbschemaSchemataRowset.SchemaOwner,
     },
     new Column[] {
     DbschemaSchemataRowset.CatalogName,
     DbschemaSchemataRowset.SchemaName,
     DbschemaSchemataRowset.SchemaOwner,
     },"absent in MS OLE DB")
     {
     public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
     return new DbschemaSchemataRowset(request, handler);
     }
     },
     **/
    /**
     * http://msdn2.microsoft.com/en-us/library/ms126299(SQL.90).aspx
     * <p>
     * restrictions:
     * TABLE_CATALOG Optional
     * TABLE_SCHEMA Optional
     * TABLE_NAME Optional
     * TABLE_TYPE Optional
     * TABLE_OLAP_TYPE Optional
     * <p>
     * Not supported
     */
    DBSCHEMA_TABLES(9, null,
            new Column[]{DbschemaTablesRowset.TableCatalog, DbschemaTablesRowset.TableSchema,
                    DbschemaTablesRowset.TableName, DbschemaTablesRowset.TableType, DbschemaTablesRowset.TableGuid,
                    DbschemaTablesRowset.Description, DbschemaTablesRowset.TablePropId,
                    DbschemaTablesRowset.DateCreated, DbschemaTablesRowset.DateModified,
                    //TableOlapType,
            },
            new Column[]{DbschemaTablesRowset.TableType, DbschemaTablesRowset.TableCatalog,
                    DbschemaTablesRowset.TableSchema, DbschemaTablesRowset.TableName,},
            "C8B52229-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DbschemaTablesRowset(request, handler);
        }
    },

    /**
     * http://msdn.microsoft.com/library/en-us/oledb/htm/
     * oledbtables_info_rowset.asp
     * <p>
     * <p>
     * restrictions
     * <p>
     * Not supported
     */
    DBSCHEMA_TABLES_INFO(10, null,
            new Column[]{DbschemaTablesInfoRowset.TableCatalog, DbschemaTablesInfoRowset.TableSchema,
                    DbschemaTablesInfoRowset.TableName, DbschemaTablesInfoRowset.TableType,
                    DbschemaTablesInfoRowset.TableGuid, DbschemaTablesInfoRowset.Bookmarks,
                    DbschemaTablesInfoRowset.BookmarkType, DbschemaTablesInfoRowset.BookmarkDataType,
                    DbschemaTablesInfoRowset.BookmarkMaximumLength, DbschemaTablesInfoRowset.BookmarkInformation,
                    DbschemaTablesInfoRowset.TableVersion, DbschemaTablesInfoRowset.Cardinality,
                    DbschemaTablesInfoRowset.Description, DbschemaTablesInfoRowset.TablePropId,},
            null /* cannot find doc -- presume unsorted */, "C8B522E0-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DbschemaTablesInfoRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/en-us/library/ms126032(SQL.90).aspx
     * <p>
     * restrictions
     * CATALOG_NAME Optional
     * SCHEMA_NAME Optional
     * CUBE_NAME Mandatory
     * ACTION_NAME Optional
     * ACTION_TYPE Optional
     * COORDINATE Mandatory
     * COORDINATE_TYPE Mandatory
     * INVOCATION
     * (Optional) The INVOCATION restriction column defaults to the
     * value of MDACTION_INVOCATION_INTERACTIVE. To retrieve all
     * actions, use the MDACTION_INVOCATION_ALL value in the
     * INVOCATION restriction column.
     * CUBE_SOURCE
     * (Optional) A bitmap with one of the following valid values:
     * <p>
     * 1 CUBE
     * 2 DIMENSION
     * <p>
     * Default restriction is a value of 1.
     * <p>
     * Not supported
     */
    MDSCHEMA_ACTIONS(11, null,
            new Column[]{MdschemaActionsRowset.CatalogName, MdschemaActionsRowset.SchemaName,
                    MdschemaActionsRowset.CubeName, MdschemaActionsRowset.ActionName, MdschemaActionsRowset.Coordinate,
                    MdschemaActionsRowset.CoordinateType,},
            new Column[]{
                    // Spec says sort on CATALOG_NAME, SCHEMA_NAME, CUBE_NAME,
                    // ACTION_NAME.
                    MdschemaActionsRowset.CatalogName, MdschemaActionsRowset.SchemaName, MdschemaActionsRowset.CubeName,
                    MdschemaActionsRowset.ActionName,},
            "A07CCD08-8148-11D0-87BB-00C04FC33942") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaActionsRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/en-us/library/ms126271(SQL.90).aspx
     * <p>
     * restrictions
     * CATALOG_NAME Optional.
     * SCHEMA_NAME Optional.
     * CUBE_NAME Optional.
     * CUBE_TYPE
     * (Optional) A bitmap with one of these valid values:
     * 1 CUBE
     * 2 DIMENSION
     * Default restriction is a value of 1.
     * BASE_CUBE_NAME Optional.
     * <p>
     * Not supported
     * CREATED_ON
     * LAST_SCHEMA_UPDATE
     * SCHEMA_UPDATED_BY
     * LAST_DATA_UPDATE
     * DATA_UPDATED_BY
     * ANNOTATIONS
     */
    MDSCHEMA_CUBES(12, null,
            new Column[]{MdschemaCubesRowset.CatalogName, MdschemaCubesRowset.SchemaName,
                    MdschemaCubesRowset.CubeName, MdschemaCubesRowset.CubeType, MdschemaCubesRowset.CubeSource,
                    MdschemaCubesRowset.CubeGuid, MdschemaCubesRowset.CreatedOn, MdschemaCubesRowset.LastSchemaUpdate,
                    MdschemaCubesRowset.SchemaUpdatedBy, MdschemaCubesRowset.LastDataUpdate,
                    MdschemaCubesRowset.DataUpdatedBy, MdschemaCubesRowset.IsDrillthroughEnabled,
                    MdschemaCubesRowset.IsWriteEnabled, MdschemaCubesRowset.IsLinkable,
                    MdschemaCubesRowset.IsSqlEnabled, MdschemaCubesRowset.CubeCaption, MdschemaCubesRowset.Description,
                    MdschemaCubesRowset.Dimensions, MdschemaCubesRowset.Sets, MdschemaCubesRowset.Measures,
                    MdschemaCubesRowset.BaseCubeName},
            new Column[]{MdschemaCubesRowset.CatalogName, MdschemaCubesRowset.SchemaName,
                    MdschemaCubesRowset.CubeName,},
            "C8B522D8-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaCubesRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/en-us/library/ms126180(SQL.90).aspx
     * http://msdn2.microsoft.com/en-us/library/ms126180.aspx
     * <p>
     * restrictions
     * CATALOG_NAME Optional.
     * SCHEMA_NAME Optional.
     * CUBE_NAME Optional.
     * DIMENSION_NAME Optional.
     * DIMENSION_UNIQUE_NAME Optional.
     * CUBE_SOURCE (Optional) A bitmap with one of the following valid
     * values:
     * 1 CUBE
     * 2 DIMENSION
     * Default restriction is a value of 1.
     * <p>
     * DIMENSION_VISIBILITY (Optional) A bitmap with one of the following
     * valid values:
     * 1 Visible
     * 2 Not visible
     * Default restriction is a value of 1.
     */
    MDSCHEMA_DIMENSIONS(13, null,
            new Column[]{MdschemaDimensionsRowset.CatalogName, MdschemaDimensionsRowset.SchemaName,
                    MdschemaDimensionsRowset.CubeName, MdschemaDimensionsRowset.DimensionName,
                    MdschemaDimensionsRowset.DimensionUniqueName, MdschemaDimensionsRowset.DimensionGuid,
                    MdschemaDimensionsRowset.DimensionCaption, MdschemaDimensionsRowset.DimensionOrdinal,
                    MdschemaDimensionsRowset.DimensionType, MdschemaDimensionsRowset.DimensionCardinality,
                    MdschemaDimensionsRowset.DefaultHierarchy, MdschemaDimensionsRowset.Description,
                    MdschemaDimensionsRowset.IsVirtual, MdschemaDimensionsRowset.IsReadWrite,
                    MdschemaDimensionsRowset.DimensionUniqueSettings,
                    MdschemaDimensionsRowset.DimensionMasterUniqueName, MdschemaDimensionsRowset.DimensionIsVisible,
                    MdschemaDimensionsRowset.Hierarchies,},
            new Column[]{MdschemaDimensionsRowset.CatalogName, MdschemaDimensionsRowset.SchemaName,
                    MdschemaDimensionsRowset.CubeName, MdschemaDimensionsRowset.DimensionName,},
            "C8B522D9-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaDimensionsRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/en-us/library/ms126257(SQL.90).aspx
     * <p>
     * restrictions
     * LIBRARY_NAME Optional.
     * INTERFACE_NAME Optional.
     * FUNCTION_NAME Optional.
     * ORIGIN Optional.
     * <p>
     * Not supported
     * DLL_NAME
     * Optional
     * HELP_FILE
     * Optional
     * HELP_CONTEXT
     * Optional
     * - SQL Server xml schema says that this must be present
     * OBJECT
     * Optional
     * CAPTION The display caption for the function.
     */
    MDSCHEMA_FUNCTIONS(14, null,
            new Column[]{MdschemaFunctionsRowset.FunctionName, MdschemaFunctionsRowset.Description,
                    MdschemaFunctionsRowset.ParameterList, MdschemaFunctionsRowset.ReturnType,
                    MdschemaFunctionsRowset.Origin, MdschemaFunctionsRowset.InterfaceName,
                    MdschemaFunctionsRowset.LibraryName, MdschemaFunctionsRowset.Caption,},
            new Column[]{MdschemaFunctionsRowset.LibraryName, MdschemaFunctionsRowset.InterfaceName,
                    MdschemaFunctionsRowset.FunctionName, MdschemaFunctionsRowset.Origin,},
            "A07CCD07-8148-11D0-87BB-00C04FC33942") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaFunctionsRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/en-us/library/ms126062(SQL.90).aspx
     * <p>
     * restrictions
     * CATALOG_NAME Optional.
     * SCHEMA_NAME Optional.
     * CUBE_NAME Optional.
     * DIMENSION_UNIQUE_NAME Optional.
     * HIERARCHY_NAME Optional.
     * HIERARCHY_UNIQUE_NAME Optional.
     * HIERARCHY_ORIGIN
     * (Optional) A default restriction is in effect
     * on MD_USER_DEFINED and MD_SYSTEM_ENABLED.
     * CUBE_SOURCE
     * (Optional) A bitmap with one of the following valid values:
     * 1 CUBE
     * 2 DIMENSION
     * Default restriction is a value of 1.
     * HIERARCHY_VISIBILITY
     * (Optional) A bitmap with one of the following valid values:
     * 1 Visible
     * 2 Not visible
     * Default restriction is a value of 1.
     * <p>
     * Not supported
     * HIERARCHY_ORIGIN
     * HIERARCHY_DISPLAY_FOLDER
     * INSTANCE_SELECTION
     */
    MDSCHEMA_HIERARCHIES(15, null,
            new Column[]{MdschemaHierarchiesRowset.CatalogName, MdschemaHierarchiesRowset.SchemaName,
                    MdschemaHierarchiesRowset.CubeName, MdschemaHierarchiesRowset.DimensionUniqueName,
                    MdschemaHierarchiesRowset.HierarchyName, MdschemaHierarchiesRowset.HierarchyUniqueName,
                    MdschemaHierarchiesRowset.HierarchyGuid, MdschemaHierarchiesRowset.HierarchyCaption,
                    MdschemaHierarchiesRowset.DimensionType, MdschemaHierarchiesRowset.HierarchyCardinality,
                    MdschemaHierarchiesRowset.DefaultMember, MdschemaHierarchiesRowset.AllMember,
                    MdschemaHierarchiesRowset.Description, MdschemaHierarchiesRowset.Structure,
                    MdschemaHierarchiesRowset.IsVirtual, MdschemaHierarchiesRowset.IsReadWrite,
                    MdschemaHierarchiesRowset.DimensionUniqueSettings, MdschemaHierarchiesRowset.DimensionIsVisible,
                    MdschemaHierarchiesRowset.HierarchyOrdinal, MdschemaHierarchiesRowset.DimensionIsShared,
                    MdschemaHierarchiesRowset.HierarchyIsVisible, MdschemaHierarchiesRowset.HierarchyOrigin,
                    MdschemaHierarchiesRowset.CubeSource, MdschemaHierarchiesRowset.HierarchyVisibility,
                    MdschemaHierarchiesRowset.Levels, MdschemaHierarchiesRowset.HIERARCHY_DISPLAY_FOLDER,
                    MdschemaHierarchiesRowset.InstanceSelection,
                    MdschemaHierarchiesRowset.GroupingBehavior, MdschemaHierarchiesRowset.StructureType},
            new Column[]{MdschemaHierarchiesRowset.CatalogName, MdschemaHierarchiesRowset.SchemaName,
                    MdschemaHierarchiesRowset.CubeName, MdschemaHierarchiesRowset.DimensionUniqueName,
                    MdschemaHierarchiesRowset.HierarchyName,},
            "C8B522DA-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaHierarchiesRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/en-us/library/ms126038(SQL.90).aspx
     * <p>
     * restriction
     * CATALOG_NAME Optional.
     * SCHEMA_NAME Optional.
     * CUBE_NAME Optional.
     * DIMENSION_UNIQUE_NAME Optional.
     * HIERARCHY_UNIQUE_NAME Optional.
     * LEVEL_NAME Optional.
     * LEVEL_UNIQUE_NAME Optional.
     * LEVEL_ORIGIN
     * (Optional) A default restriction is in effect
     * on MD_USER_DEFINED and MD_SYSTEM_ENABLED
     * CUBE_SOURCE
     * (Optional) A bitmap with one of the following valid values:
     * 1 CUBE
     * 2 DIMENSION
     * Default restriction is a value of 1.
     * LEVEL_VISIBILITY
     * (Optional) A bitmap with one of the following values:
     * 1 Visible
     * 2 Not visible
     * Default restriction is a value of 1.
     * <p>
     * Not supported
     * CUSTOM_ROLLUP_SETTINGS
     * LEVEL_UNIQUE_SETTINGS
     * LEVEL_ORDERING_PROPERTY
     * LEVEL_DBTYPE
     * LEVEL_MASTER_UNIQUE_NAME
     * LEVEL_NAME_SQL_COLUMN_NAME Customers:(All)!NAME
     * LEVEL_KEY_SQL_COLUMN_NAME Customers:(All)!KEY
     * LEVEL_UNIQUE_NAME_SQL_COLUMN_NAME Customers:(All)!UNIQUE_NAME
     * LEVEL_ATTRIBUTE_HIERARCHY_NAME
     * LEVEL_KEY_CARDINALITY
     * LEVEL_ORIGIN
     */
    MDSCHEMA_LEVELS(16, null, new Column[]{MdschemaLevelsRowset.CatalogName, MdschemaLevelsRowset.SchemaName,
            MdschemaLevelsRowset.CubeName, MdschemaLevelsRowset.DimensionUniqueName,
            MdschemaLevelsRowset.HierarchyUniqueName, MdschemaLevelsRowset.LevelName,
            MdschemaLevelsRowset.LevelUniqueName, MdschemaLevelsRowset.LevelGuid, MdschemaLevelsRowset.LevelCaption,
            MdschemaLevelsRowset.LevelNumber, MdschemaLevelsRowset.LevelCardinality, MdschemaLevelsRowset.LevelType,
            MdschemaLevelsRowset.Description, MdschemaLevelsRowset.CustomRollupSettings, MdschemaLevelsRowset.LevelUniqueSettings,
            MdschemaLevelsRowset.LevelIsVisible, MdschemaLevelsRowset.LEVEL_ORDERING_PROPERTY, MdschemaLevelsRowset.LEVEL_DBTYPE,
            MdschemaLevelsRowset.LEVEL_NAME_SQL_COLUMN_NAME, MdschemaLevelsRowset.LEVEL_KEY_SQL_COLUMN_NAME,
            MdschemaLevelsRowset.LEVEL_UNIQUE_NAME_SQL_COLUMN_NAME, MdschemaLevelsRowset.LevHieAttrName,
            MdschemaLevelsRowset.LEVEL_KEY_CARDINALITY, MdschemaLevelsRowset.LevelOrigin, MdschemaLevelsRowset.CubeSource,
            MdschemaLevelsRowset.LevelVisibility},
            new Column[]{MdschemaLevelsRowset.CatalogName, MdschemaLevelsRowset.SchemaName,
                    MdschemaLevelsRowset.CubeName, MdschemaLevelsRowset.DimensionUniqueName,
                    MdschemaLevelsRowset.HierarchyUniqueName, MdschemaLevelsRowset.LevelNumber,},
            "C8B522DB-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaLevelsRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/en-us/library/ms126250(SQL.90).aspx
     * <p>
     * restrictions
     * CATALOG_NAME Optional.
     * SCHEMA_NAME Optional.
     * CUBE_NAME Optional.
     * MEASURE_NAME Optional.
     * MEASURE_UNIQUE_NAME Optional.
     * CUBE_SOURCE
     * (Optional) A bitmap with one of the following valid values:
     * 1 CUBE
     * 2 DIMENSION
     * Default restriction is a value of 1.
     * MEASURE_VISIBILITY
     * (Optional) A bitmap with one of the following valid values:
     * 1 Visible
     * 2 Not Visible
     * Default restriction is a value of 1.
     * <p>
     * Not supported
     * MEASURE_GUID
     * NUMERIC_PRECISION
     * NUMERIC_SCALE
     * MEASURE_UNITS
     * EXPRESSION
     * MEASURE_NAME_SQL_COLUMN_NAME
     * MEASURE_UNQUALIFIED_CAPTION
     * MEASUREGROUP_NAME
     * MEASURE_DISPLAY_FOLDER
     * DEFAULT_FORMAT_STRING
     */
    MDSCHEMA_MEASURES(17, null, new Column[]{MdschemaMeasuresRowset.CatalogName, MdschemaMeasuresRowset.SchemaName,
            MdschemaMeasuresRowset.CubeName, MdschemaMeasuresRowset.MeasureName,
            MdschemaMeasuresRowset.MeasureUniqueName, MdschemaMeasuresRowset.MeasureCaption,
            MdschemaMeasuresRowset.MeasureGuid, MdschemaMeasuresRowset.MeasureAggregator,
            MdschemaMeasuresRowset.DataType,
            MdschemaMeasuresRowset.NumericPrecision, MdschemaMeasuresRowset.NumericScale,
            MdschemaMeasuresRowset.MeasureIsVisible, MdschemaMeasuresRowset.LevelsList,
            MdschemaMeasuresRowset.Description, MdschemaMeasuresRowset.FormatString,
            MdschemaMeasuresRowset.MEASUREGROUP_NAME, MdschemaMeasuresRowset.MEASURE_DISPLAY_FOLDER,
            MdschemaMeasuresRowset.CubeSource, MdschemaMeasuresRowset.MeasureVisibility},
            new Column[]{MdschemaMeasuresRowset.CatalogName, MdschemaMeasuresRowset.SchemaName,
                    MdschemaMeasuresRowset.CubeName, MdschemaMeasuresRowset.MeasureName,},
            "C8B522DC-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaMeasuresRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/es-es/library/ms126046.aspx
     * <p>
     * <p>
     * restrictions
     * CATALOG_NAME Optional.
     * SCHEMA_NAME Optional.
     * CUBE_NAME Optional.
     * DIMENSION_UNIQUE_NAME Optional.
     * HIERARCHY_UNIQUE_NAME Optional.
     * LEVEL_UNIQUE_NAME Optional.
     * LEVEL_NUMBER Optional.
     * MEMBER_NAME Optional.
     * MEMBER_UNIQUE_NAME Optional.
     * MEMBER_CAPTION Optional.
     * MEMBER_TYPE Optional.
     * TREE_OP (Optional) Only applies to a single member:
     * MDTREEOP_ANCESTORS (0x20) returns all of the ancestors.
     * MDTREEOP_CHILDREN (0x01) returns only the immediate children.
     * MDTREEOP_SIBLINGS (0x02) returns members on the same level.
     * MDTREEOP_PARENT (0x04) returns only the immediate parent.
     * MDTREEOP_SELF (0x08) returns itself in the list of
     * returned rows.
     * MDTREEOP_DESCENDANTS (0x10) returns all of the descendants.
     * CUBE_SOURCE (Optional) A bitmap with one of the
     * following valid values:
     * 1 CUBE
     * 2 DIMENSION
     * Default restriction is a value of 1.
     * <p>
     * Not supported
     */
    MDSCHEMA_MEMBERS(18, null, new Column[]{MdschemaMembersRowset.CatalogName, MdschemaMembersRowset.SchemaName,
            MdschemaMembersRowset.CubeName, MdschemaMembersRowset.DimensionUniqueName,
            MdschemaMembersRowset.HierarchyUniqueName, MdschemaMembersRowset.LevelUniqueName,
            MdschemaMembersRowset.LevelNumber, MdschemaMembersRowset.MemberOrdinal, MdschemaMembersRowset.MemberName,
            MdschemaMembersRowset.MemberUniqueName, MdschemaMembersRowset.MemberType, MdschemaMembersRowset.MemberGuid,
            MdschemaMembersRowset.MemberCaption, MdschemaMembersRowset.ChildrenCardinality,
            MdschemaMembersRowset.ParentLevel, MdschemaMembersRowset.ParentUniqueName,
            MdschemaMembersRowset.ParentCount, MdschemaMembersRowset.TreeOp_, MdschemaMembersRowset.Depth,},
            new Column[]{MdschemaMembersRowset.CatalogName, MdschemaMembersRowset.SchemaName,
                    MdschemaMembersRowset.CubeName, MdschemaMembersRowset.DimensionUniqueName,
                    MdschemaMembersRowset.HierarchyUniqueName, MdschemaMembersRowset.LevelUniqueName,
                    MdschemaMembersRowset.LevelNumber, MdschemaMembersRowset.MemberOrdinal,},
            "C8B522DE-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaMembersRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/en-us/library/ms126309(SQL.90).aspx
     * <p>
     * restrictions
     * CATALOG_NAME Mandatory
     * SCHEMA_NAME Optional
     * CUBE_NAME Optional
     * DIMENSION_UNIQUE_NAME Optional
     * HIERARCHY_UNIQUE_NAME Optional
     * LEVEL_UNIQUE_NAME Optional
     * <p>
     * MEMBER_UNIQUE_NAME Optional
     * PROPERTY_NAME Optional
     * PROPERTY_TYPE Optional
     * PROPERTY_CONTENT_TYPE
     * (Optional) A default restriction is in place on MDPROP_MEMBER
     * OR MDPROP_CELL.
     * PROPERTY_ORIGIN
     * (Optional) A default restriction is in place on MD_USER_DEFINED
     * OR MD_SYSTEM_ENABLED
     * CUBE_SOURCE
     * (Optional) A bitmap with one of the following valid values:
     * 1 CUBE
     * 2 DIMENSION
     * Default restriction is a value of 1.
     * PROPERTY_VISIBILITY
     * (Optional) A bitmap with one of the following valid values:
     * 1 Visible
     * 2 Not visible
     * Default restriction is a value of 1.
     * <p>
     * Not supported
     * PROPERTY_ORIGIN
     * CUBE_SOURCE
     * PROPERTY_VISIBILITY
     * CHARACTER_MAXIMUM_LENGTH
     * CHARACTER_OCTET_LENGTH
     * NUMERIC_PRECISION
     * NUMERIC_SCALE
     * DESCRIPTION
     * SQL_COLUMN_NAME
     * LANGUAGE
     * PROPERTY_ATTRIBUTE_HIERARCHY_NAME
     * PROPERTY_CARDINALITY
     * MIME_TYPE
     * PROPERTY_IS_VISIBLE
     */
    MDSCHEMA_PROPERTIES(19, null,
            new Column[]{MdschemaPropertiesRowset.CatalogName, MdschemaPropertiesRowset.SchemaName,
                    MdschemaPropertiesRowset.CubeName, MdschemaPropertiesRowset.DimensionUniqueName,
                    MdschemaPropertiesRowset.HierarchyUniqueName, MdschemaPropertiesRowset.LevelUniqueName,
                    MdschemaPropertiesRowset.MemberUniqueName, MdschemaPropertiesRowset.PropertyName,
                    MdschemaPropertiesRowset.PropertyCaption, MdschemaPropertiesRowset.PropertyType,
                    MdschemaPropertiesRowset.DataType,
                    MdschemaPropertiesRowset.CharacterMaximumLength, MdschemaPropertiesRowset.NumericPrecision,
                    MdschemaPropertiesRowset.NumericScale,
                    MdschemaPropertiesRowset.PropertyContentType,
                    MdschemaPropertiesRowset.Description},
            null /* not sorted */, "C8B522DD-5CF3-11CE-ADE5-00AA0044773D") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaPropertiesRowset(request, handler);
        }
    },

    /**
     * http://msdn2.microsoft.com/en-us/library/ms126290(SQL.90).aspx
     * <p>
     * restrictions
     * CATALOG_NAME Optional.
     * SCHEMA_NAME Optional.
     * CUBE_NAME Optional.
     * SET_NAME Optional.
     * SCOPE Optional.
     * HIERARCHY_UNIQUE_NAME Optional.
     * CUBE_SOURCE Optional.
     * Note: Only one hierarchy can be included, and only those named
     * sets whose hierarchies exactly match the restriction are
     * returned.
     * <p>
     * Not supported
     * EXPRESSION
     * DIMENSIONS
     * SET_DISPLAY_FOLDER
     */
    MDSCHEMA_SETS(20, null,
            new Column[]{MdschemaSetsRowset.CatalogName, MdschemaSetsRowset.SchemaName, MdschemaSetsRowset.CubeName, MdschemaSetsRowset.SetName, MdschemaSetsRowset.SetCaption,
                    MdschemaSetsRowset.Scope, MdschemaSetsRowset.Description, MdschemaSetsRowset.Expression, MdschemaSetsRowset.Dimensions, MdschemaSetsRowset.SetDisplayFolder,
                    MdschemaSetsRowset.HierarchyUniqueName, MdschemaSetsRowset.CubeSource, MdschemaSetsRowset.SetEvaluationContext},
            new Column[]{MdschemaSetsRowset.CatalogName, MdschemaSetsRowset.SchemaName, MdschemaSetsRowset.CubeName, MdschemaSetsRowset.CubeSource,
                    MdschemaSetsRowset.SetEvaluationContext},
            "A07CCD0B-8148-11D0-87BB-00C04FC33942") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaSetsRowset(request, handler);
        }
    },

    /**
     * https://docs.microsoft.com/en-us/bi-reference/schema-rowsets/xml/discover-xml-metadata-rowset
     */
    DISCOVER_XML_METADATA(21, "Returns an XML document describing a requested object. " +
            "The rowset that is returned always consists of one row and one column.",
            new Column[]{DiscoverXmlMetadataRowset.Metadata, DiscoverXmlMetadataRowset.ObjectExpansion},
            null, "") {
        @Override
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new DiscoverXmlMetadataRowset(request, handler);
        }
    },

    MDSCHEMA_KPIS(22, "Describes the key performance indicators (KPIs) within a database.",
            new Column[]{MdschemaKpisRowset.CatalogName, MdschemaKpisRowset.SchemaName,
                    MdschemaKpisRowset.CubeName, MdschemaKpisRowset.MeasuregroupName, MdschemaKpisRowset.KpiName,
                    MdschemaKpisRowset.KpiCaption, MdschemaKpisRowset.KpiDescrption, MdschemaKpisRowset.KpiDisplayFolder,
                    MdschemaKpisRowset.KpiValue, MdschemaKpisRowset.KpiGoal, MdschemaKpisRowset.KpiStatus,
                    MdschemaKpisRowset.KpiTrend, MdschemaKpisRowset.KpiStatusGraphic, MdschemaKpisRowset.KpiTrendGraphic,
                    MdschemaKpisRowset.KpiWeight, MdschemaKpisRowset.KpiCurrentTimeMember, MdschemaKpisRowset.Scope,
                    MdschemaKpisRowset.Annotations},
            null, "") {
        @Override
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaKpisRowset(request, handler);
        }
    },

    MDSCHEMA_MEASUREGROUPS(23, null, new Column[]{MdschemaMeasureGroupsRowset.CatalogName, MdschemaMeasureGroupsRowset.SchemaName,
            MdschemaMeasureGroupsRowset.CubeName, MdschemaMeasureGroupsRowset.MeasureGroupName,
            MdschemaMeasureGroupsRowset.Description, MdschemaMeasureGroupsRowset.IsWriteEnabled, MdschemaMeasureGroupsRowset.MeasureGroupCaption},
            new Column[]{MdschemaMeasureGroupsRowset.CatalogName, MdschemaMeasureGroupsRowset.SchemaName,
                    MdschemaMeasureGroupsRowset.CubeName, MdschemaMeasureGroupsRowset.MeasureGroupName},
            "E1625EBF-FA96-42FD-BEA6-DB90ADAFD96B") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaMeasureGroupsRowset(request, handler);
        }
    },

    MDSCHEMA_MEASUREGROUP_DIMENSIONS(24, null, new Column[]{MdschemaMeasureGroupsDimensionsRowset.CatalogName, MdschemaMeasureGroupsDimensionsRowset.SchemaName,
            MdschemaMeasureGroupsDimensionsRowset.CubeName, MdschemaMeasureGroupsDimensionsRowset.MeasureGroupName,
            MdschemaMeasureGroupsDimensionsRowset.MeasureGroupCardinality, MdschemaMeasureGroupsDimensionsRowset.DimensionUniqueName, MdschemaMeasureGroupsDimensionsRowset.DimensionCardinality,
            MdschemaMeasureGroupsDimensionsRowset.DimensionIsVisible, MdschemaMeasureGroupsDimensionsRowset.DimensionIsFactDimension, MdschemaMeasureGroupsDimensionsRowset.DimensionPath,
            MdschemaMeasureGroupsDimensionsRowset.MeasureGroupDimension, MdschemaMeasureGroupsDimensionsRowset.DimensionGranularity, MdschemaMeasureGroupsDimensionsRowset.DimensionVisibility},
            new Column[]{MdschemaMeasureGroupsDimensionsRowset.CatalogName, MdschemaMeasureGroupsDimensionsRowset.SchemaName,
                    MdschemaMeasureGroupsDimensionsRowset.CubeName, MdschemaMeasureGroupsDimensionsRowset.MeasureGroupName, MdschemaMeasureGroupsDimensionsRowset.DimensionUniqueName},
            "A07CCD33-8148-11D0-87BB-00C04FC33942") {
        public Rowset getRowset(XmlaRequest request, XmlaHandler handler) {
            return new MdschemaMeasureGroupsDimensionsRowset(request, handler);
        }
    };

    public transient final Column[] columnDefinitions;
    transient final Column[] sortColumnDefinitions;
    transient final String schemaGuid;

    /**
     * Date the schema was last modified.
     *
     * <p>TODO: currently schema grammar does not support modify date
     * so we return just some date for now.
     */
    private static final String dateModified = "2005-01-25T17:35:32";
    private final String description;

    public static final String UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
            + "[0-9a-fA-F]{12}";

    private static final String XML_METADATA_FILE = "discover_xml_metadata.xml";

    /**
     * Creates a rowset definition.
     *
     * @param ordinal               Rowset ordinal, per OLE DB for OLAP
     * @param description           Description
     * @param columnDefinitions     List of column definitions
     * @param sortColumnDefinitions List of column definitions to sort on,
     */
    RowsetDefinition(int ordinal, String description, Column[] columnDefinitions, Column[] sortColumnDefinitions,
                     String guid) {
        Util.discard(ordinal);
        this.description = description;
        this.columnDefinitions = columnDefinitions;
        this.sortColumnDefinitions = sortColumnDefinitions;
        this.schemaGuid = guid;
    }

    public abstract Rowset getRowset(XmlaRequest request, XmlaHandler handler);

    public Column lookupColumn(String name) {
        for (Column columnDefinition : columnDefinitions) {
            if (columnDefinition.name.equals(name)) {
                return columnDefinition;
            }
        }
        return null;
    }

    /**
     * Returns a comparator with which to sort rows of this rowset definition.
     * The sort order is defined by the {@link #sortColumnDefinitions} field.
     * If the rowset is not sorted, returns null.
     */
    Comparator<Rowset.Row> getComparator() {
        if (sortColumnDefinitions == null) {
            return null;
        }
        return new Comparator<Rowset.Row>() {
            public int compare(Rowset.Row row1, Rowset.Row row2) {
                // A faster implementation is welcome.
                for (Column sortColumn : sortColumnDefinitions) {
                    Comparable val1 = (Comparable) row1.get(sortColumn.name);
                    Comparable val2 = (Comparable) row2.get(sortColumn.name);
                    if ((val1 == null) && (val2 == null)) {
                        // columns can be optional, compare next column
                        continue;
                    } else if (val1 == null) {
                        return -1;
                    } else if (val2 == null) {
                        return 1;
                    } else if (val1 instanceof String && val2 instanceof String) {
                        int v = ((String) val1).compareToIgnoreCase((String) val2);
                        // if equal (= 0), compare next column
                        if (v != 0) {
                            return v;
                        }
                    } else {
                        int v = val1.compareTo(val2);
                        // if equal (= 0), compare next column
                        if (v != 0) {
                            return v;
                        }
                    }
                }
                return 0;
            }
        };
    }

    /**
     * Generates an XML schema description to the writer.
     * This is broken into top, Row definition and bottom so that on a
     * case by case basis a RowsetDefinition can redefine the Row
     * definition output. The default assumes a flat set of elements, but
     * for example, SchemaRowsets has a element with child elements.
     *
     * @param writer SAX writer
     * @see XmlaHandler#writeDatasetXmlSchema(SaxWriter, mondrian.xmla.XmlaHandler.SetType)
     */
    public void writeRowsetXmlSchema(SaxWriter writer) {
        writeRowsetXmlSchemaColumns(writer, columnDefinitions);
    }

    public void writeRowsetXmlSchemaColumns(SaxWriter writer, RowsetDefinition.Column[] columns) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        writeRowsetXmlSchemaColumns(writer, columns, Objects.isNull(context) ? null : context.clientType);
    }

    public void writeRowsetXmlSchemaColumns(SaxWriter writer, RowsetDefinition.Column[] columns, String clientType) {
        if (XmlaRequestContext.ClientType.POWERBI.equals(clientType)) {
            writeRowsetXmlSchemaTop_PowerBI(writer);
        } else {
            writeRowsetXmlSchemaTop(writer, clientType);
        }
        writeRowsetXmlSchemaColumnsRowDef(writer, columns);
        writeRowsetXmlSchemaBottom(writer);
    }

    protected void writeRowsetXmlSchemaTop(SaxWriter writer, String clientType) {
        writer.startElement("xsd:schema", "targetNamespace", NS_XMLA_ROWSET,
                "xmlns:sql", "urn:schemas-microsoft-com:xml-sql", "elementFormDefault", "qualified");

        writer.startElement("xsd:element", "name", "root");
        writer.startElement("xsd:complexType");
        writer.startElement("xsd:sequence");
        writer.element("xsd:element", "name", "row", "type", "row", "minOccurs", 0, "maxOccurs", "unbounded");
        writer.endElement(); // xsd:sequence
        writer.endElement(); // xsd:complexType
        writer.endElement(); // xsd:element

        // MS SQL includes this in its schema section even thought
        // its not need for most queries.
        writer.startElement("xsd:simpleType", "name", "uuid");
        writer.startElement("xsd:restriction", "base", XSD_STRING);
        writer.element("xsd:pattern", "value", UUID_PATTERN);

        writer.endElement(); // xsd:restriction
        writer.endElement(); // xsd:simpleType

        if (clientType != null && clientType.equals(XmlaRequestContext.ClientType.POWERBI_DESKTOP)) {
            //Also needs xmlDocument here
            writer.startElement("xsd:complexType", "name", "xmlDocument");
            writer.startElement("xsd:sequence");
            writer.element("xsd:any");

            writer.endElement(); // xsd:sequence
            writer.endElement(); // xsd:complexType
        }
    }

    protected void writeRowsetXmlSchemaTop(SaxWriter writer) {
        writeRowsetXmlSchemaTop(writer, XmlaRequestContext.getContext().clientType);
    }

    protected void writeRowsetXmlSchemaTop_PowerBI(SaxWriter writer) {
        writer.startElement("xsd:schema", "xmlns:xsd", NS_XSD, "xmlns", NS_XMLA_ROWSET, "xmlns:xsi", NS_XSI,
                "xmlns:sql", "urn:schemas-microsoft-com:xml-sql", "targetNamespace", NS_XMLA_ROWSET,
                "elementFormDefault", "qualified");

        writer.startElement("xsd:element", "name", "root");
        writer.startElement("xsd:complexType");
        writer.startElement("xsd:sequence");
        writer.element("xsd:element", "name", "row", "type", "row", "minOccurs", 0, "maxOccurs", "unbounded");
        writer.endElement(); // xsd:sequence
        writer.endElement(); // xsd:complexType
        writer.endElement(); // xsd:element

        // MS SQL includes this in its schema section even thought
        // its not need for most queries.
        writer.startElement("xsd:simpleType", "name", "uuid");
        writer.startElement("xsd:restriction", "base", "xsd:string");
        writer.element("xsd:pattern", "value", UUID_PATTERN);

        writer.endElement(); // xsd:restriction
        writer.endElement(); // xsd:simpleType
    }

    protected void writeRowsetXmlSchemaColumnsRowDef(SaxWriter writer, RowsetDefinition.Column[] columns) {
        writer.startElement("xsd:complexType", "name", "row");
        writer.startElement("xsd:sequence");
        for (Column column : columns) {
            final String name = XmlaUtil.ElementNameEncoder.INSTANCE.encode(column.name);
            final String xsdType = column.type.columnType;

            Object[] attrs;
            if (column.nullable) {
                if (column.unbounded) {
                    attrs = new Object[]{"sql:field", column.name, "name", name, "type", xsdType, "minOccurs", 0,
                            "maxOccurs", "unbounded"};
                } else {
                    attrs = new Object[]{"sql:field", column.name, "name", name, "type", xsdType, "minOccurs", 0};
                }
            } else {
                if (column.unbounded) {
                    attrs = new Object[]{"sql:field", column.name, "name", name, "type", xsdType, "maxOccurs",
                            "unbounded"};
                } else {
                    attrs = new Object[]{"sql:field", column.name, "name", name, "type", xsdType};
                }
            }
            writer.element("xsd:element", attrs);
        }
        writer.endElement(); // xsd:sequence
        writer.endElement(); // xsd:complexType
    }

    protected void writeRowsetXmlSchemaBottom(SaxWriter writer) {
        writer.endElement(); // xsd:schema
    }

    public enum Type {
        String("xsd:string"), StringArray("xsd:string"), Array("xsd:string"),
        Enumeration("xsd:string"), EnumerationArray("xsd:string"), EnumString("xsd:string"),
        Boolean("xsd:boolean"), StringSometimesArray("xsd:string"), Integer("xsd:int"),
        UnsignedInteger("xsd:unsignedInt"), DateTime("xsd:dateTime"), Rowset(null),
        Short("xsd:short"), UUID("uuid"), UnsignedShort("xsd:unsignedShort"),
        Long("xsd:long"), UnsignedLong("xsd:unsignedLong"), XmlDocument("xmlDocument");

        public final String columnType;

        Type(String columnType) {
            this.columnType = columnType;
        }

        boolean isEnum() {
            return this == Enumeration || this == EnumerationArray || this == EnumString;
        }

        String getName() {
            return this == String ? "string" : name();
        }
    }

    private static XmlaConstants.DBType getDBTypeFromProperty(Property prop) {
        switch (prop.getDatatype()) {
            case STRING:
                return XmlaConstants.DBType.WSTR;
            case INTEGER:
            case UNSIGNED_INTEGER:
            case DOUBLE:
                return XmlaConstants.DBType.R8;
            case BOOLEAN:
                return XmlaConstants.DBType.BOOL;
            default:
                // TODO: what type is it really, its not a string
                return XmlaConstants.DBType.WSTR;
        }
    }

    public static class Column {

        /**
         * This is used as the true value for the restriction parameter.
         */
        static final boolean RESTRICTION = true;
        /**
         * This is used as the false value for the restriction parameter.
         */
        static final boolean NOT_RESTRICTION = false;

        /**
         * This is used as the false value for the nullable parameter.
         */
        static final boolean REQUIRED = false;
        /**
         * This is used as the true value for the nullable parameter.
         */
        static final boolean OPTIONAL = true;

        /**
         * This is used as the false value for the unbounded parameter.
         */
        static final boolean ONE_MAX = false;
        /**
         * This is used as the true value for the unbounded parameter.
         */
        static final boolean UNBOUNDED = true;

        public final String name;
        final Type type;
        final Enumeration enumeration;
        final String description;
        final boolean restriction;
        final boolean nullable;
        final boolean unbounded;

        /**
         * Creates a column.
         *
         * @param name           Name of column
         * @param type           A {@link mondrian.xmla.RowsetDefinition.Type} value
         * @param enumeratedType Must be specified for enumeration or array
         *                       of enumerations
         * @param description    Description of column
         * @param restriction    Whether column can be used as a filter on its
         *                       rowset
         * @param nullable       Whether column can contain null values
         * @pre type != null
         * @pre (type = = Type.Enumeration
         *| | type = = Type.EnumerationArray
         *| | type = = Type.EnumString)
         * == (enumeratedType != null)
         * @pre description == null || description.indexOf('\r') == -1
         */
        Column(String name, Type type, Enumeration enumeratedType, boolean restriction, boolean nullable,
               String description) {
            this(name, type, enumeratedType, restriction, nullable, ONE_MAX, description);
        }

        Column(String name, Type type, Enumeration enumeratedType, boolean restriction, boolean nullable,
               boolean unbounded, String description) {
            assert type != null;
            assert (type == Type.Enumeration || type == Type.EnumerationArray
                    || type == Type.EnumString) == (enumeratedType != null);
            // Line endings must be UNIX style (LF) not Windows style (LF+CR).
            // Thus the client will receive the same XML, regardless
            // of the server O/S.
            assert description == null || description.indexOf('\r') == -1;
            this.name = name;
            this.type = type;
            this.enumeration = enumeratedType;
            this.description = description;
            this.restriction = restriction;
            this.nullable = nullable;
            this.unbounded = unbounded;
        }

        /**
         * Retrieves a value of this column from a row. The base implementation
         * uses reflection to call an accessor method; a derived class may
         * provide a different implementation.
         *
         * @param row Row
         */
        protected Object get(Object row) {
            return getFromAccessor(row);
        }

        /**
         * Retrieves the value of this column "MyColumn" from a field called
         * "myColumn".
         *
         * @param row Current row
         * @return Value of given this property of the given row
         */
        protected final Object getFromField(Object row) {
            try {
                String javaFieldName = name.substring(0, 1).toLowerCase() + name.substring(1);
                Field field = row.getClass().getField(javaFieldName);
                return field.get(row);
            } catch (NoSuchFieldException e) {
                throw Util.newInternal(e, "Error while accessing rowset column " + name);
            } catch (SecurityException e) {
                throw Util.newInternal(e, "Error while accessing rowset column " + name);
            } catch (IllegalAccessException e) {
                throw Util.newInternal(e, "Error while accessing rowset column " + name);
            }
        }

        /**
         * Retrieves the value of this column "MyColumn" by calling a method
         * called "getMyColumn()".
         *
         * @param row Current row
         * @return Value of given this property of the given row
         */
        protected final Object getFromAccessor(Object row) {
            try {
                String javaMethodName = "get" + name;
                Method method = row.getClass().getMethod(javaMethodName);
                return method.invoke(row);
            } catch (SecurityException e) {
                throw Util.newInternal(e, "Error while accessing rowset column " + name);
            } catch (IllegalAccessException e) {
                throw Util.newInternal(e, "Error while accessing rowset column " + name);
            } catch (NoSuchMethodException e) {
                throw Util.newInternal(e, "Error while accessing rowset column " + name);
            } catch (InvocationTargetException e) {
                throw Util.newInternal(e, "Error while accessing rowset column " + name);
            }
        }

        public String getColumnType() {
            if (type.isEnum()) {
                return enumeration.type.columnType;
            }
            return type.columnType;
        }
    }

    // -------------------------------------------------------------------------
    // From this point on, just rowset classess.

    static class DiscoverDatasourcesRowset extends Rowset {
        private static final Column DataSourceName = new Column("DataSourceName", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the data source, such as FoodMart 2000.");
        private static final Column DataSourceDescription = new Column("DataSourceDescription", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "A description of the data source, as entered by the " + "publisher.");
        private static final Column URL = new Column("URL", Type.String, null, Column.RESTRICTION, Column.OPTIONAL,
                "The unique path that shows where to invoke the XML for " + "Analysis methods for that data source.");
        private static final Column DataSourceInfo = new Column("DataSourceInfo", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "A string containing any additional information required to "
                        + "connect to the data source. This can include the Initial "
                        + "Catalog property or other information for the provider.\n"
                        + "Example: \"Provider=MSOLAP;Data Source=Local;\"");
        private static final Column ProviderName = new Column("ProviderName", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the provider behind the data source.\n" + "Example: \"MSDASQL\"");
        private static final Column ProviderType = new Column("ProviderType", Type.EnumerationArray,
                Enumeration.PROVIDER_TYPE, Column.RESTRICTION, Column.REQUIRED, Column.UNBOUNDED,
                "The types of data supported by the provider. May include one "
                        + "or more of the following types. Example follows this " + "table.\n"
                        + "TDP: tabular data provider.\n" + "MDP: multidimensional data provider.\n"
                        + "DMP: data mining provider. A DMP provider implements the "
                        + "OLE DB for Data Mining specification.");
        private static final Column AuthenticationMode = new Column("AuthenticationMode", Type.EnumString,
                Enumeration.AUTHENTICATION_MODE, Column.RESTRICTION, Column.REQUIRED,
                "Specification of what type of security mode the data source "
                        + "uses. Values can be one of the following:\n"
                        + "Unauthenticated: no user ID or password needs to be sent.\n"
                        + "Authenticated: User ID and Password must be included in the "
                        + "information required for the connection.\n"
                        + "Integrated: the data source uses the underlying security to "
                        + "determine authorization, such as Integrated Security "
                        + "provided by Microsoft Internet Information Services (IIS).");

        public DiscoverDatasourcesRowset(XmlaRequest request, XmlaHandler handler) {
            super(DISCOVER_DATASOURCES, request, handler);
        }

        private static final Column[] columns = {DataSourceName, DataSourceDescription, URL, DataSourceInfo,
                ProviderName, ProviderType, AuthenticationMode,};

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            if (needConnection()) {
                final XmlaExtra extra = handler.connectionFactory.getExtra();
                for (Map<String, Object> ds : extra.getDataSources(connection)) {
                    Row row = new Row();
                    for (Column column : columns) {
                        row.set(column.name, ds.get(column.name));
                    }
                    addRow(row, rows);
                }
            } else {
                // using pre-configured discover datasources response
                Row row = new Row();
                Map<String, Object> map = this.handler.connectionFactory.getPreConfiguredDiscoverDatasourcesResponse();
                for (Column column : columns) {
                    row.set(column.name, map.get(column.name));
                }
                addRow(row, rows);
            }
        }

        @Override
        protected boolean needConnection() {
            // If the olap connection factory has a pre configured response,
            // we don't need to connect to find metadata. This is good.
            return this.handler.connectionFactory.getPreConfiguredDiscoverDatasourcesResponse() == null;
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static class DiscoverSchemaRowsetsRowset extends Rowset {
        // Excel
        private static final Column RestrictionsMask = new Column("RestrictionsMask", Type.UnsignedLong, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "");
        private static final Column SchemaName = new Column("SchemaName", Type.StringArray, null, Column.RESTRICTION,
                Column.REQUIRED,
                "The name of the schema/request. This returns the values in "
                        + "the RequestTypes enumeration, plus any additional types "
                        + "supported by the provider. The provider defines rowset "
                        + "structures for the additional types");
        private static final Column SchemaGuid = new Column("SchemaGuid", Type.UUID, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The GUID of the schema.");
        private static final Column Restrictions = new Column("Restrictions", Type.Array, null, Column.NOT_RESTRICTION,
                Column.REQUIRED,
                "An array of the restrictions suppoted by provider. An example " + "follows this table.");
        private static final Column Description = new Column("Description", Type.String, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "A localizable description of the schema");

        public DiscoverSchemaRowsetsRowset(XmlaRequest request, XmlaHandler handler) {
            super(DISCOVER_SCHEMA_ROWSETS, request, handler);
        }

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException {
            RowsetDefinition[] rowsetDefinitions = RowsetDefinition.class.getEnumConstants().clone();
            if (!XmlaRequestContext.getContext().clientType.equals(XmlaRequestContext.ClientType.POWERBI_DESKTOP)) {
                rowsetDefinitions = (RowsetDefinition[]) ArrayUtils.removeElement(
                        ArrayUtils.removeElement(rowsetDefinitions, MDSCHEMA_KPIS),
                        DISCOVER_XML_METADATA);
            }

            Arrays.sort(rowsetDefinitions, new Comparator<RowsetDefinition>() {
                @Override
                public int compare(RowsetDefinition o1, RowsetDefinition o2) {
                    return o1.name().compareTo(o2.name());
                }
            });

            List<Object> schemaNames = new ArrayList<>();
            if (null != restrictions && restrictions.size() > 0 && restrictions.containsKey("SchemaName")) {
                Object objShecmaNames = restrictions.get("SchemaName");
                if (objShecmaNames instanceof Iterable) {
                    Iterator i = ((Iterable) objShecmaNames).iterator();
                    while (i.hasNext()) {
                        Object o = i.next();
                        schemaNames.add(o);
                    }
                } else {
                    schemaNames.add(objShecmaNames);
                }

            }

            for (RowsetDefinition rowsetDefinition : rowsetDefinitions) {
                if (schemaNames.size() > 0 && !schemaNames.contains(rowsetDefinition.name())) {
                    continue;
                }

                Row row = new Row();
                row.set(SchemaName.name, rowsetDefinition.name());

                // TODO: If we have a SchemaGuid output here
                row.set(SchemaGuid.name, rowsetDefinition.schemaGuid);

                row.set(Restrictions.name, getRestrictions(rowsetDefinition));

                String desc = rowsetDefinition.getDescription();
                row.set(Description.name, (desc == null) ? "" : desc);

                //Excel
                //row.set(RestrictionsMask.name, "" );
                addRow(row, rows);
            }
        }

        private List<XmlElement> getRestrictions(RowsetDefinition rowsetDefinition) {
            List<XmlElement> restrictionList = new ArrayList<XmlElement>();
            final Column[] columns = rowsetDefinition.columnDefinitions;
            for (Column column : columns) {
                if (column.restriction) {
                    restrictionList.add(new XmlElement(Restrictions.name, null,
                            new XmlElement[]{new XmlElement("Name", null, column.name),
                                    new XmlElement("Type", null, column.getColumnType())}));
                }
            }
            return restrictionList;
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    public String getDescription() {
        return description;
    }

    static class DiscoverPropertiesRowset extends Rowset {
        private final Util.Predicate1<PropertyDefinition> propNameCond;

        DiscoverPropertiesRowset(XmlaRequest request, XmlaHandler handler) {
            super(DISCOVER_PROPERTIES, request, handler);
            propNameCond = makeCondition(PROPDEF_NAME_GETTER, PropertyName);
        }

        private static final Column PropertyName = new Column("PropertyName", Type.StringSometimesArray, null,
                Column.RESTRICTION, Column.REQUIRED, "The name of the property.");
        private static final Column PropertyDescription = new Column("PropertyDescription", Type.String, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "A localizable text description of the property.");
        private static final Column PropertyType = new Column("PropertyType", Type.String, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "The XML data type of the property.");
        private static final Column PropertyAccessType = new Column("PropertyAccessType", Type.EnumString,
                Enumeration.ACCESS, Column.NOT_RESTRICTION, Column.REQUIRED,
                "Access for the property. The value can be Read, Write, or " + "ReadWrite.");
        private static final Column IsRequired = new Column("IsRequired", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "True if a property is required, false if it is not required.");
        private static final Column Value = new Column("Value", Type.String, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "The current value of the property.");

        /* to be able to call setCatalogPropertyValue() */
        protected boolean needConnection() {
            return true; //;
        }

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            for (PropertyDefinition propertyDefinition : PropertyDefinition.class.getEnumConstants()) {
                if (!XmlaRequestContext.getContext().clientType.equals(XmlaRequestContext.ClientType.POWERBI_DESKTOP) &&
                        propertyDefinition == PropertyDefinition.DBMSVersion) {
                    continue;
                }

                if (propertyDefinition.name().equals(PropertyDefinition.Catalog.name())) {
                    setCatalogPropertyValue(request, connection);
                }

                if (!propNameCond.test(propertyDefinition)) {
                    continue;
                }
                Row row = new Row();
                row.set(PropertyName.name, propertyDefinition.name());
                row.set(PropertyDescription.name, propertyDefinition.description);
                row.set(PropertyType.name, propertyDefinition.type.getName());
                row.set(PropertyAccessType.name, propertyDefinition.access);
                row.set(IsRequired.name, false);
                row.set(Value.name, extra.getPropertyValue(propertyDefinition));
                addRow(row, rows);
            }
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }

        private void setCatalogPropertyValue(XmlaRequest request, OlapConnection connection) {
            LOGGER.debug("setting property value");
            try {
                String currentCatalog = null;
                for (Map.Entry<String, String> property : request.getProperties().entrySet()) {
                    if (property.getKey().equals("Catalog")) {
                        currentCatalog = property.getValue();
                        break;
                    }
                }
                if (currentCatalog == null || currentCatalog.length() == 0) {
                    // set the first catalog in schema as initial catalog
                    List<Catalog> catalogs = connection.getOlapCatalogs();
                    if (catalogs.size() != 0) {
                        currentCatalog = catalogs.get(0).getName();
                    }
                }
                XmlaRequestContext context = XmlaRequestContext.getContext();
                context.currentCatalog = currentCatalog;

            } catch (OlapException e) {
                throw new RuntimeException("Failed to obtain a list of catalogs form the connection object.", e);
            }
        }
    }

    static class DiscoverEnumeratorsRowset extends Rowset {
        DiscoverEnumeratorsRowset(XmlaRequest request, XmlaHandler handler) {
            super(DISCOVER_ENUMERATORS, request, handler);
        }

        private static final Column EnumName = new Column("EnumName", Type.StringArray, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the enumerator that contains a set of values.");
        private static final Column EnumDescription = new Column("EnumDescription", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "A localizable description of the enumerator.");
        private static final Column EnumType = new Column("EnumType", Type.String, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "The data type of the Enum values.");
        private static final Column ElementName = new Column("ElementName", Type.String, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "The name of one of the value elements in the enumerator set.\n" + "Example: TDP");
        private static final Column ElementDescription = new Column("ElementDescription", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "A localizable description of the element (optional).");
        private static final Column ElementValue = new Column("ElementValue", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The value of the element.\n" + "Example: 01");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException {
            List<Enumeration> enumerators = getEnumerators();
            for (Enumeration enumerator : enumerators) {
                final List<? extends Enum> values = enumerator.getValues();
                for (Enum<?> value : values) {
                    Row row = new Row();
                    row.set(EnumName.name, enumerator.name);
                    row.set(EnumDescription.name, enumerator.description);

                    // Note: SQL Server always has EnumType string
                    // Need type of element of array, not the array
                    // it self.
                    row.set(EnumType.name, "string");

                    final String name = (value instanceof XmlaConstant) ? ((XmlaConstant) value).xmlaName()
                            : value.name();
                    row.set(ElementName.name, name);

                    final String description = (value instanceof XmlaConstant) ? ((XmlaConstant) value).getDescription()
                            : (value instanceof XmlaConstants.EnumWithDesc)
                            ? ((XmlaConstants.EnumWithDesc) value).getDescription()
                            : null;
                    if (description != null) {
                        row.set(ElementDescription.name, description);
                    }

                    switch (enumerator.type) {
                        case String:
                        case StringArray:
                            // these don't have ordinals
                            break;
                        default:
                            final int ordinal = (value instanceof XmlaConstant
                                    && ((XmlaConstant) value).xmlaOrdinal() != -1) ? ((XmlaConstant) value).xmlaOrdinal()
                                    : value.ordinal();
                            row.set(ElementValue.name, ordinal);
                            break;
                    }
                    addRow(row, rows);
                }
            }
        }

        private static List<Enumeration> getEnumerators() {
            // Build a set because we need to eliminate duplicates.
            SortedSet<Enumeration> enumeratorSet = new TreeSet<Enumeration>(new Comparator<Enumeration>() {
                public int compare(Enumeration o1, Enumeration o2) {
                    return o1.name.compareTo(o2.name);
                }
            });
            for (RowsetDefinition rowsetDefinition : RowsetDefinition.class.getEnumConstants()) {
                for (Column column : rowsetDefinition.columnDefinitions) {
                    if (column.enumeration != null) {
                        enumeratorSet.add(column.enumeration);
                    }
                }
            }
            return new ArrayList<Enumeration>(enumeratorSet);
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static class DiscoverKeywordsRowset extends Rowset {
        DiscoverKeywordsRowset(XmlaRequest request, XmlaHandler handler) {
            super(DISCOVER_KEYWORDS, request, handler);
        }

        private static final Column Keyword = new Column("Keyword", Type.StringSometimesArray, null, Column.RESTRICTION,
                Column.REQUIRED, "A list of all the keywords reserved by a provider.\n" + "Example: AND");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            for (String keyword : extra.getKeywords()) {
                Row row = new Row();
                row.set(Keyword.name, keyword);
                addRow(row, rows);
            }
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static class DiscoverLiteralsRowset extends Rowset {
        DiscoverLiteralsRowset(XmlaRequest request, XmlaHandler handler) {
            super(DISCOVER_LITERALS, request, handler);
        }

        private static final Column LiteralName = new Column("LiteralName", Type.StringSometimesArray, null,
                Column.RESTRICTION, Column.REQUIRED,
                "The name of the literal described in the row.\n" + "Example: DBLITERAL_LIKE_PERCENT");

        private static final Column LiteralValue = new Column("LiteralValue", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL,
                "Contains the actual literal value.\n" + "Example, if LiteralName is DBLITERAL_LIKE_PERCENT and the "
                        + "percent character (%) is used to match zero or more characters "
                        + "in a LIKE clause, this column's value would be \"%\".");

        private static final Column LiteralInvalidChars = new Column("LiteralInvalidChars", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "The characters, in the literal, that are not valid.\n"
                        + "For example, if table names can contain anything other than a "
                        + "numeric character, this string would be \"0123456789\".");

        private static final Column LiteralInvalidStartingChars = new Column("LiteralInvalidStartingChars", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL,
                "The characters that are not valid as the first character of the "
                        + "literal. If the literal can start with any valid character, " + "this is null.");

        private static final Column LiteralMaxLength = new Column("LiteralMaxLength", Type.Integer, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "The maximum number of characters in the literal. If there is no "
                        + "maximum or the maximum is unknown, the value is ?1.");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException {
            populate(XmlaConstants.Literal.class, rows, new Comparator<XmlaConstants.Literal>() {
                public int compare(XmlaConstants.Literal o1, XmlaConstants.Literal o2) {
                    return o1.name().compareTo(o2.name());
                }
            });
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    public static class DiscoverXmlMetadataRowset extends Rowset {
        DiscoverXmlMetadataRowset(XmlaRequest request, XmlaHandler handler) {
            super(DISCOVER_XML_METADATA, request, handler);
        }

        private static final Column Metadata = new Column("METADATA", Type.XmlDocument, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "An XML document that describes the object requested by the restriction.");

        private static final Column ObjectExpansion = new Column("ObjectExpansion", Type.String, null,
                Column.RESTRICTION, Column.OPTIONAL, "The degree of expansion in the result.");

        @Override
        protected void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows) throws XmlaException, SQLException {
            String metadata;
            try {
                metadata = IOUtils.toString(this.getClass().getClassLoader().getResource(XML_METADATA_FILE));
            } catch (IOException ioe) {
                throw new SQLException(String.format("Reading xml metadata file %s error.", XML_METADATA_FILE));
            }

            Row metadataRow = new Row();
            metadataRow.set("METADATA", new PlainText(metadata));
            addRow(metadataRow, rows);
        }

        public Column[] outputColumns = new Column[]{Metadata};
    }

    static class DbschemaCatalogsRowset extends Rowset {
        private final Util.Predicate1<Catalog> catalogNameCond;

        DbschemaCatalogsRowset(XmlaRequest request, XmlaHandler handler) {
            super(DBSCHEMA_CATALOGS, request, handler);
            catalogNameCond = Util.truePredicate1();
        }

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "Catalog name. Cannot be NULL.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "Human-readable description of the catalog.");
        private static final Column Roles = new Column("ROLES", Type.String, null, Column.NOT_RESTRICTION,
                Column.REQUIRED,
                "A comma delimited list of roles to which the current user "
                        + "belongs. An asterisk (*) is included as a role if the "
                        + "current user is a server or database administrator. "
                        + "Username is appended to ROLES if one of the roles uses " + "dynamic security.");
        private static final Column DateModified = new Column("DATE_MODIFIED", Type.DateTime, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "The date that the catalog was last modified.");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            for (Catalog catalog : catIter(connection, catalogNameCond, catalogNameCond)) {
                for (Schema schema : catalog.getSchemas()) {
                    Row row = new Row();
                    row.set(CatalogName.name, catalog.getName());

                    // TODO: currently schema grammar does not support a
                    // description
                    row.set(Description.name, "No description available");

                    // get Role names
                    StringBuilder buf = new StringBuilder(100);
                    final List<String> roleNames = extra.getSchemaRoleNames(schema);
                    serialize(buf, roleNames);
                    row.set(Roles.name, buf.toString());

                    // TODO: currently schema grammar does not support modify
                    // date so we return just some date for now.
                    if (false) {
                        row.set(DateModified.name, dateModified);
                    }
                    addRow(row, rows);
                }
            }
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static class DbschemaColumnsRowset extends Rowset {
        private final Util.Predicate1<Catalog> tableCatalogCond;
        private final Util.Predicate1<Cube> tableNameCond;
        private final Util.Predicate1<String> columnNameCond;

        DbschemaColumnsRowset(XmlaRequest request, XmlaHandler handler) {
            super(DBSCHEMA_COLUMNS, request, handler);
            tableCatalogCond = makeCondition(CATALOG_NAME_GETTER, TableCatalog);
            tableNameCond = makeCondition(ELEMENT_NAME_GETTER, TableName);
            columnNameCond = makeCondition(ColumnName);
        }

        private static final Column TableCatalog = new Column("TABLE_CATALOG", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the Database.");
        private static final Column TableSchema = new Column("TABLE_SCHEMA", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, null);
        private static final Column TableName = new Column("TABLE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the cube.");
        private static final Column ColumnName = new Column("COLUMN_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the attribute hierarchy or measure.");
        private static final Column OrdinalPosition = new Column("ORDINAL_POSITION", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "The position of the column, beginning with 1.");
        private static final Column ColumnHasDefault = new Column("COLUMN_HAS_DEFAULT", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Not supported.");

        //  A bitmask indicating the information stored in
        //      DBCOLUMNFLAGS in OLE DB.
        //  1 = Bookmark
        //  2 = Fixed length
        //  4 = Nullable
        //  8 = Row versioning
        //  16 = Updateable column
        //
        // And, of course, MS SQL Server sometimes has the value of 80!!
        private static final Column ColumnFlags = new Column("COLUMN_FLAGS", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "A DBCOLUMNFLAGS bitmask indicating column properties.");
        private static final Column IsNullable = new Column("IS_NULLABLE", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "Always returns false.");
        private static final Column DataType = new Column("DATA_TYPE", Type.UnsignedShort, null, Column.NOT_RESTRICTION,
                Column.REQUIRED,
                "The data type of the column. Returns a string for dimension " + "columns and a variant for measures.");
        private static final Column CharacterMaximumLength = new Column("CHARACTER_MAXIMUM_LENGTH",
                Type.UnsignedInteger, null, Column.NOT_RESTRICTION, Column.OPTIONAL,
                "The maximum possible length of a value within the column.");
        private static final Column CharacterOctetLength = new Column("CHARACTER_OCTET_LENGTH", Type.UnsignedInteger,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL,
                "The maximum possible length of a value within the column, in "
                        + "bytes, for character or binary columns.");
        private static final Column NumericPrecision = new Column("NUMERIC_PRECISION", Type.UnsignedShort, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "The maximum precision of the column for numeric data types " + "other than DBTYPE_VARNUMERIC.");
        private static final Column NumericScale = new Column("NUMERIC_SCALE", Type.Short, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The number of digits to the right of the decimal point for "
                + "DBTYPE_DECIMAL, DBTYPE_NUMERIC, DBTYPE_VARNUMERIC. " + "Otherwise, this is NULL.");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, OlapException {
            for (Catalog catalog : catIter(connection, tableCatalogCond, catNameCond())) {
                // By definition, mondrian catalogs have only one
                // schema. It is safe to use get(0)
                final Schema schema = catalog.getSchemas().get(0);
                final boolean emitInvisibleMembers = XmlaUtil.shouldEmitInvisibleMembers(request);
                int ordinalPosition = 1;
                Row row;

                for (Cube cube : filter(sortedCubes(schema), tableNameCond)) {
                    for (Dimension dimension : cube.getDimensions()) {
                        for (Hierarchy hierarchy : dimension.getHierarchies()) {
                            ordinalPosition = populateHierarchy(cube, hierarchy, ordinalPosition, rows);
                        }
                    }

                    List<Measure> rms = cube.getMeasures();
                    for (int k = 1; k < rms.size(); k++) {
                        Measure member = rms.get(k);

                        // null == true for regular cubes
                        // virtual cubes do not set the visible property
                        // on its measures so it might be null.
                        Boolean visible = (Boolean) member.getPropertyValue(Property.StandardMemberProperty.$visible);
                        if (visible == null) {
                            visible = true;
                        }
                        if (!emitInvisibleMembers && !visible) {
                            continue;
                        }

                        String memberName = member.getName();
                        final String columnName = "Measures:" + memberName;
                        if (!columnNameCond.test(columnName)) {
                            continue;
                        }

                        row = new Row();
                        row.set(TableCatalog.name, catalog.getName());
                        row.set(TableName.name, cube.getName());
                        row.set(ColumnName.name, columnName);
                        row.set(OrdinalPosition.name, ordinalPosition++);
                        row.set(ColumnHasDefault.name, false);
                        row.set(ColumnFlags.name, 0);
                        row.set(IsNullable.name, false);
                        // TODO: here is where one tries to determine the
                        // type of the column - since these are all
                        // Measures, aggregate Measures??, maybe they
                        // are all numeric? (or currency)
                        row.set(DataType.name, XmlaConstants.DBType.R8.xmlaOrdinal());
                        // TODO: 16/255 seems to be what MS SQL Server
                        // always returns.
                        row.set(NumericPrecision.name, 16);
                        row.set(NumericScale.name, 255);
                        addRow(row, rows);
                    }
                }
            }
        }

        private int populateHierarchy(Cube cube, Hierarchy hierarchy, int ordinalPosition, List<Row> rows) {
            String schemaName = cube.getSchema().getName();
            String cubeName = cube.getName();
            String hierarchyName = hierarchy.getName();

            if (hierarchy.hasAll()) {
                Row row = new Row();
                row.set(TableCatalog.name, schemaName);
                row.set(TableName.name, cubeName);
                row.set(ColumnName.name, hierarchyName + ":(All)!NAME");
                row.set(OrdinalPosition.name, ordinalPosition++);
                row.set(ColumnHasDefault.name, false);
                row.set(ColumnFlags.name, 0);
                row.set(IsNullable.name, false);
                // names are always WSTR
                row.set(DataType.name, XmlaConstants.DBType.WSTR.xmlaOrdinal());
                row.set(CharacterMaximumLength.name, 0);
                row.set(CharacterOctetLength.name, 0);
                addRow(row, rows);

                row = new Row();
                row.set(TableCatalog.name, schemaName);
                row.set(TableName.name, cubeName);
                row.set(ColumnName.name, hierarchyName + ":(All)!UNIQUE_NAME");
                row.set(OrdinalPosition.name, ordinalPosition++);
                row.set(ColumnHasDefault.name, false);
                row.set(ColumnFlags.name, 0);
                row.set(IsNullable.name, false);
                // names are always WSTR
                row.set(DataType.name, XmlaConstants.DBType.WSTR.xmlaOrdinal());
                row.set(CharacterMaximumLength.name, 0);
                row.set(CharacterOctetLength.name, 0);
                addRow(row, rows);

                if (false) {
                    // TODO: SQLServer outputs this hasall KEY column name -
                    // don't know what it's for
                    row = new Row();
                    row.set(TableCatalog.name, schemaName);
                    row.set(TableName.name, cubeName);
                    row.set(ColumnName.name, hierarchyName + ":(All)!KEY");
                    row.set(OrdinalPosition.name, ordinalPosition++);
                    row.set(ColumnHasDefault.name, false);
                    row.set(ColumnFlags.name, 0);
                    row.set(IsNullable.name, false);
                    // names are always BOOL
                    row.set(DataType.name, XmlaConstants.DBType.BOOL.xmlaOrdinal());
                    row.set(NumericPrecision.name, 255);
                    row.set(NumericScale.name, 255);
                    addRow(row, rows);
                }
            }

            for (Level level : hierarchy.getLevels()) {
                ordinalPosition = populateLevel(cube, hierarchy, level, ordinalPosition, rows);
            }
            return ordinalPosition;
        }

        private int populateLevel(Cube cube, Hierarchy hierarchy, Level level, int ordinalPosition, List<Row> rows) {
            String schemaName = cube.getSchema().getName();
            String cubeName = cube.getName();
            String hierarchyName = hierarchy.getName();
            String levelName = level.getName();

            Row row = new Row();
            row.set(TableCatalog.name, schemaName);
            row.set(TableName.name, cubeName);
            row.set(ColumnName.name, hierarchyName + ':' + levelName + "!NAME");
            row.set(OrdinalPosition.name, ordinalPosition++);
            row.set(ColumnHasDefault.name, false);
            row.set(ColumnFlags.name, 0);
            row.set(IsNullable.name, false);
            // names are always WSTR
            row.set(DataType.name, XmlaConstants.DBType.WSTR.xmlaOrdinal());
            row.set(CharacterMaximumLength.name, 0);
            row.set(CharacterOctetLength.name, 0);
            addRow(row, rows);

            row = new Row();
            row.set(TableCatalog.name, schemaName);
            row.set(TableName.name, cubeName);
            row.set(ColumnName.name, hierarchyName + ':' + levelName + "!UNIQUE_NAME");
            row.set(OrdinalPosition.name, ordinalPosition++);
            row.set(ColumnHasDefault.name, false);
            row.set(ColumnFlags.name, 0);
            row.set(IsNullable.name, false);
            // names are always WSTR
            row.set(DataType.name, XmlaConstants.DBType.WSTR.xmlaOrdinal());
            row.set(CharacterMaximumLength.name, 0);
            row.set(CharacterOctetLength.name, 0);
            addRow(row, rows);

            /*
            TODO: see above
            row = new Row();
            row.set(TableCatalog.name, schemaName);
            row.set(TableName.name, cubeName);
            row.set(ColumnName.name,
                hierarchyName + ":" + levelName + "!KEY");
            row.set(OrdinalPosition.name, ordinalPosition++);
            row.set(ColumnHasDefault.name, false);
            row.set(ColumnFlags.name, 0);
            row.set(IsNullable.name, false);
            // names are always BOOL
            row.set(DataType.name, DBType.BOOL.ordinal());
            row.set(NumericPrecision.name, 255);
            row.set(NumericScale.name, 255);
            addRow(row, rows);
            */
            NamedList<Property> props = level.getProperties();
            for (Property prop : props) {
                String propName = prop.getName();

                row = new Row();
                row.set(TableCatalog.name, schemaName);
                row.set(TableName.name, cubeName);
                row.set(ColumnName.name, hierarchyName + ':' + levelName + '!' + propName);
                row.set(OrdinalPosition.name, ordinalPosition++);
                row.set(ColumnHasDefault.name, false);
                row.set(ColumnFlags.name, 0);
                row.set(IsNullable.name, false);

                XmlaConstants.DBType dbType = getDBTypeFromProperty(prop);
                row.set(DataType.name, dbType.xmlaOrdinal());

                switch (prop.getDatatype()) {
                    case STRING:
                        row.set(CharacterMaximumLength.name, 0);
                        row.set(CharacterOctetLength.name, 0);
                        break;
                    case INTEGER:
                    case UNSIGNED_INTEGER:
                    case DOUBLE:
                        // TODO: 16/255 seems to be what MS SQL Server
                        // always returns.
                        row.set(NumericPrecision.name, 16);
                        row.set(NumericScale.name, 255);
                        break;
                    case BOOLEAN:
                        row.set(NumericPrecision.name, 255);
                        row.set(NumericScale.name, 255);
                        break;
                    default:
                        // TODO: what type is it really, its
                        // not a string
                        row.set(CharacterMaximumLength.name, 0);
                        row.set(CharacterOctetLength.name, 0);
                        break;
                }
                addRow(row, rows);
            }
            return ordinalPosition;
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static class DbschemaProviderTypesRowset extends Rowset {
        private final Util.Predicate1<Integer> dataTypeCond;

        DbschemaProviderTypesRowset(XmlaRequest request, XmlaHandler handler) {
            super(DBSCHEMA_PROVIDER_TYPES, request, handler);
            dataTypeCond = makeCondition(DataType);
        }

        private static final Column TypeName = new Column("TYPE_NAME", Type.String, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "The provider-specific data type name.");
        private static final Column DataType = new Column("DATA_TYPE", Type.UnsignedShort, null, Column.RESTRICTION,
                Column.REQUIRED, "The indicator of the data type.");
        private static final Column ColumnSize = new Column("COLUMN_SIZE", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "The length of a non-numeric column. If the data type is "
                + "numeric, this is the upper bound on the maximum precision " + "of the data type.");
        private static final Column LiteralPrefix = new Column("LITERAL_PREFIX", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "The character or characters used to prefix a literal of this " + "type in a text command.");
        private static final Column LiteralSuffix = new Column("LITERAL_SUFFIX", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "The character or characters used to suffix a literal of this " + "type in a text command.");
        private static final Column IsNullable = new Column("IS_NULLABLE", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A Boolean that indicates whether the data type is nullable. "
                + "NULL-- indicates that it is not known whether the data type " + "is nullable.");
        private static final Column CaseSensitive = new Column("CASE_SENSITIVE", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "A Boolean that indicates whether the data type is a " + "characters type and case-sensitive.");
        private static final Column Searchable = new Column("SEARCHABLE", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "An integer indicating how the data type can be used in "
                + "searches if the provider supports ICommandText; otherwise, " + "NULL.");
        private static final Column UnsignedAttribute = new Column("UNSIGNED_ATTRIBUTE", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "A Boolean that indicates whether the data type is unsigned.");
        private static final Column FixedPrecScale = new Column("FIXED_PREC_SCALE", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "A Boolean that indicates whether the data type has a fixed " + "precision and scale.");
        private static final Column AutoUniqueValue = new Column("AUTO_UNIQUE_VALUE", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "A Boolean that indicates whether the data type is " + "autoincrementing.");
        private static final Column IsLong = new Column("IS_LONG", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A Boolean that indicates whether the data type is a binary "
                + "large object (BLOB) and has very long data.");
        private static final Column BestMatch = new Column("BEST_MATCH", Type.Boolean, null, Column.RESTRICTION,
                Column.OPTIONAL, "A Boolean that indicates whether the data type is a best " + "match.");

        @Override
        protected boolean needConnection() {
            return false;
        }

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException {
            // Identifies the (base) data types supported by the data provider.
            Row row;

            // i4
            Integer dt = XmlaConstants.DBType.I4.xmlaOrdinal();
            if (dataTypeCond.test(dt)) {
                row = new Row();
                row.set(TypeName.name, XmlaConstants.DBType.I4.userName);
                row.set(DataType.name, dt);
                row.set(ColumnSize.name, 8);
                row.set(IsNullable.name, true);
                row.set(Searchable.name, null);
                row.set(UnsignedAttribute.name, false);
                row.set(FixedPrecScale.name, false);
                row.set(AutoUniqueValue.name, false);
                row.set(IsLong.name, false);
                row.set(BestMatch.name, true);
                addRow(row, rows);
            }

            // R8
            dt = XmlaConstants.DBType.R8.xmlaOrdinal();
            if (dataTypeCond.test(dt)) {
                row = new Row();
                row.set(TypeName.name, XmlaConstants.DBType.R8.userName);
                row.set(DataType.name, dt);
                row.set(ColumnSize.name, 16);
                row.set(IsNullable.name, true);
                row.set(Searchable.name, null);
                row.set(UnsignedAttribute.name, false);
                row.set(FixedPrecScale.name, false);
                row.set(AutoUniqueValue.name, false);
                row.set(IsLong.name, false);
                row.set(BestMatch.name, true);
                addRow(row, rows);
            }

            // CY
            dt = XmlaConstants.DBType.CY.xmlaOrdinal();
            if (dataTypeCond.test(dt)) {
                row = new Row();
                row.set(TypeName.name, XmlaConstants.DBType.CY.userName);
                row.set(DataType.name, dt);
                row.set(ColumnSize.name, 8);
                row.set(IsNullable.name, true);
                row.set(Searchable.name, null);
                row.set(UnsignedAttribute.name, false);
                row.set(FixedPrecScale.name, false);
                row.set(AutoUniqueValue.name, false);
                row.set(IsLong.name, false);
                row.set(BestMatch.name, true);
                addRow(row, rows);
            }

            // BOOL
            dt = XmlaConstants.DBType.BOOL.xmlaOrdinal();
            if (dataTypeCond.test(dt)) {
                row = new Row();
                row.set(TypeName.name, XmlaConstants.DBType.BOOL.userName);
                row.set(DataType.name, dt);
                row.set(ColumnSize.name, 1);
                row.set(IsNullable.name, true);
                row.set(Searchable.name, null);
                row.set(UnsignedAttribute.name, false);
                row.set(FixedPrecScale.name, false);
                row.set(AutoUniqueValue.name, false);
                row.set(IsLong.name, false);
                row.set(BestMatch.name, true);
                addRow(row, rows);
            }

            // I8
            dt = XmlaConstants.DBType.I8.xmlaOrdinal();
            if (dataTypeCond.test(dt)) {
                row = new Row();
                row.set(TypeName.name, XmlaConstants.DBType.I8.userName);
                row.set(DataType.name, dt);
                row.set(ColumnSize.name, 16);
                row.set(IsNullable.name, true);
                row.set(Searchable.name, null);
                row.set(UnsignedAttribute.name, false);
                row.set(FixedPrecScale.name, false);
                row.set(AutoUniqueValue.name, false);
                row.set(IsLong.name, false);
                row.set(BestMatch.name, true);
                addRow(row, rows);
            }

            // WSTR
            dt = XmlaConstants.DBType.WSTR.xmlaOrdinal();
            if (dataTypeCond.test(dt)) {
                row = new Row();
                row.set(TypeName.name, XmlaConstants.DBType.WSTR.userName);
                row.set(DataType.name, dt);
                // how big are the string columns in the db
                row.set(ColumnSize.name, 255);
                row.set(LiteralPrefix.name, "\"");
                row.set(LiteralSuffix.name, "\"");
                row.set(IsNullable.name, true);
                row.set(CaseSensitive.name, false);
                row.set(Searchable.name, null);
                row.set(FixedPrecScale.name, false);
                row.set(AutoUniqueValue.name, false);
                row.set(IsLong.name, false);
                row.set(BestMatch.name, true);
                addRow(row, rows);
            }
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    /**
     * static class DbschemaSchemataRowset extends Rowset {
     * private final Util.Predicate1<Catalog> catalogNameCond;
     * <p>
     * DbschemaSchemataRowset(XmlaRequest request, XmlaHandler handler) {
     * super(DBSCHEMA_SCHEMATA, request, handler);
     * catalogNameCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
     * }
     * <p>
     * private static final Column CatalogName =
     * new Column(
     * "CATALOG_NAME",
     * Type.String,
     * null,
     * Column.RESTRICTION,
     * Column.REQUIRED,
     * "The provider-specific data type name.");
     * private static final Column SchemaName =
     * new Column(
     * "SCHEMA_NAME",
     * Type.String,
     * null,
     * Column.RESTRICTION,
     * Column.REQUIRED,
     * "The indicator of the data type.");
     * private static final Column SchemaOwner =
     * new Column(
     * "SCHEMA_OWNER",
     * Type.String,
     * null,
     * Column.RESTRICTION,
     * Column.REQUIRED,
     * "The length of a non-numeric column. If the data type is "
     * + "numeric, this is the upper bound on the maximum precision "
     * + "of the data type.");
     * <p>
     * public void populateImpl(
     * XmlaResponse response,
     * OlapConnection connection,
     * List<Row> rows)
     * throws XmlaException, OlapException
     * {
     * for (Catalog catalog
     * : catIter(connection, catalogNameCond, catNameCond()))
     * {
     * for (Schema schema : catalog.getSchemas()) {
     * Row row = new Row();
     * row.set(CatalogName.name, catalog.getName());
     * row.set(SchemaName.name, schema.getName());
     * row.set(SchemaOwner.name, "");
     * addRow(row, rows);
     * }
     * }
     * }
     * <p>
     * protected void setProperty(
     * PropertyDefinition propertyDef, String value)
     * {
     * switch (propertyDef) {
     * case Content:
     * break;
     * default:
     * super.setProperty(propertyDef, value);
     * }
     * }
     * }
     */
    static class DbschemaTablesRowset extends Rowset {
        private final Util.Predicate1<Catalog> tableCatalogCond;
        private final Util.Predicate1<Cube> tableNameCond;
        private final Util.Predicate1<String> tableTypeCond;

        DbschemaTablesRowset(XmlaRequest request, XmlaHandler handler) {
            super(DBSCHEMA_TABLES, request, handler);
            tableCatalogCond = makeCondition(CATALOG_NAME_GETTER, TableCatalog);
            tableNameCond = makeCondition(ELEMENT_NAME_GETTER, TableName);
            tableTypeCond = makeCondition(TableType);
        }

        private static final Column TableCatalog = new Column("TABLE_CATALOG", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the catalog to which this object belongs.");
        private static final Column TableSchema = new Column("TABLE_SCHEMA", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the cube to which this object belongs.");
        private static final Column TableName = new Column("TABLE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the object, if TABLE_TYPE is TABLE.");
        private static final Column TableType = new Column("TABLE_TYPE", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The type of the table. TABLE indicates the object is a "
                + "measure group. SYSTEM TABLE indicates the object is a " + "dimension.");

        private static final Column TableGuid = new Column("TABLE_GUID", Type.UUID, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Not supported.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A human-readable description of the object.");
        private static final Column TablePropId = new Column("TABLE_PROPID", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Not supported.");
        private static final Column DateCreated = new Column("DATE_CREATED", Type.DateTime, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Not supported.");
        private static final Column DateModified = new Column("DATE_MODIFIED", Type.DateTime, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "The date the object was last modified.");
        /*
        private static final Column TableOlapType =
            new Column(
                "TABLE_OLAP_TYPE",
                Type.String,
                null,
                Column.RESTRICTION,
                Column.OPTIONAL,
                "The OLAP type of the object.  MEASURE_GROUP indicates the "
                + "object is a measure group.  CUBE_DIMENSION indicated the "
                + "object is a dimension.");
        */

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, OlapException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            for (Catalog catalog : catIter(connection, catNameCond(), tableCatalogCond)) {
                // By definition, mondrian catalogs have only one
                // schema. It is safe to use get(0)
                final Schema schema = catalog.getSchemas().get(0);
                Row row;
                for (Cube cube : filter(sortedCubes(schema), tableNameCond)) {
                    String desc = cube.getDescription();
                    if (desc == null) {
                        //TODO: currently this is always null
                        desc = catalog.getName() + " - " + cube.getName() + " Cube";
                    }

                    if (tableTypeCond.test("TABLE")) {
                        row = new Row();
                        row.set(TableCatalog.name, catalog.getName());
                        row.set(TableName.name, cube.getName());
                        row.set(TableType.name, "TABLE");
                        row.set(Description.name, desc);
                        if (false) {
                            row.set(DateModified.name, dateModified);
                        }
                        addRow(row, rows);
                    }

                    if (tableTypeCond.test("SYSTEM TABLE")) {
                        for (Dimension dimension : cube.getDimensions()) {
                            if (dimension.getDimensionType() == Dimension.Type.MEASURE) {
                                continue;
                            }
                            for (Hierarchy hierarchy : dimension.getHierarchies()) {
                                populateHierarchy(extra, cube, hierarchy, rows);
                            }
                        }
                    }
                }
            }
        }

        private void populateHierarchy(XmlaExtra extra, Cube cube, Hierarchy hierarchy, List<Row> rows) {
            if (hierarchy.getName().endsWith("$Parent")) {
                // We don't return generated Parent-Child
                // hierarchies.
                return;
            }
            for (Level level : hierarchy.getLevels()) {
                populateLevel(extra, cube, hierarchy, level, rows);
            }
        }

        private void populateLevel(XmlaExtra extra, Cube cube, Hierarchy hierarchy, Level level,
                                   List<Row> rows) {
            String schemaName = cube.getSchema().getName();
            String cubeName = cube.getName();
            String hierarchyName = extra.getHierarchyName(hierarchy);
            String levelName = level.getName();

            String tableName = cubeName + ':' + hierarchyName + ':' + levelName;

            String desc = level.getDescription();
            if (desc == null) {
                //TODO: currently this is always null
                desc = schemaName + " - " + cubeName + " Cube - " + hierarchyName + " Hierarchy - " + levelName
                        + " Level";
            }

            Row row = new Row();
            row.set(TableCatalog.name, schemaName);
            row.set(TableName.name, tableName);
            row.set(TableType.name, "SYSTEM TABLE");
            row.set(Description.name, desc);
            if (false) {
                row.set(DateModified.name, dateModified);
            }
            addRow(row, rows);
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    // TODO: Is this needed????
    static class DbschemaTablesInfoRowset extends Rowset {
        DbschemaTablesInfoRowset(XmlaRequest request, XmlaHandler handler) {
            super(DBSCHEMA_TABLES_INFO, request, handler);
        }

        private static final Column TableCatalog = new Column("TABLE_CATALOG", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "Catalog name. NULL if the provider does not support " + "catalogs.");
        private static final Column TableSchema = new Column("TABLE_SCHEMA", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "Unqualified schema name. NULL if the provider does not " + "support schemas.");
        private static final Column TableName = new Column("TABLE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "Table name.");
        private static final Column TableType = new Column("TABLE_TYPE", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED,
                "Table type. One of the following or a provider-specific "
                        + "value: ALIAS, TABLE, SYNONYM, SYSTEM TABLE, VIEW, GLOBAL "
                        + "TEMPORARY, LOCAL TEMPORARY, EXTERNAL TABLE, SYSTEM VIEW");
        private static final Column TableGuid = new Column("TABLE_GUID", Type.UUID, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "GUID that uniquely identifies the table. Providers that do "
                + "not use GUIDs to identify tables should return NULL in this " + "column.");

        private static final Column Bookmarks = new Column("BOOKMARKS", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "Whether this table supports bookmarks. Allways is false.");
        private static final Column BookmarkType = new Column("BOOKMARK_TYPE", Type.Integer, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Default bookmark type supported on this table.");
        private static final Column BookmarkDataType = new Column("BOOKMARK_DATATYPE", Type.UnsignedShort, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "The indicator of the bookmark's native data type.");
        private static final Column BookmarkMaximumLength = new Column("BOOKMARK_MAXIMUM_LENGTH", Type.UnsignedInteger,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "Maximum length of the bookmark in bytes.");
        private static final Column BookmarkInformation = new Column("BOOKMARK_INFORMATION", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL,
                "A bitmask specifying additional information about bookmarks " + "over the rowset. ");
        private static final Column TableVersion = new Column("TABLE_VERSION", Type.Long, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Version number for this table or NULL if the provider does "
                + "not support returning table version information.");
        private static final Column Cardinality = new Column("CARDINALITY", Type.UnsignedLong, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "Cardinality (number of rows) of the table.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Human-readable description of the table.");
        private static final Column TablePropId = new Column("TABLE_PROPID", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Property ID of the table. Return null.");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, OlapException {
            for (Catalog catalog : catIter(connection, catNameCond())) {
                // By definition, mondrian catalogs have only one
                // schema. It is safe to use get(0)
                final Schema schema = catalog.getSchemas().get(0);
                //TODO: Is this cubes or tables? SQL Server returns what
                // in foodmart are cube names for TABLE_NAME
                for (Cube cube : sortedCubes(schema)) {
                    String cubeName = cube.getName();
                    String desc = cube.getDescription();
                    if (desc == null) {
                        //TODO: currently this is always null
                        desc = catalog.getName() + " - " + cubeName + " Cube";
                    }
                    //TODO: SQL Server returns 1000000 for all tables
                    int cardinality = 1000000;
                    String version = "null";

                    Row row = new Row();
                    row.set(TableCatalog.name, catalog.getName());
                    row.set(TableName.name, cubeName);
                    row.set(TableType.name, "TABLE");
                    row.set(Bookmarks.name, false);
                    row.set(TableVersion.name, version);
                    row.set(Cardinality.name, cardinality);
                    row.set(Description.name, desc);
                    addRow(row, rows);
                }
            }
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static class MdschemaActionsRowset extends Rowset {
        MdschemaActionsRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_ACTIONS, request, handler);
        }

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the catalog to which this action belongs.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the schema to which this action belongs.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the cube to which this action belongs.");
        private static final Column ActionName = new Column("ACTION_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the action.");
        private static final Column Coordinate = new Column("COORDINATE", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, null);
        private static final Column CoordinateType = new Column("COORDINATE_TYPE", Type.Integer, null,
                Column.RESTRICTION, Column.REQUIRED, null);

        // TODO: optional columns:
        // ACTION_TYPE
        // INVOCATION
        // CUBE_SOURCE

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException {
            // mondrian doesn't support actions. It's not an error to ask for
            // them, there just aren't any
        }
    }

    public static class MdschemaCubesRowset extends Rowset {
        private final Util.Predicate1<Catalog> catalogNameCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;

        MdschemaCubesRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_CUBES, request, handler);
            catalogNameCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
        }

        public static final String MD_CUBTYPE_CUBE = "CUBE";
        public static final String MD_CUBTYPE_VIRTUAL_CUBE = "VIRTUAL CUBE";

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the catalog to which this cube belongs.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the schema to which this cube belongs.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "Name of the cube.");
        private static final Column CubeType = new Column("CUBE_TYPE", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "Cube type.");
        private static final Column CubeSource = new Column("CUBE_SOURCE", Type.UnsignedShort, null, Column.RESTRICTION,
                Column.OPTIONAL, "Cube Source.");
        private static final Column CubeGuid = new Column("CUBE_GUID", Type.UUID, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Cube GUID.");
        private static final Column CreatedOn = new Column("CREATED_ON", Type.DateTime, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Date and time of cube creation.");
        private static final Column LastSchemaUpdate = new Column("LAST_SCHEMA_UPDATE", Type.DateTime, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Date and time of last schema update.");
        private static final Column SchemaUpdatedBy = new Column("SCHEMA_UPDATED_BY", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "User ID of the person who last updated the schema.");
        private static final Column LastDataUpdate = new Column("LAST_DATA_UPDATE", Type.DateTime, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Date and time of last data update.");
        private static final Column DataUpdatedBy = new Column("DATA_UPDATED_BY", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "User ID of the person who last updated the data.");
        private static final Column IsDrillthroughEnabled = new Column("IS_DRILLTHROUGH_ENABLED", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.REQUIRED,
                "Describes whether DRILLTHROUGH can be performed on the " + "members of a cube");
        private static final Column IsWriteEnabled = new Column("IS_WRITE_ENABLED", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "Describes whether a cube is write-enabled");
        private static final Column IsLinkable = new Column("IS_LINKABLE", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "Describes whether a cube can be used in a linked cube");
        private static final Column IsSqlEnabled = new Column("IS_SQL_ENABLED", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "Describes whether or not SQL can be used on the cube");
        private static final Column CubeCaption = new Column("CUBE_CAPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The caption of the cube.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A user-friendly description of the dimension.");
        private static final Column Dimensions = new Column("DIMENSIONS", Type.Rowset, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Dimensions in this cube.");
        private static final Column Sets = new Column("SETS", Type.Rowset, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Sets in this cube.");
        private static final Column Measures = new Column("MEASURES", Type.Rowset, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Measures in this cube.");
        private static final Column BaseCubeName = new Column("BASE_CUBE_NAME", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The parent cube name.");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            for (Catalog catalog : catIter(connection, catNameCond(), catalogNameCond)) {
                for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                    for (Cube cube : filter(sortedCubes(schema), cubeNameCond)) {
                        String desc = cube.getDescription();
                        if (desc == null) {
                            desc = catalog.getName() + " Schema - " + cube.getName() + " Cube";
                        }

                        Row row = new Row();
                        row.set(CatalogName.name, catalog.getName());
                        row.set(SchemaName.name, schema.getName());
                        row.set(CubeName.name, cube.getName());
                        row.set(CubeType.name, extra.getCubeType(cube));
                        //row.set(CubeGuid.name, "");
                        //row.set(CreatedOn.name, "");
                        //row.set(LastSchemaUpdate.name, "");
                        //row.set(SchemaUpdatedBy.name, "");
                        //row.set(LastDataUpdate.name, "");
                        //row.set(DataUpdatedBy.name, "");
                        row.set(IsDrillthroughEnabled.name, true);
                        row.set(IsWriteEnabled.name, false);
                        row.set(IsLinkable.name, false);
                        row.set(IsSqlEnabled.name, false);
                        row.set(CubeCaption.name, cube.getCaption());
                        row.set(Description.name, desc);
                        Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        String formattedDate = formatter.format(extra.getSchemaLoadDate(schema));
                        row.set(LastSchemaUpdate.name, formattedDate);
                        if (deep) {
                            row.set(Dimensions.name,
                                    new MdschemaDimensionsRowset(wrapRequest(request,
                                            Olap4jUtil.mapOf(MdschemaDimensionsRowset.CatalogName, catalog.getName(),
                                                    MdschemaDimensionsRowset.SchemaName, schema.getName(),
                                                    MdschemaDimensionsRowset.CubeName, cube.getName())),
                                            handler));
                            row.set(Sets.name,
                                    new MdschemaSetsRowset(wrapRequest(request,
                                            Olap4jUtil.mapOf(MdschemaSetsRowset.CatalogName, catalog.getName(),
                                                    MdschemaSetsRowset.SchemaName, schema.getName(),
                                                    MdschemaSetsRowset.CubeName, cube.getName())),
                                            handler));
                            row.set(Measures.name,
                                    new MdschemaMeasuresRowset(
                                            wrapRequest(request, Olap4jUtil.mapOf(MdschemaMeasuresRowset.CatalogName,
                                                    catalog.getName(), MdschemaMeasuresRowset.SchemaName,
                                                    schema.getName(), MdschemaMeasuresRowset.CubeName, cube.getName())),
                                            handler));
                        }
                        addRow(row, rows);
                    }
                }
            }
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static class MdschemaDimensionsRowset extends Rowset {
        private final Util.Predicate1<Catalog> catalogNameCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;
        private final Util.Predicate1<Dimension> dimensionUnameCond;
        private final Util.Predicate1<Dimension> dimensionNameCond;

        MdschemaDimensionsRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_DIMENSIONS, request, handler);
            catalogNameCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
            dimensionUnameCond = makeCondition(ELEMENT_UNAME_GETTER, DimensionUniqueName);
            dimensionNameCond = makeCondition(ELEMENT_NAME_GETTER, DimensionName);
        }

        public static final int MD_DIMTYPE_OTHER = 3;
        public static final int MD_DIMTYPE_MEASURE = 2;
        public static final int MD_DIMTYPE_TIME = 1;

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the database.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "Not supported.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the cube.");
        private static final Column DimensionName = new Column("DIMENSION_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the dimension.");
        private static final Column DimensionUniqueName = new Column("DIMENSION_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, "The unique name of the dimension.");
        private static final Column DimensionGuid = new Column("DIMENSION_GUID", Type.UUID, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Not supported.");
        private static final Column DimensionCaption = new Column("DIMENSION_CAPTION", Type.String, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "The caption of the dimension.");
        private static final Column DimensionOrdinal = new Column("DIMENSION_ORDINAL", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "The position of the dimension within the cube.");
        // SQL Server returns values:
        //   MD_DIMTYPE_TIME (1)
        //   MD_DIMTYPE_MEASURE (2)
        //   MD_DIMTYPE_OTHER (3)
        private static final Column DimensionType = new Column("DIMENSION_TYPE", Type.Short, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "The type of the dimension.");
        private static final Column DimensionCardinality = new Column("DIMENSION_CARDINALITY", Type.UnsignedInteger,
                null, Column.NOT_RESTRICTION, Column.REQUIRED, "The number of members in the key attribute.");
        private static final Column DefaultHierarchy = new Column("DEFAULT_HIERARCHY", Type.String, null,
                Column.NOT_RESTRICTION, Column.REQUIRED,
                "A hierarchy from the dimension. Preserved for backwards " + "compatibility.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A user-friendly description of the dimension.");
        private static final Column IsVirtual = new Column("IS_VIRTUAL", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Always FALSE.");
        private static final Column IsReadWrite = new Column("IS_READWRITE", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A Boolean that indicates whether the dimension is " + "write-enabled.");
        // SQL Server returns values: 0 or 1
        private static final Column DimensionUniqueSettings = new Column("DIMENSION_UNIQUE_SETTINGS", Type.Integer,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL,
                "A bitmap that specifies which columns contain unique values "
                        + "if the dimension contains only members with unique names.");
        private static final Column DimensionMasterUniqueName = new Column("DIMENSION_MASTER_UNIQUE_NAME", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "Always NULL.");
        private static final Column DimensionIsVisible = new Column("DIMENSION_IS_VISIBLE", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Always TRUE.");
        private static final Column Hierarchies = new Column("HIERARCHIES", Type.Rowset, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Hierarchies in this dimension.");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            for (Catalog catalog : catIter(connection, catNameCond(), catalogNameCond)) {
                populateCatalog(connection, catalog, rows);
            }
        }

        protected void populateCatalog(OlapConnection connection, Catalog catalog, List<Row> rows)
                throws XmlaException, SQLException {
            for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                for (Cube cube : filteredCubes(schema, cubeNameCond)) {
                    populateCube(connection, catalog, cube, rows);
                }
            }
        }

        protected void populateCube(OlapConnection connection, Catalog catalog, Cube cube, List<Row> rows)
                throws XmlaException, SQLException {
            for (Dimension dimension : filter(cube.getDimensions(), dimensionNameCond, dimensionUnameCond)) {
                populateDimension(connection, catalog, cube, dimension, rows);
            }
        }

        protected void populateDimension(OlapConnection connection, Catalog catalog, Cube cube, Dimension dimension,
                                         List<Row> rows) throws XmlaException, SQLException {
            String desc = dimension.getDescription();
            if (desc == null) {
                desc = cube.getName() + " Cube - " + dimension.getName() + " Dimension";
            }

            Row row = new Row();
            row.set(CatalogName.name, catalog.getName());
            row.set(SchemaName.name, cube.getSchema().getName());
            row.set(CubeName.name, cube.getName());
            row.set(DimensionName.name, dimension.getName());
            row.set(DimensionUniqueName.name, dimension.getUniqueName());
            row.set(DimensionCaption.name, dimension.getCaption());
            row.set(DimensionOrdinal.name, cube.getDimensions().indexOf(dimension));
            row.set(DimensionType.name, getDimensionType(dimension));

            //Is this the number of primaryKey members there are??
            // According to microsoft this is:
            //    "The number of members in the key attribute."
            // There may be a better way of doing this but
            // this is what I came up with. Note that I need to
            // add '1' to the number inorder for it to match
            // match what microsoft SQL Server is producing.
            // The '1' might have to do with whether or not the
            // hierarchy has a 'all' member or not - don't know yet.
            // large data set total for Orders cube 0m42.923s
            Hierarchy firstHierarchy = dimension.getHierarchies().get(0);
            NamedList<Level> levels = firstHierarchy.getLevels();
            Level lastLevel = levels.get(levels.size() - 1);

            final XmlaExtra extra = handler.connectionFactory.getExtra();
            //            int n = extra.getLevelCardinality(lastLevel); // Mondrian fix (see mondrian.rolap.RolapHierarchy.RolapHierarchy()) to comply with XMLA schema unsignedInt
            int n = 1000;
            row.set(DimensionCardinality.name, n == Integer.MIN_VALUE ? Integer.MAX_VALUE : n + 1);

            // TODO: I think that this is just the dimension name
            row.set(DefaultHierarchy.name, dimension.getUniqueName());
            row.set(Description.name, desc);
            row.set(IsVirtual.name, false);
            // SQL Server always returns false
            row.set(IsReadWrite.name, false);
            // TODO: don't know what to do here
            // Are these the levels with uniqueMembers == true?
            // How are they mapped to specific column numbers?
            row.set(DimensionUniqueSettings.name, 0);
            row.set(DimensionIsVisible.name, dimension.isVisible());
            if (!dimension.isVisible()) {
                return;
            }
            if (deep) {
                row.set(Hierarchies.name, new MdschemaHierarchiesRowset(
                        wrapRequest(request,
                                Olap4jUtil.mapOf(MdschemaHierarchiesRowset.CatalogName, catalog.getName(),
                                        MdschemaHierarchiesRowset.SchemaName, cube.getSchema().getName(),
                                        MdschemaHierarchiesRowset.CubeName, cube.getName(),
                                        MdschemaHierarchiesRowset.DimensionUniqueName, dimension.getUniqueName())),
                        handler));
            }

            addRow(row, rows);
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static int getDimensionType(Dimension dim) throws OlapException {
        switch (dim.getDimensionType()) {
            case MEASURE:
                return MdschemaDimensionsRowset.MD_DIMTYPE_MEASURE;
//        case TIME:
//            return MdschemaDimensionsRowset.MD_DIMTYPE_TIME;
            default:
                return MdschemaDimensionsRowset.MD_DIMTYPE_OTHER;
        }
    }

    public static class MdschemaFunctionsRowset extends Rowset {
        /**
         * http://www.csidata.com/custserv/onlinehelp/VBSdocs/vbs57.htm
         */
        public enum VarType {
            Empty("Uninitialized (default)"), Null("Contains no valid data"), Integer("Integer subtype"), Long(
                    "Long subtype"), Single("Single subtype"), Double("Double subtype"), Currency(
                    "Currency subtype"), Date("Date subtype"), String("String subtype"), Object(
                    "Object subtype"), Error("Error subtype"), Boolean("Boolean subtype"), Variant(
                    "Variant subtype"), DataObject("DataObject subtype"), Decimal(
                    "Decimal subtype"), Byte("Byte subtype"), Array("Array subtype");

            VarType(String description) {
                Util.discard(description);
            }
        }

        private final Util.Predicate1<String> functionNameCond;

        MdschemaFunctionsRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_FUNCTIONS, request, handler);
            functionNameCond = makeCondition(FunctionName);
        }

        private static final Column FunctionName = new Column("FUNCTION_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the function.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A description of the function.");
        private static final Column ParameterList = new Column("PARAMETER_LIST", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "A comma delimited list of parameters.");
        private static final Column ReturnType = new Column("RETURN_TYPE", Type.Integer, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "The VARTYPE of the return data type of the function.");
        private static final Column Origin = new Column("ORIGIN", Type.Integer, null, Column.RESTRICTION,
                Column.REQUIRED,
                "The origin of the function:  1 for MDX functions.  2 for " + "user-defined functions.");
        private static final Column InterfaceName = new Column("INTERFACE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the interface for user-defined functions");
        private static final Column LibraryName = new Column("LIBRARY_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL,
                "The name of the type library for user-defined functions. " + "NULL for MDX functions.");
        private static final Column Caption = new Column("CAPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The display caption for the function.");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            for (Catalog catalog : catIter(connection, catNameCond())) {
                // By definition, mondrian catalogs have only one
                // schema. It is safe to use get(0)
                final Schema schema = catalog.getSchemas().get(0);
                List<XmlaExtra.FunctionDefinition> funDefs = new ArrayList<XmlaExtra.FunctionDefinition>();

                // olap4j does not support describing functions. Call an
                // auxiliary method.
                extra.getSchemaFunctionList(funDefs, schema, functionNameCond);
                for (XmlaExtra.FunctionDefinition funDef : funDefs) {
                    Row row = new Row();
                    row.set(FunctionName.name, funDef.functionName);
                    row.set(Description.name, funDef.description);
                    row.set(ParameterList.name, funDef.parameterList);
                    row.set(ReturnType.name, funDef.returnType);
                    row.set(Origin.name, funDef.origin);
                    //row.set(LibraryName.name, "");
                    row.set(InterfaceName.name, funDef.interfaceName);
                    row.set(Caption.name, funDef.caption);
                    addRow(row, rows);
                }
            }
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static class MdschemaHierarchiesRowset extends Rowset {
        private final Util.Predicate1<Catalog> catalogCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;
        private final Util.Predicate1<Dimension> dimensionUnameCond;
        private final Util.Predicate1<Hierarchy> hierarchyUnameCond;
        private final Util.Predicate1<Hierarchy> hierarchyNameCond;

        MdschemaHierarchiesRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_HIERARCHIES, request, handler);
            catalogCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
            dimensionUnameCond = makeCondition(ELEMENT_UNAME_GETTER, DimensionUniqueName);
            hierarchyUnameCond = makeCondition(ELEMENT_UNAME_GETTER, HierarchyUniqueName);
            hierarchyNameCond = makeCondition(ELEMENT_NAME_GETTER, HierarchyName);
        }

        private static final int MD_USER_DEFINED = 0x0000001;

        private static final int MD_SYSTEM_ENABLED = 0x0000002;

        private static final int MD_SYSTEM_INTERNAL = 0x0000004;

        private static final int MD_ONE_ATTRIBUTE = 0x0000006;

        private static final short NONE_DISPLAY_HIERARCHY = 0x0000000;

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the catalog to which this hierarchy belongs.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "Not supported");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the cube to which this hierarchy belongs.");
        private static final Column DimensionUniqueName = new Column("DIMENSION_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED,
                "The unique name of the dimension to which this hierarchy " + "belongs.");
        private static final Column HierarchyName = new Column("HIERARCHY_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED,
                "The name of the hierarchy. Blank if there is only a single " + "hierarchy in the dimension.");
        private static final Column HierarchyUniqueName = new Column("HIERARCHY_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, "The unique name of the hierarchy.");

        private static final Column HierarchyGuid = new Column("HIERARCHY_GUID", Type.UUID, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Hierarchy GUID.");

        private static final Column HierarchyCaption = new Column("HIERARCHY_CAPTION", Type.String, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "A label or a caption associated with the hierarchy.");
        private static final Column DimensionType = new Column("DIMENSION_TYPE", Type.Short, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "The type of the dimension.");
        private static final Column HierarchyCardinality = new Column("HIERARCHY_CARDINALITY", Type.UnsignedInteger,
                null, Column.NOT_RESTRICTION, Column.REQUIRED, "The number of members in the hierarchy.");
        private static final Column DefaultMember = new Column("DEFAULT_MEMBER", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "The default member for this hierarchy.");
        private static final Column AllMember = new Column("ALL_MEMBER", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The member at the highest level of rollup in the hierarchy.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A human-readable description of the hierarchy. NULL if no " + "description exists.");
        private static final Column Structure = new Column("STRUCTURE", Type.Short, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "The structure of the hierarchy.");
        private static final Column IsVirtual = new Column("IS_VIRTUAL", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "Always returns False.");
        private static final Column IsReadWrite = new Column("IS_READWRITE", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.REQUIRED,
                "A Boolean that indicates whether the Write Back to dimension " + "column is enabled.");
        private static final Column DimensionUniqueSettings = new Column("DIMENSION_UNIQUE_SETTINGS", Type.Integer,
                null, Column.NOT_RESTRICTION, Column.REQUIRED, "Always returns MDDIMENSIONS_MEMBER_KEY_UNIQUE (1).");
        private static final Column DimensionIsVisible = new Column("DIMENSION_IS_VISIBLE", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.REQUIRED,
                "A Boolean that indicates whether the parent dimension is visible.");
        private static final Column HierarchyIsVisible = new Column("HIERARCHY_IS_VISIBLE", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "A Boolean that indicates whether the hieararchy is visible.");
        private static final Column HierarchyOrdinal = new Column("HIERARCHY_ORDINAL", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED,
                "The ordinal number of the hierarchy across all hierarchies of " + "the cube.");
        private static final Column HierarchyOrigin = new Column("HIERARCHY_ORIGIN", Type.UnsignedShort, null,
                Column.RESTRICTION, Column.REQUIRED,
                "A bitmask that determines the source of the hierarchy.");
        private static final Column CubeSource = new Column("CUBE_SOURCE", Type.UnsignedShort, null,
                Column.RESTRICTION, Column.OPTIONAL,
                "A bitmap with one of the following valid values:\n1 CUBE\n2 DIMENSION.");
        private static final Column HierarchyVisibility = new Column("HIERARCHY_VISIBILITY", Type.UnsignedShort, null,
                Column.RESTRICTION, Column.OPTIONAL,
                "A bitmap with one of the following valid values:\n1 Visible\n2 Not visible.");
        private static final Column DimensionIsShared = new Column("DIMENSION_IS_SHARED", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "Always returns true.");
        private static final Column Levels = new Column("LEVELS", Type.Rowset, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Levels in this hierarchy.");

        private static final Column InstanceSelection = new Column("INSTANCE_SELECTION", Type.UnsignedShort, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "hierarchy display style");

        private static final Column HIERARCHY_DISPLAY_FOLDER = new Column("HIERARCHY_DISPLAY_FOLDER", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");

        private static final Column GroupingBehavior = new Column("GROUPING_BEHAVIOR", Type.UnsignedShort, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");

        private static final Column StructureType = new Column("STRUCTURE_TYPE", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");

        private int hierarchyVisibility() {
            Object visibilityList = restrictions.get(HierarchyVisibility.name);
            if (!(visibilityList instanceof List<?>)) {
                return 1;
            }

            String visibilityString = ((List<?>)visibilityList).get(0).toString();
            if (StringUtils.isEmpty(visibilityString)) {
                return 1;
            }

            return Integer.parseInt(visibilityString);
        }

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            if (isSupportRowsetVisibilities()) {
                int visibility = hierarchyVisibility();
                if (visibility == 2) {
                    return;
                }
            }

            for (Catalog catalog : catIter(connection, catNameCond(), catalogCond)) {
                populateCatalog(connection, catalog, rows);
            }
        }

        protected void populateCatalog(OlapConnection connection, Catalog catalog, List<Row> rows)
                throws XmlaException, SQLException {
            for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                for (Cube cube : filteredCubes(schema, cubeNameCond)) {
                    populateCube(connection, catalog, cube, rows);
                }
            }
        }

        protected void populateCube(OlapConnection connection, Catalog catalog, Cube cube, List<Row> rows)
                throws XmlaException, SQLException {
            int ordinal = 0;
            for (Dimension dimension : cube.getDimensions()) {
                // Must increment ordinal for all dimensions but
                // only output some of them.
                boolean genOutput = dimensionUnameCond.test(dimension);
                if (genOutput) {
                    populateDimension(connection, catalog, cube, dimension, ordinal, rows);
                }
                ordinal += dimension.getHierarchies().size();
            }
        }

        protected void populateDimension(OlapConnection connection, Catalog catalog, Cube cube, Dimension dimension,
                                         int ordinal, List<Row> rows) throws XmlaException, SQLException {
            final NamedList<Hierarchy> hierarchies = dimension.getHierarchies();
            for (Hierarchy hierarchy : filter(hierarchies, hierarchyNameCond, hierarchyUnameCond)) {
                populateHierarchy(connection, catalog, cube, dimension, hierarchy,
                        ordinal + hierarchies.indexOf(hierarchy), rows);
            }
        }

        protected void populateHierarchy(OlapConnection connection, Catalog catalog, Cube cube, Dimension dimension,
                                         Hierarchy hierarchy, int ordinal, List<Row> rows) throws XmlaException, SQLException {
            if (hierarchy.getName().endsWith("$Parent")) {
                // We don't return generated Parent-Child
                // hierarchies.
                return;
            }
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            String desc = hierarchy.getDescription();
            if (desc == null) {
                desc = cube.getName() + " Cube - " + extra.getHierarchyName(hierarchy) + " Hierarchy";
            }

            Row row = new Row();
            row.set(CatalogName.name, catalog.getName());
            row.set(SchemaName.name, cube.getSchema().getName());
            row.set(CubeName.name, cube.getName());
            row.set(DimensionUniqueName.name, dimension.getUniqueName());
            row.set(HierarchyName.name, hierarchy.getName());
            row.set(HierarchyUniqueName.name, hierarchy.getUniqueName());
            //row.set(HierarchyGuid.name, "");

            row.set(HierarchyCaption.name, hierarchy.getCaption());
            row.set(DimensionType.name, getDimensionType(dimension));
            // The number of members in the hierarchy. Because
            // of the presence of multiple hierarchies, this number
            // might not be the same as DIMENSION_CARDINALITY. This
            // value can be an approximation of the real
            // cardinality. Consumers should not assume that this
            // value is accurate.
            //            int cardinality = extra.getHierarchyCardinality(hierarchy);
            int cardinality = 1000;
            row.set(HierarchyCardinality.name,
                    //cardinality
                    cardinality < 0 ? Integer.MAX_VALUE : cardinality);

            row.set(DefaultMember.name, hierarchy.getDefaultMember().getUniqueName());
            if (hierarchy.hasAll()) {
                row.set(AllMember.name, hierarchy.getRootMembers().get(0).getUniqueName());
            }
            row.set(Description.name, desc);

            //TODO: only support:
            // MD_STRUCTURE_FULLYBALANCED (0)
            // MD_STRUCTURE_RAGGEDBALANCED (1)
            row.set(Structure.name, extra.getHierarchyStructure(hierarchy));

            row.set(IsVirtual.name, false);
            row.set(IsReadWrite.name, false);

            // NOTE that SQL Server returns '0' not '1'.
            row.set(DimensionUniqueSettings.name, 1);

            row.set(DimensionIsVisible.name, dimension.isVisible());
            boolean hierarchyVisible = dimension.isVisible() && hierarchy.isVisible();
            row.set(HierarchyIsVisible.name, hierarchyVisible);
            if (!hierarchyVisible) {
                return;
            }
            boolean isAttribute = hierarchy.getLevels().size() == 2
                    && hierarchy.getLevels().get(0).getUniqueName().contains("All");
            if (isAttribute) {
                row.set(HierarchyOrigin.name, MD_ONE_ATTRIBUTE);
                row.set(InstanceSelection.name, NONE_DISPLAY_HIERARCHY);
                row.set(HierarchyOrdinal.name, 1);
            } else {
                row.set(HierarchyOrigin.name, MD_USER_DEFINED);
                row.set(HierarchyOrdinal.name, 1);
            }
            row.set(GroupingBehavior.name, "1");
            row.set(StructureType.name, "Natural");
            String description = hierarchy.getDescription();
            String subfolder = "";
            if (description != null) {
                subfolder = description;
            }
            row.set(HIERARCHY_DISPLAY_FOLDER.name, subfolder);
            // always true
            row.set(DimensionIsShared.name, true);

            if (deep) {
                row.set(Levels.name,
                        new MdschemaLevelsRowset(
                                wrapRequest(request,
                                        Olap4jUtil.mapOf(MdschemaLevelsRowset.CatalogName, catalog.getName(),
                                                MdschemaLevelsRowset.SchemaName, cube.getSchema().getName(),
                                                MdschemaLevelsRowset.CubeName, cube.getName(),
                                                MdschemaLevelsRowset.DimensionUniqueName, dimension.getUniqueName(),
                                                MdschemaLevelsRowset.HierarchyUniqueName, hierarchy.getUniqueName())),
                                handler));
            }
            addRow(row, rows);
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    static class MdschemaLevelsRowset extends Rowset {
        private final Util.Predicate1<Catalog> catalogCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;
        private final Util.Predicate1<Dimension> dimensionUnameCond;
        private final Util.Predicate1<Hierarchy> hierarchyUnameCond;
        private final Util.Predicate1<Level> levelUnameCond;
        private final Util.Predicate1<Level> levelNameCond;

        MdschemaLevelsRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_LEVELS, request, handler);
            catalogCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
            dimensionUnameCond = makeCondition(ELEMENT_UNAME_GETTER, DimensionUniqueName);
            hierarchyUnameCond = makeCondition(ELEMENT_UNAME_GETTER, HierarchyUniqueName);
            levelUnameCond = makeCondition(ELEMENT_UNAME_GETTER, LevelUniqueName);
            levelNameCond = makeCondition(ELEMENT_NAME_GETTER, LevelName);
        }

        public static final int MDLEVEL_TYPE_UNKNOWN = 0x0000;
        public static final int MDLEVEL_TYPE_REGULAR = 0x0000;
        public static final int MDLEVEL_TYPE_ALL = 0x0001;
        public static final int MDLEVEL_TYPE_CALCULATED = 0x0002;
        public static final int MDLEVEL_TYPE_TIME = 0x0004;
        public static final int MDLEVEL_TYPE_RESERVED1 = 0x0008;
        public static final int MDLEVEL_TYPE_TIME_YEARS = 0x0014;
        public static final int MDLEVEL_TYPE_TIME_HALF_YEAR = 0x0024;
        public static final int MDLEVEL_TYPE_TIME_QUARTERS = 0x0044;
        public static final int MDLEVEL_TYPE_TIME_MONTHS = 0x0084;
        public static final int MDLEVEL_TYPE_TIME_WEEKS = 0x0104;
        public static final int MDLEVEL_TYPE_TIME_DAYS = 0x0204;
        public static final int MDLEVEL_TYPE_TIME_HOURS = 0x0304;
        public static final int MDLEVEL_TYPE_TIME_MINUTES = 0x0404;
        public static final int MDLEVEL_TYPE_TIME_SECONDS = 0x0804;
        public static final int MDLEVEL_TYPE_TIME_UNDEFINED = 0x1004;
        public static final int ONE_ATTRIBUTE_HIERARCHY = 0x0006;
        public static final int USER_DEFINED_HIERARCHY = 0x0001;

        // TODO: move the following 2 fields to olap4j

        public static final int MDDIMENSIONS_MEMBER_KEY_UNIQUE = 1;
        public static final int MDDIMENSIONS_MEMBER_NAME_UNIQUE = 2;

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the catalog to which this level belongs.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the schema to which this level belongs.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the cube to which this level belongs.");
        private static final Column DimensionUniqueName = new Column("DIMENSION_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED,
                "The unique name of the dimension to which this level " + "belongs.");
        private static final Column HierarchyUniqueName = new Column("HIERARCHY_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, "The unique name of the hierarchy.");
        private static final Column LevelName = new Column("LEVEL_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the level.");
        private static final Column LevelUniqueName = new Column("LEVEL_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, "The properly escaped unique name of the level.");
        private static final Column LevelGuid = new Column("LEVEL_GUID", Type.UUID, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Level GUID.");
        private static final Column LevelCaption = new Column("LEVEL_CAPTION", Type.String, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "A label or caption associated with the hierarchy.");
        private static final Column LevelNumber = new Column("LEVEL_NUMBER", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED,
                "The distance of the level from the root of the hierarchy. " + "Root level is zero (0).");
        private static final Column LevelCardinality = new Column("LEVEL_CARDINALITY", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED,
                "The number of members in the level. This value can be an " + "approximation of the real cardinality.");
        private static final Column LevelType = new Column("LEVEL_TYPE", Type.Integer, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "Type of the level");
        private static final Column CustomRollupSettings = new Column("CUSTOM_ROLLUP_SETTINGS", Type.Integer, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "A bitmap that specifies the custom rollup options.");
        private static final Column LevelUniqueSettings = new Column("LEVEL_UNIQUE_SETTINGS", Type.Integer, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "A bitmap that specifies which columns contain unique values, "
                + "if the level only has members with unique names or keys.");
        private static final Column LevelIsVisible = new Column("LEVEL_IS_VISIBLE", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "A Boolean that indicates whether the level is visible.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A human-readable description of the level. NULL if no " + "description exists.");
        private static final Column LevelOrigin = new Column("LEVEL_ORIGIN", Type.UnsignedInteger, null, Column.RESTRICTION,
                Column.OPTIONAL, "");
        private static Column CubeSource = new Column("CUBE_SOURCE", Type.UnsignedShort, null, Column.RESTRICTION,
                Column.OPTIONAL, "");
        private static Column LevelVisibility = new Column("LEVEL_VISIBILITY", Type.UnsignedShort, null, Column.RESTRICTION,
                Column.OPTIONAL, "");
        private static final Column LEVEL_ORDERING_PROPERTY = new Column("LEVEL_ORDERING_PROPERTY", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");
        private static final Column LEVEL_DBTYPE = new Column("LEVEL_DBTYPE", Type.Short, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");
        private static final Column LEVEL_NAME_SQL_COLUMN_NAME = new Column("LEVEL_NAME_SQL_COLUMN_NAME", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");
        private static final Column LEVEL_KEY_SQL_COLUMN_NAME = new Column("LEVEL_KEY_SQL_COLUMN_NAME", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");
        private static final Column LEVEL_UNIQUE_NAME_SQL_COLUMN_NAME = new Column("LEVEL_UNIQUE_NAME_SQL_COLUMN_NAME", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");
        private static final Column LevHieAttrName = new Column("LEVEL_ATTRIBUTE_HIERARCHY_NAME", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");
        private static final Column LEVEL_KEY_CARDINALITY = new Column("LEVEL_KEY_CARDINALITY", Type.Short, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "");


        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            for (Catalog catalog : catIter(connection, catNameCond(), catalogCond)) {
                populateCatalog(connection, catalog, rows);
            }
        }

        protected void populateCatalog(OlapConnection connection, Catalog catalog, List<Row> rows)
                throws XmlaException, SQLException {
            for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                for (Cube cube : filteredCubes(schema, cubeNameCond)) {
                    populateCube(connection, catalog, cube, rows);
                }
            }
        }

        protected void populateCube(OlapConnection connection, Catalog catalog, Cube cube, List<Row> rows)
                throws XmlaException, SQLException {
            for (Dimension dimension : filter(cube.getDimensions(), dimensionUnameCond)) {
                populateDimension(connection, catalog, cube, dimension, rows);
            }
        }

        protected void populateDimension(OlapConnection connection, Catalog catalog, Cube cube, Dimension dimension,
                                         List<Row> rows) throws XmlaException, SQLException {
            for (Hierarchy hierarchy : filter(dimension.getHierarchies(), hierarchyUnameCond)) {
                populateHierarchy(connection, catalog, cube, hierarchy, rows);
            }
        }

        protected void populateHierarchy(OlapConnection connection, Catalog catalog, Cube cube, Hierarchy hierarchy,
                                         List<Row> rows) throws XmlaException, SQLException {
            for (Level level : filter(hierarchy.getLevels(), levelUnameCond, levelNameCond)) {
                outputLevel(connection, catalog, cube, hierarchy, level, rows);
            }
        }

        /**
         * Outputs a level.
         *
         * @param catalog   Catalog name
         * @param cube      Cube definition
         * @param hierarchy Hierarchy
         * @param level     Level
         * @param rows      List of rows to output to
         * @return whether the level is visible
         * @throws XmlaException If error occurs
         */
        protected boolean outputLevel(OlapConnection connection, Catalog catalog, Cube cube, Hierarchy hierarchy,
                                      Level level, List<Row> rows) throws XmlaException, SQLException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            String desc = level.getDescription();
            if (desc == null) {
                desc = cube.getName() + " Cube - " + extra.getHierarchyName(hierarchy) + " Hierarchy - "
                        + level.getName() + " Level";
            }

            Row row = new Row();
            row.set(CatalogName.name, catalog.getName());
            row.set(SchemaName.name, cube.getSchema().getName());
            row.set(CubeName.name, cube.getName());
            row.set(DimensionUniqueName.name, hierarchy.getDimension().getUniqueName());
            row.set(HierarchyUniqueName.name, hierarchy.getUniqueName());
            row.set(LevelName.name, level.getName());
            row.set(LevelUniqueName.name, level.getUniqueName());
            //row.set(LevelGuid.name, "");
            row.set(LevelCaption.name, level.getCaption());
            // see notes on this #getDepth()
            row.set(LevelNumber.name, level.getDepth());

            // Get level cardinality
            // According to microsoft this is:
            //   "The number of members in the level."
            //            int n = extra.getLevelCardinality(level);
            int n = 1000;
            if (level.getCaption().equals("(All)")) {
                n = 1;
            }
            row.set(LevelCardinality.name, n < 0 ? Integer.MAX_VALUE : n);

            row.set(LevelType.name, getLevelType(level));

            // TODO: most of the time this is correct
            row.set(CustomRollupSettings.name, 0);

            int uniqueSettings = 0;
//            if (level.getLevelType() == Level.Type.ALL) {
//                uniqueSettings |= 2;
//            }
//            if (extra.isLevelUnique(level)) {
//                uniqueSettings |= 1;
//            }
            boolean isAttribute = hierarchy.getLevels().size() == 2
                    && hierarchy.getLevels().get(0).getUniqueName().contains("All");
            boolean isMeasure = level.getCaption().equals("MeasuresLevel");
            boolean isAllMemberLevel = level.getCaption().equals("(All)");
            if (isAttribute || isMeasure) {
                row.set(LevelOrigin.name, ONE_ATTRIBUTE_HIERARCHY);
                if (isAllMemberLevel) {
                    row.set(LEVEL_ORDERING_PROPERTY.name, "(All)");
                    row.set(LEVEL_DBTYPE.name, 3);
                } else if (isAttribute) {
                    String hierarchyRef = "[$" + hierarchy.getUniqueName() + "]";
                    row.set(LEVEL_ORDERING_PROPERTY.name, level.getName());
                    row.set(LEVEL_NAME_SQL_COLUMN_NAME.name, "NAME( " + hierarchyRef + " )");
                    row.set(LEVEL_KEY_SQL_COLUMN_NAME.name, "KEY( " + hierarchyRef + " )");
                    row.set(LEVEL_UNIQUE_NAME_SQL_COLUMN_NAME.name, "UNIQUENAME( " + hierarchyRef + " )");
                    row.set(LevHieAttrName.name, level.getName());
                    row.set(LEVEL_DBTYPE.name, 130);
                } else {
                    row.set(LevHieAttrName.name, level.getCaption());
                    row.set(LEVEL_DBTYPE.name, 130);
                }
            } else {
                row.set(LevelOrigin.name, USER_DEFINED_HIERARCHY);
            }
            row.set(LEVEL_KEY_CARDINALITY.name, 1);
            row.set(LevelUniqueSettings.name, uniqueSettings);
            boolean levelVisible = level.getDimension().isVisible()
                    && level.getHierarchy().isVisible()
                    && level.isVisible();
            row.set(LevelIsVisible.name, levelVisible);
            row.set(Description.name, desc);
            if (!levelVisible) {
                return true;
            }
            addRow(row, rows);
            return true;
        }

        private int getLevelType(Level lev) {
            return lev.getLevelType().xmlaOrdinal();
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    public static class MdschemaMeasuresRowset extends Rowset {
        public static final int MDMEASURE_AGGR_UNKNOWN = 0;
        public static final int MDMEASURE_AGGR_SUM = 1;
        public static final int MDMEASURE_AGGR_COUNT = 2;
        public static final int MDMEASURE_AGGR_MIN = 3;
        public static final int MDMEASURE_AGGR_MAX = 4;
        public static final int MDMEASURE_AGGR_AVG = 5;
        public static final int MDMEASURE_AGGR_VAR = 6;
        public static final int MDMEASURE_AGGR_STD = 7;
        public static final int MDMEASURE_AGGR_CALCULATED = 127;

        private final Util.Predicate1<Catalog> catalogCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;
        private final Util.Predicate1<Measure> measureUnameCond;
        private final Util.Predicate1<Measure> measureNameCond;

        MdschemaMeasuresRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_MEASURES, request, handler);
            catalogCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
            measureNameCond = makeCondition(ELEMENT_NAME_GETTER, MeasureName);
            measureUnameCond = makeCondition(ELEMENT_UNAME_GETTER, MeasureUniqueName);
        }

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the catalog to which this measure belongs.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the schema to which this measure belongs.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the cube to which this measure belongs.");
        private static final Column MeasureName = new Column("MEASURE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the measure.");
        private static final Column MeasureUniqueName = new Column("MEASURE_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, "The Unique name of the measure.");
        private static final Column MeasureCaption = new Column("MEASURE_CAPTION", Type.String, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "A label or caption associated with the measure.");
        private static final Column MeasureGuid = new Column("MEASURE_GUID", Type.UUID, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Measure GUID.");
        private static final Column MeasureAggregator = new Column("MEASURE_AGGREGATOR", Type.Integer, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "How a measure was derived.");
        private static final Column DataType = new Column("DATA_TYPE", Type.UnsignedShort, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "Data type of the measure.");
        //As mentioned in document, this is a non-restriction column. However, it will cause an error if we change it
        //to be a restriction column.
        private static final Column NumericPrecision = new Column("NUMERIC_PRECISION", Type.UnsignedShort, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The maximum precision of the property.");
        private static final Column NumericScale = new Column("NUMERIC_SCALE", Type.Short, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The number of digits to the right of the decimal point");
        private static final Column MeasureIsVisible = new Column("MEASURE_IS_VISIBLE", Type.Boolean, null,
                Column.RESTRICTION, Column.REQUIRED, "A Boolean that always returns True. If the measure is not "
                + "visible, it will not be included in the schema rowset.");
        private static final Column LevelsList = new Column("LEVELS_LIST", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL,
                "A string that always returns NULL. EXCEPT that SQL Server " + "returns non-null values!!!");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A human-readable description of the measure.");
        private static final Column FormatString = new Column("DEFAULT_FORMAT_STRING", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "The default format string for the measure.");

        private static final Column MEASUREGROUP_NAME = new Column("MEASUREGROUP_NAME", Type.String, null, Column.RESTRICTION, Column.OPTIONAL, "");
        private static final Column CubeSource = new Column("CUBE_SOURCE", Type.UnsignedShort, null,
                Column.RESTRICTION, Column.OPTIONAL, "A bitmap with one of the following valid values:\n1 CUBE\n2 DIMENSION.");
        private static final Column MeasureVisibility = new Column("MEASURE_VISIBILITY", Type.UnsignedShort, null,
                Column.RESTRICTION, Column.OPTIONAL, "A bitmap with one of the following valid values:\n1 Visible\n2 Not visible.");

        private static final Column MEASURE_DISPLAY_FOLDER = new Column("MEASURE_DISPLAY_FOLDER", Type.String, null, Column.NOT_RESTRICTION, Column.OPTIONAL, "");

        private int measureVisibility() {
            Object visibilityList = restrictions.get(MeasureVisibility.name);
            if (!(visibilityList instanceof List<?>)) {
                return 1;
            }

            String visibilityString = ((List<?>)visibilityList).get(0).toString();
            if (StringUtils.isEmpty(visibilityString)) {
                return 1;
            }

            return Integer.parseInt(visibilityString);
        }

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            if (isSupportRowsetVisibilities()) {
                int visibility = measureVisibility();
                if (visibility == 2) {
                    return;
                }
            }

            for (Catalog catalog : catIter(connection, catNameCond(), catalogCond)) {
                populateCatalog(connection, catalog, rows);
            }
        }

        protected void populateCatalog(OlapConnection connection, Catalog catalog, List<Row> rows)
                throws XmlaException, SQLException {
            // SQL Server actually includes the LEVELS_LIST row
            StringBuilder buf = new StringBuilder(100);

            for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                for (Cube cube : filteredCubes(schema, cubeNameCond)) {
                    buf.setLength(0);

                    int j = 0;
                    for (Dimension dimension : cube.getDimensions()) {
                        if (dimension.getDimensionType() == Dimension.Type.MEASURE) {
                            continue;
                        }
                        for (Hierarchy hierarchy : dimension.getHierarchies()) {
                            if (hierarchy.getName().endsWith("$Parent")) {
                                continue;
                            }
                            NamedList<Level> levels = hierarchy.getLevels();
                            Level lastLevel = levels.get(levels.size() - 1);
                            if (j++ > 0) {
                                buf.append(',');
                            }
                            buf.append(lastLevel.getUniqueName());
                        }
                    }
                    String levelListStr = buf.toString();

                    List<Member> calcMembers = new ArrayList<Member>();
                    for (Measure measure : filter(cube.getMeasures(), measureNameCond, measureUnameCond)) {
                        if (measure.isCalculated()) {
                            // Output calculated measures after stored
                            // measures.
                            calcMembers.add(measure);
                        } else {
                            populateMember(connection, catalog, measure, cube, levelListStr, rows);
                        }
                    }

                    for (Member member : calcMembers) {
                        populateMember(connection, catalog, member, cube, null, rows);
                    }
                }
            }
        }

        private void populateMember(OlapConnection connection, Catalog catalog, Member member, Cube cube,
                                    String levelListStr, List<Row> rows) throws SQLException {
            Boolean visible = (Boolean) member.getPropertyValue(Property.StandardMemberProperty.$visible);
            if (visible == null) {
                visible = true;
            }
            if (!visible && !XmlaUtil.shouldEmitInvisibleMembers(request)) {
                return;
            }

            //TODO: currently this is always null
            String desc = member.getDescription();
            if (desc == null) {
                desc = cube.getName() + " Cube - " + member.getName() + " Member";
            }

            final String formatString = (String) member.getPropertyValue(Property.StandardCellProperty.FORMAT_STRING);

            Row row = new Row();
            row.set(CatalogName.name, catalog.getName());
            row.set(SchemaName.name, cube.getSchema().getName());
            row.set(CubeName.name, cube.getName());
            row.set(MeasureName.name, member.getName());
            row.set(MeasureUniqueName.name, member.getUniqueName());
            row.set(MeasureCaption.name, member.getCaption());
            //row.set(MeasureGuid.name, "");

            final XmlaExtra extra = handler.connectionFactory.getExtra();
            row.set(MeasureAggregator.name, extra.getMeasureAggregator(member));

            // DATA_TYPE DBType best guess is string
            XmlaConstants.DBType dbType = XmlaConstants.DBType.WSTR;
            String datatype = (String) member.getPropertyValue(Property.StandardCellProperty.DATATYPE);
            if (datatype != null) {
                if (datatype.equalsIgnoreCase("Integer")) {
                    dbType = XmlaConstants.DBType.I4;
                } else if (datatype.equalsIgnoreCase("Numeric")) {
                    dbType = XmlaConstants.DBType.R8;
                } else {
                    dbType = XmlaConstants.DBType.WSTR;
                }
            }
            row.set(DataType.name, dbType.xmlaOrdinal());
            row.set(MeasureIsVisible.name, visible);

            if (levelListStr != null) {
                row.set(LevelsList.name, levelListStr);
            }

            row.set(Description.name, desc);
            row.set(FormatString.name, formatString);
            String description = member.getDescription();
            if (description != null && description.indexOf(':') != -1) {
                String folder = description.split(":")[1];
                String subfolderName = null;
                if (folder != null) {
                    String measureGroupName = folder.split("@")[0];
                    if (measureGroupName == null || "".equals(measureGroupName) || "Calculated Measure".equals(measureGroupName)) {
                        row.set(MEASUREGROUP_NAME.name, "Values");
                    } else {
                        row.set(MEASUREGROUP_NAME.name, measureGroupName);
                    }
                    if (folder.length() > (measureGroupName.length() + 1)) {
                        subfolderName = folder.substring(measureGroupName.length() + 1);
                    }
                }
                row.set(MEASURE_DISPLAY_FOLDER.name, subfolderName == null ? "" : subfolderName);
            }
            addRow(row, rows);
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    private static boolean isSupportRowsetVisibilities() {
        SemanticConfig config = SemanticConfig.getInstance();
        boolean rowsetVisibilitiesSupported = config.isRowsetVisibilitiesSupported();
        String projectVisibilitiesSupported = config.getProjectRowsetVisibilities();
        if (!rowsetVisibilitiesSupported) {
            return false;
        }
        if (StringUtils.isBlank(projectVisibilitiesSupported) || "*".equals(projectVisibilitiesSupported)) {
            return true;
        }
        List<String> projects = Arrays.asList(projectVisibilitiesSupported.split(","));
        return projects.contains(XmlaRequestContext.getContext().currentProject);
    }

    static class MdschemaMembersRowset extends Rowset {
        private final Util.Predicate1<Catalog> catalogCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;
        private final Util.Predicate1<Dimension> dimensionUnameCond;
        private final Util.Predicate1<Hierarchy> hierarchyUnameCond;
        private final Util.Predicate1<Member> memberNameCond;
        private final Util.Predicate1<Member> memberUnameCond;
        private final Util.Predicate1<Member> memberTypeCond;

        MdschemaMembersRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_MEMBERS, request, handler);
            catalogCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
            dimensionUnameCond = makeCondition(ELEMENT_UNAME_GETTER, DimensionUniqueName);
            hierarchyUnameCond = makeCondition(ELEMENT_UNAME_GETTER, HierarchyUniqueName);
            memberNameCond = makeCondition(ELEMENT_NAME_GETTER, MemberName);
            memberUnameCond = makeCondition(ELEMENT_UNAME_GETTER, MemberUniqueName);
            memberTypeCond = makeCondition(MEMBER_TYPE_GETTER, MemberType);
        }

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the catalog to which this member belongs.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the schema to which this member belongs.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "Name of the cube to which this member belongs.");
        private static final Column DimensionUniqueName = new Column("DIMENSION_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, "Unique name of the dimension to which this member belongs.");
        private static final Column HierarchyUniqueName = new Column("HIERARCHY_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, "Unique name of the hierarchy. If the member belongs to more "
                + "than one hierarchy, there is one row for each hierarchy to " + "which it belongs.");
        private static final Column LevelUniqueName = new Column("LEVEL_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, " Unique name of the level to which the member belongs.");
        private static final Column LevelNumber = new Column("LEVEL_NUMBER", Type.UnsignedInteger, null,
                Column.RESTRICTION, Column.REQUIRED, "The distance of the member from the root of the hierarchy.");
        private static final Column MemberOrdinal = new Column("MEMBER_ORDINAL", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED,
                "Ordinal number of the member. Sort rank of the member when "
                        + "members of this dimension are sorted in their natural sort "
                        + "order. If providers do not have the concept of natural "
                        + "ordering, this should be the rank when sorted by " + "MEMBER_NAME.");
        private static final Column MemberName = new Column("MEMBER_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "Name of the member.");
        private static final Column MemberUniqueName = new Column("MEMBER_UNIQUE_NAME", Type.StringSometimesArray, null,
                Column.RESTRICTION, Column.REQUIRED, " Unique name of the member.");
        private static final Column MemberType = new Column("MEMBER_TYPE", Type.Integer, null, Column.RESTRICTION,
                Column.REQUIRED, "Type of the member.");
        private static final Column MemberGuid = new Column("MEMBER_GUID", Type.UUID, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Memeber GUID.");
        private static final Column MemberCaption = new Column("MEMBER_CAPTION", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "A label or caption associated with the member.");
        private static final Column ChildrenCardinality = new Column("CHILDREN_CARDINALITY", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "Number of children that the member has.");
        private static final Column ParentLevel = new Column("PARENT_LEVEL", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED,
                "The distance of the member's parent from the root level of " + "the hierarchy.");
        private static final Column ParentUniqueName = new Column("PARENT_UNIQUE_NAME", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Unique name of the member's parent.");
        private static final Column ParentCount = new Column("PARENT_COUNT", Type.UnsignedInteger, null,
                Column.NOT_RESTRICTION, Column.REQUIRED, "Number of parents that this member has.");
        private static final Column TreeOp_ = new Column("TREE_OP", Type.Enumeration, Enumeration.TREE_OP,
                Column.RESTRICTION, Column.OPTIONAL, "Tree Operation");
        /* Mondrian specified member properties. */
        private static final Column Depth = new Column("DEPTH", Type.Integer, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "depth");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            for (Catalog catalog : catIter(connection, catNameCond(), catalogCond)) {
                populateCatalog(connection, catalog, rows);
            }
        }

        protected void populateCatalog(OlapConnection connection, Catalog catalog, List<Row> rows)
                throws XmlaException, SQLException {
            for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                for (Cube cube : filteredCubes(schema, cubeNameCond)) {
                    if (isRestricted(MemberUniqueName)) {
                        // NOTE: it is believed that if MEMBER_UNIQUE_NAME is
                        // a restriction, then none of the remaining possible
                        // restrictions other than TREE_OP are relevant
                        // (or allowed??).
                        outputUniqueMemberName(connection, catalog, cube, rows);
                    } else {
                        populateCube(connection, catalog, cube, rows);
                    }
                }
            }
        }

        protected void populateCube(OlapConnection connection, Catalog catalog, Cube cube, List<Row> rows)
                throws XmlaException, SQLException {
            if (isRestricted(LevelUniqueName)) {
                // Note: If the LEVEL_UNIQUE_NAME has been specified, then
                // the dimension and hierarchy are specified implicitly.
                String levelUniqueName = getRestrictionValueAsString(LevelUniqueName);
                if (levelUniqueName == null) {
                    // The query specified two or more unique names
                    // which means that nothing will match.
                    return;
                }

                Level level = lookupLevel(cube, levelUniqueName);
                if (level != null) {
                    // Get members of this level, without access control, but
                    // including calculated members.
                    List<Member> members = level.getMembers();
                    outputMembers(connection, members, catalog, cube, rows);
                }
            } else {
                for (Dimension dimension : filter(cube.getDimensions(), dimensionUnameCond)) {
                    populateDimension(connection, catalog, cube, dimension, rows);
                }
            }
        }

        protected void populateDimension(OlapConnection connection, Catalog catalog, Cube cube, Dimension dimension,
                                         List<Row> rows) throws XmlaException, SQLException {
            for (Hierarchy hierarchy : filter(dimension.getHierarchies(), hierarchyUnameCond)) {
                populateHierarchy(connection, catalog, cube, hierarchy, rows);
            }
        }

        protected void populateHierarchy(OlapConnection connection, Catalog catalog, Cube cube, Hierarchy hierarchy,
                                         List<Row> rows) throws XmlaException, SQLException {
            if (isRestricted(LevelNumber)) {
                int levelNumber = getRestrictionValueAsInt(LevelNumber);
                if (levelNumber == -1) {
                    LOGGER.warn("RowsetDefinition.populateHierarchy: " + "LevelNumber invalid");
                    return;
                }
                NamedList<Level> levels = hierarchy.getLevels();
                if (levelNumber >= levels.size()) {
                    LOGGER.warn("RowsetDefinition.populateHierarchy: " + "LevelNumber (" + levelNumber
                            + ") is greater than number of levels (" + levels.size() + ") for hierarchy \""
                            + hierarchy.getUniqueName() + "\"");
                    return;
                }

                Level level = levels.get(levelNumber);
                List<Member> members = level.getMembers();
                outputMembers(connection, members, catalog, cube, rows);
            } else {
                // At this point we get ALL of the members associated with
                // the Hierarchy (rather than getting them one at a time).
                // The value returned is not used at this point but they are
                // now cached in the SchemaReader.
                for (Level level : hierarchy.getLevels()) {
                    outputMembers(connection, level.getMembers(), catalog, cube, rows);
                }
            }
        }

        /**
         * Returns whether a value contains all of the bits in a mask.
         */
        private static boolean mask(int value, int mask) {
            return (value & mask) == mask;
        }

        /**
         * Adds a member to a result list and, depending upon the
         * <code>treeOp</code> parameter, other relatives of the member. This
         * method recursively invokes itself to walk up, down, or across the
         * hierarchy.
         */
        private void populateMember(OlapConnection connection, Catalog catalog, Cube cube, Member member, int treeOp,
                                    List<Row> rows) throws SQLException {
            // Visit node itself.
            if (mask(treeOp, TreeOp.SELF.xmlaOrdinal())) {
                outputMember(connection, member, catalog, cube, rows);
            }
            // Visit node's siblings (not including itself).
            if (mask(treeOp, TreeOp.SIBLINGS.xmlaOrdinal())) {
                final List<Member> siblings;
                final Member parent = member.getParentMember();
                if (parent == null) {
                    siblings = member.getHierarchy().getRootMembers();
                } else {
                    siblings = Olap4jUtil.cast(parent.getChildMembers());
                }
                for (Member sibling : siblings) {
                    if (sibling.equals(member)) {
                        continue;
                    }
                    populateMember(connection, catalog, cube, sibling, TreeOp.SELF.xmlaOrdinal(), rows);
                }
            }
            // Visit node's descendants or its immediate children, but not both.
            if (mask(treeOp, TreeOp.DESCENDANTS.xmlaOrdinal())) {
                for (Member child : member.getChildMembers()) {
                    populateMember(connection, catalog, cube, child,
                            TreeOp.SELF.xmlaOrdinal() | TreeOp.DESCENDANTS.xmlaOrdinal(), rows);
                }
            } else if (mask(treeOp, TreeOp.CHILDREN.xmlaOrdinal())) {
                for (Member child : member.getChildMembers()) {
                    populateMember(connection, catalog, cube, child, TreeOp.SELF.xmlaOrdinal(), rows);
                }
            }
            // Visit node's ancestors or its immediate parent, but not both.
            if (mask(treeOp, TreeOp.ANCESTORS.xmlaOrdinal())) {
                final Member parent = member.getParentMember();
                if (parent != null) {
                    populateMember(connection, catalog, cube, parent,
                            TreeOp.SELF.xmlaOrdinal() | TreeOp.ANCESTORS.xmlaOrdinal(), rows);
                }
            } else if (mask(treeOp, TreeOp.PARENT.xmlaOrdinal())) {
                final Member parent = member.getParentMember();
                if (parent != null) {
                    populateMember(connection, catalog, cube, parent, TreeOp.SELF.xmlaOrdinal(), rows);
                }
            }
        }

        protected ArrayList<Column> pruneRestrictions(ArrayList<Column> list) {
            // If they've restricted TreeOp, we don't want to literally filter
            // the result on TreeOp (because it's not an output column) or
            // on MemberUniqueName (because TreeOp will have caused us to
            // generate other members than the one asked for).
            if (list.contains(TreeOp_)) {
                list.remove(TreeOp_);
                list.remove(MemberUniqueName);
            }
            return list;
        }

        private void outputMembers(OlapConnection connection, List<Member> members, final Catalog catalog, Cube cube,
                                   List<Row> rows) throws SQLException {
            for (Member member : members) {
                outputMember(connection, member, catalog, cube, rows);
            }
        }

        private void outputUniqueMemberName(final OlapConnection connection, final Catalog catalog, Cube cube,
                                            List<Row> rows) throws SQLException {
            final Object unameRestrictions = restrictions.get(MemberUniqueName.name);
            List<String> list;
            if (unameRestrictions instanceof String) {
                list = Collections.singletonList((String) unameRestrictions);
            } else {
                list = (List<String>) unameRestrictions;
            }
            for (String memberUniqueName : list) {
                final IdentifierNode identifierNode = IdentifierNode.parseIdentifier(memberUniqueName);
                Member member = cube.lookupMember(identifierNode.getSegmentList());
                if (member == null) {
                    return;
                }
                if (isRestricted(TreeOp_)) {
                    int treeOp = getRestrictionValueAsInt(TreeOp_);
                    if (treeOp == -1) {
                        return;
                    }
                    populateMember(connection, catalog, cube, member, treeOp, rows);
                } else {
                    outputMember(connection, member, catalog, cube, rows);
                }
            }
        }

        private void outputMember(OlapConnection connection, Member member, final Catalog catalog, Cube cube,
                                  List<Row> rows) throws SQLException {
            if (!memberNameCond.test(member)) {
                return;
            }
            if (!memberTypeCond.test(member)) {
                return;
            }

            final XmlaExtra extra = handler.connectionFactory.getExtra();
            extra.checkMemberOrdinal(member);

            // Check whether the member is visible, otherwise do not dump.
            Boolean visible = (Boolean) member.getPropertyValue(Property.StandardMemberProperty.$visible);
            if (visible == null) {
                visible = true;
            }
            if (!visible && !XmlaUtil.shouldEmitInvisibleMembers(request)) {
                return;
            }

            final Level level = member.getLevel();
            final Hierarchy hierarchy = level.getHierarchy();
            final Dimension dimension = hierarchy.getDimension();

            int adjustedLevelDepth = level.getDepth();

            Row row = new Row();
            row.set(CatalogName.name, catalog.getName());
            row.set(SchemaName.name, cube.getSchema().getName());
            row.set(CubeName.name, cube.getName());
            row.set(DimensionUniqueName.name, dimension.getUniqueName());
            row.set(HierarchyUniqueName.name, hierarchy.getUniqueName());
            row.set(LevelUniqueName.name, level.getUniqueName());
            row.set(LevelNumber.name, adjustedLevelDepth);
            row.set(MemberOrdinal.name, member.getOrdinal());
            row.set(MemberName.name, member.getName());
            row.set(MemberUniqueName.name, member.getUniqueName());
            row.set(MemberType.name, member.getMemberType().ordinal());
            //row.set(MemberGuid.name, "");
            row.set(MemberCaption.name, member.getCaption());
            int defaultCard = 1000;
            row.set(ChildrenCardinality.name, defaultCard);

            if (adjustedLevelDepth == 0) {
                row.set(ParentLevel.name, 0);
            } else {
                row.set(ParentLevel.name, adjustedLevelDepth - 1);
                final Member parentMember = member.getParentMember();
                if (parentMember != null) {
                    row.set(ParentUniqueName.name, parentMember.getUniqueName());
                }
            }

            row.set(ParentCount.name, member.getParentMember() == null ? 0 : 1);

            row.set(Depth.name, member.getDepth());
            addRow(row, rows);
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    public static class MdschemaSetsRowset extends Rowset {
        private final Util.Predicate1<Catalog> catalogCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;
        private final Util.Predicate1<NamedSet> setUnameCond;
        private static final String GLOBAL_SCOPE = "1";

        MdschemaSetsRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_SETS, request, handler);
            catalogCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
            setUnameCond = makeCondition(ELEMENT_UNAME_GETTER, SetName);
        }

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, true, true, null);
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, true, true, null);
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, true, false, null);
        private static final Column SetName = new Column("SET_NAME", Type.String, null, true, false, null);
        private static final Column SetCaption = new Column("SET_CAPTION", Type.String, null, true, true, null);
        private static final Column Scope = new Column("SCOPE", Type.Integer, null, true, false, null);
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, false, true,
                "A human-readable description of the measure.");
        // Some DMVs do restriction on this column, but it is not supported here
        private static final Column CubeSource = new Column("CUBE_SOURCE", Type.UnsignedShort, null, true, true, null);
        private static final Column Expression = new Column("EXPRESSION", Type.String, null, false, true, "");
        private static final Column Dimensions = new Column("DIMENSIONS", Type.String, null, false, true, "");

        private static final Column SetDisplayFolder = new Column("SET_DISPLAY_FOLDER", Type.String, null, false, true, "");
        private static final Column SetEvaluationContext = new Column("SET_EVALUATION_CONTEXT", Type.Integer, null, true, true, "");
        private static final Column HierarchyUniqueName = new Column("HIERARCHY_UNIQUE_NAME", Type.String, null, true, true, "");


        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, OlapException {
            for (Catalog catalog : catIter(connection, catNameCond(), catalogCond)) {
                processCatalog(connection, catalog, rows);
            }
        }

        private void processCatalog(OlapConnection connection, Catalog catalog, List<Row> rows) throws OlapException {
            for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                for (Cube cube : filter(sortedCubes(schema), cubeNameCond)) {
                    populateNamedSets(cube, catalog, rows);
                }
            }
        }

        private String getDimensions(NamedSet namedSet) {
            List<String> list = new ArrayList<>();
            collectExpNode(namedSet.getExpression(), list);
            return list.stream().filter(x -> !"[Measures]".equals(x)).distinct().collect(Collectors.joining(","));
        }

        private void collectExpNode(ParseTreeNode node, List<String> list) {
            if (node instanceof CallNode) {
                for (Object object : ((CallNode) node).getArgList()) {
                    if (object instanceof CallNode) {
                        collectExpNode((CallNode) object, list);
                    } else if (object instanceof HierarchyNode) {
                        list.add(object.toString());
                    } else if (object instanceof LevelNode) {
                        list.add(((LevelNode) object).getLevel().getHierarchy().getUniqueName());
                    } else if (object instanceof MemberNode) {
                        list.add(((MemberNode) object).getMember().getLevel().getHierarchy().getUniqueName());
                    }
                }
            }
        }

        private void populateNamedSets(Cube cube, Catalog catalog, List<Row> rows) {
            for (NamedSet namedSet : filter(cube.getSets(), setUnameCond)) {
                if (namedSet.isVisible()) {
                    Row row = new Row();
                    row.set(CatalogName.name, catalog.getName());
                    row.set(SchemaName.name, cube.getSchema().getName());
                    row.set(CubeName.name, cube.getName());
                    if (XmlaRequestContext.getContext().tableauFlag) {
                        row.set(SetName.name, namedSet.getName());
                    } else {
                        row.set(SetName.name, namedSet.getUniqueName());
                    }
                    row.set(SetCaption.name, namedSet.getCaption());
                    row.set(Scope.name, GLOBAL_SCOPE);
                    row.set(Description.name, namedSet.getDescription());
                    row.set(Dimensions.name, getDimensions(namedSet));
                    addRow(row, rows);
                }
            }
        }
    }

    static class MdschemaPropertiesRowset extends Rowset {
        private final Util.Predicate1<Catalog> catalogCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;
        private final Util.Predicate1<Dimension> dimensionUnameCond;
        private final Util.Predicate1<Hierarchy> hierarchyUnameCond;
        private final Util.Predicate1<Property> propertyNameCond;

        MdschemaPropertiesRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_PROPERTIES, request, handler);
            catalogCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
            dimensionUnameCond = makeCondition(ELEMENT_UNAME_GETTER, DimensionUniqueName);
            hierarchyUnameCond = makeCondition(ELEMENT_UNAME_GETTER, HierarchyUniqueName);
            propertyNameCond = makeCondition(ELEMENT_NAME_GETTER, PropertyName);
        }

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the database.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the schema to which this property belongs.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the cube.");
        private static final Column DimensionUniqueName = new Column("DIMENSION_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.OPTIONAL, "The unique name of the dimension.");
        private static final Column HierarchyUniqueName = new Column("HIERARCHY_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.OPTIONAL, "The unique name of the hierarchy.");
        private static final Column LevelUniqueName = new Column("LEVEL_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.OPTIONAL, "The unique name of the level to which this property belongs.");
        // According to MS this should not be nullable
        private static final Column MemberUniqueName = new Column("MEMBER_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.OPTIONAL, "The unique name of the member to which the property belongs.");
        private static final Column PropertyName = new Column("PROPERTY_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "Name of the property.");
        private static final Column PropertyType = new Column("PROPERTY_TYPE", Type.Short, null, Column.RESTRICTION,
                Column.REQUIRED, "A bitmap that specifies the type of the property");
        private static final Column PropertyCaption = new Column("PROPERTY_CAPTION", Type.String, null,
                Column.NOT_RESTRICTION, Column.REQUIRED,
                "A label or caption associated with the property, used " + "primarily for display purposes.");
        private static final Column DataType = new Column("DATA_TYPE", Type.UnsignedShort, null, Column.NOT_RESTRICTION,
                Column.REQUIRED, "Data type of the property.");
        private static final Column CharacterMaximumLength = new Column("CHARACTER_MAXIMUM_LENGTH", Type.UnsignedInteger, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The maximum possible length of the property.");
        private static final Column NumericPrecision = new Column("NUMERIC_PRECISION", Type.UnsignedShort, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The maximum precision of the property.");
        private static final Column NumericScale = new Column("NUMERIC_SCALE", Type.Short, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The number of digits to the right of the decimal point");
        private static final Column PropertyContentType = new Column("PROPERTY_CONTENT_TYPE", Type.Short, null,
                Column.RESTRICTION, Column.OPTIONAL, "The type of the property.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A human-readable description of the measure.");

        protected boolean needConnection() {
            return true;
        }

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            // Default PROPERTY_TYPE is MDPROP_MEMBER.
            @SuppressWarnings({"unchecked"}) final List<String> list = (List<String>) restrictions.get(PropertyType.name);
            Set<Property.TypeFlag> typeFlags;
            if (list == null) {
                typeFlags = Olap4jUtil.enumSetOf(Property.TypeFlag.MEMBER);
            } else {
                typeFlags = Property.TypeFlag.getDictionary().forMask(Integer.valueOf(list.get(0)));
            }

            for (Property.TypeFlag typeFlag : typeFlags) {
                switch (typeFlag) {
                    case MEMBER:
                        populateMember(rows, connection);
                        break;
                    case CELL:
                        populateCell(rows);
                        break;
                    case SYSTEM:
                    case BLOB:
                    default:
                        break;
                }
            }
        }

        private void populateCell(List<Row> rows) {
            for (Property.StandardCellProperty property : Property.StandardCellProperty.values()) {
                Row row = new Row();
                row.set(PropertyType.name, Property.TypeFlag.getDictionary().toMask(property.getType()));
                row.set(PropertyName.name, property.name());
                row.set(PropertyCaption.name, property.getCaption());
                row.set(DataType.name, property.getDatatype().xmlaOrdinal());
                addRow(row, rows);
            }
        }

        private void populateMember(List<Row> rows, OlapConnection connection) throws SQLException {
            /* https://github.com/olap4j/olap4j-xmlaserver/issues/15
             * OlapConnection connection =
                handler.getConnection(
                    request, Collections.<String, String>emptyMap());*/
            for (Catalog catalog : catIter(connection, catNameCond(), catalogCond)) {
                populateCatalog(catalog, rows);
            }
        }

        protected void populateCatalog(Catalog catalog, List<Row> rows) throws XmlaException, SQLException {
            for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                for (Cube cube : filteredCubes(schema, cubeNameCond)) {
                    populateCube(catalog, cube, rows);
                }
            }
        }

        protected void populateCube(Catalog catalog, Cube cube, List<Row> rows) throws XmlaException, SQLException {
            if (cube instanceof SharedDimensionHolderCube) {
                return;
            }
            if (isRestricted(LevelUniqueName)) {
                // Note: If the LEVEL_UNIQUE_NAME has been specified, then
                // the dimension and hierarchy are specified implicitly.
                String levelUniqueName = getRestrictionValueAsString(LevelUniqueName);
                if (levelUniqueName == null) {
                    // The query specified two or more unique names
                    // which means that nothing will match.
                    return;
                }
                Level level = lookupLevel(cube, levelUniqueName);
                if (level == null) {
                    return;
                }
                populateLevel(catalog, cube, level, rows);
            } else {
                for (Dimension dimension : filter(cube.getDimensions(), dimensionUnameCond)) {
                    populateDimension(catalog, cube, dimension, rows);
                }
            }
        }

        private void populateDimension(Catalog catalog, Cube cube, Dimension dimension, List<Row> rows)
                throws SQLException {
            for (Hierarchy hierarchy : filter(dimension.getHierarchies(), hierarchyUnameCond)) {
                populateHierarchy(catalog, cube, hierarchy, rows);
            }
        }

        private void populateHierarchy(Catalog catalog, Cube cube, Hierarchy hierarchy, List<Row> rows)
                throws SQLException {
            for (Level level : hierarchy.getLevels()) {
                populateLevel(catalog, cube, level, rows);
            }
        }

        private void populateLevel(Catalog catalog, Cube cube, Level level, List<Row> rows) throws SQLException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            for (Property property : filter(extra.getLevelProperties(level), propertyNameCond)) {
                if (extra.isPropertyInternal(property)) {
                    continue;
                }
                outputProperty(extra, property, catalog, cube, level, rows);
            }
        }

        private void outputProperty(XmlaExtra extra, Property property, Catalog catalog, Cube cube,
                                    Level level, List<Row> rows) {
            Hierarchy hierarchy = level.getHierarchy();
            Dimension dimension = hierarchy.getDimension();

            String propertyName = property.getName();

            Row row = new Row();
            row.set(CatalogName.name, catalog.getName());
            row.set(SchemaName.name, cube.getSchema().getName());
            row.set(CubeName.name, cube.getName());
            row.set(DimensionUniqueName.name, dimension.getUniqueName());
            /*
            row.set(HierarchyUniqueName.name, hierarchy.getUniqueName());
            row.set(LevelUniqueName.name, level.getUniqueName());
            */
            row.set(HierarchyUniqueName.name, dimension.getUniqueName() + "." + hierarchy.getUniqueName());
            row.set(LevelUniqueName.name,
                    dimension.getUniqueName() + "." + hierarchy.getUniqueName() + "." + level.getUniqueName());
            //TODO: what is the correct value here
            //row.set(MemberUniqueName.name, "");

            row.set(PropertyName.name, propertyName);
            // Only member properties now
            row.set(PropertyType.name, Property.TypeFlag.MEMBER.xmlaOrdinal());
            row.set(PropertyContentType.name, Property.ContentType.REGULAR.xmlaOrdinal());
            row.set(PropertyCaption.name, property.getCaption());
            XmlaConstants.DBType dbType = getDBTypeFromProperty(property);
            row.set(DataType.name, dbType.xmlaOrdinal());

            String desc = cube.getName() + " Cube - " + extra.getHierarchyName(hierarchy) + " Hierarchy - "
                    + level.getName() + " Level - " + property.getName() + " Property";
            row.set(Description.name, desc);

            addRow(row, rows);
        }

        protected void setProperty(PropertyDefinition propertyDef, String value) {
            switch (propertyDef) {
                case Content:
                    break;
                default:
                    super.setProperty(propertyDef, value);
            }
        }
    }

    /**
     * MDSCHEMA_KPIS is not implemented.
     * See <a href="https://docs.microsoft.com/en-us/bi-reference/schema-rowsets/ole-db-olap/mdschema-kpis-rowset"></a>
     */
    static class MdschemaKpisRowset extends Rowset {
        MdschemaKpisRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_KPIS, request, handler);
        }

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String,
                null, Column.RESTRICTION, Column.OPTIONAL, "The source database.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String,
                null, Column.RESTRICTION, Column.OPTIONAL, "Not supported.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String,
                null, Column.RESTRICTION, Column.OPTIONAL, "The parent cube for the KPI.");
        private static final Column MeasuregroupName = new Column("MEASUREGROUP_NAME", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL,
                "The associated measure group for the KPI.\n" +
                        "You can use this column to determine the dimensionality of the KPI. " +
                        "If \"<NULL>\", the KPI will be dimensioned by all measure groups.\n" +
                        "The default value is \"<NULL>\".");
        private static final Column KpiName = new Column("KPI_NAME", Type.String,
                null, Column.RESTRICTION, Column.OPTIONAL, "The name of the KPI.");
        private static final Column KpiCaption = new Column("KPI_CAPTION", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL,
                "A label or caption associated with the KPI. " +
                        "Used primarily for display purposes. If a caption does not exist, KPI_NAME is returned.");
        private static final Column KpiDescrption = new Column("KPI_DESCRIPTION", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "A human-readable description of the KPI.");
        private static final Column KpiDisplayFolder = new Column("KPI_DISPLAY_FOLDER", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "A string that identifies " +
                "the path of the display folder that the client application uses to show the member. " +
                "The folder level separator is defined by the client application. For the " +
                "tools and clients supplied by Analysis Services, the backslash (\\) is " +
                "the level separator. To provide multiple display folders, use a semicolon (;) " +
                "to separate the folders.");
        private static final Column KpiValue = new Column("KPI_VALUE", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The unique name of the member " +
                "in the measures dimension for the KPI Value.");
        private static final Column KpiGoal = new Column("KPI_GOAL", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The unique name of the member in " +
                "the measures dimension for the KPI Goal.\nReturns NULL if there is no Goal defined.");
        private static final Column KpiStatus = new Column("KPI_STATUS", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The unique name of the member in " +
                "the measures dimension for the KPI Status.\nReturns NULL if there is no Status defined.");
        private static final Column KpiTrend = new Column("KPI_TREND", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The unique name of the member in " +
                "the measures dimension for the KPI Trend.\nReturns NULL if there is no Trend defined.");
        private static final Column KpiStatusGraphic = new Column("KPI_STATUS_GRAPHIC", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The default graphical representation of the KPI.");
        private static final Column KpiTrendGraphic = new Column("KPI_TREND_GRAPHIC", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The default graphical representation of the KPI.");
        private static final Column KpiWeight = new Column("KPI_WEIGHT", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The unique name of the member in " +
                "the measures dimension for the KPI Weight.");
        private static final Column KpiCurrentTimeMember = new Column("KPI_CURRENT_TIME_MEMBER", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The unique name of the member in " +
                "the time dimension that defines the temporal context of the KPI.\nReturns NULL if there is no Time member defined.");
        private static final Column KpiParentKpiName = new Column("KPI_PARENT_KPI_NAME", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The name of the parent KPI.");
        private static final Column Scope = new Column("SCOPE", Type.Integer,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "The scope of the KPI. " +
                "The KPI can be a session KPI or global KPI.\nThis column can have one of the following values:\n" +
                "MDKPI_SCOPE_GLOBAL=1\nMDKPI_SCOPE_SESSION=2");
        private static final Column Annotations = new Column("ANNOTATIONS", Type.String,
                null, Column.NOT_RESTRICTION, Column.OPTIONAL, "(Optional) A set of notes, in XML format.");
        private static final Column CubeSource = new Column("CUBE_SOURCE", Type.UnsignedShort,
                null, Column.RESTRICTION, Column.OPTIONAL, "Default restriction is a value of 1. " +
                "A bitmap with one of the following valid values:\n1 CUBE\n2 DIMENSION");

        @Override
        protected void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows) throws XmlaException, SQLException {

        }
    }

    public static class MdschemaMeasureGroupsRowset extends Rowset {

        private final Util.Predicate1<Catalog> catalogNameCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;
        private final Util.Predicate1<Measure> measureUnameCond;
        private final Util.Predicate1<Measure> measureNameCond;

        MdschemaMeasureGroupsRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_MEASUREGROUPS, request, handler);
            catalogNameCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
            measureNameCond = makeCondition(ELEMENT_NAME_GETTER, MeasureName);
            measureUnameCond = makeCondition(ELEMENT_UNAME_GETTER, MeasureUniqueName);
        }

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the catalog to which this measure group belongs.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the schema to which this measure group belongs.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the cube to which this measure group belongs.");
        private static final Column MeasureGroupName = new Column("MEASUREGROUP_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the measure group.");
        private static final Column Description = new Column("DESCRIPTION", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "A human-readable description of the measure group.");
        private static final Column IsWriteEnabled = new Column("IS_WRITE_ENABLED", Type.Boolean, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "Describes whether a measure group is write-enabled");
        private static final Column MeasureGroupCaption = new Column("MEASUREGROUP_CAPTION", Type.String, null,
                Column.NOT_RESTRICTION, Column.OPTIONAL, "A label or caption associated with the measure group.");
        private static final Column MeasureName = new Column("MEASURE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the measure.");
        private static final Column MeasureUniqueName = new Column("MEASURE_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, "The Unique name of the measure.");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            for (Catalog catalog : catIter(connection, catNameCond(), catalogNameCond)) {
                for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                    for (Cube cube : filter(sortedCubes(schema), cubeNameCond)) {
                        Set<String> measureGroupNames = new HashSet<>();
                        for (Measure measure : filter(cube.getMeasures(), measureNameCond, measureUnameCond)) {
                            String description = measure.getDescription();
                            String measureGroupName = null;
                            if (description != null && description.indexOf(':') != -1) {
                                String folder = description.split(":")[1];
                                if (folder != null) {
                                    measureGroupName = folder.split("@")[0];
                                }
                            }
                            if (measureGroupName == null || "".equals(measureGroupName) || "Calculated Measure".equals(measureGroupName)) {
                                measureGroupNames.add("Values");
                            } else {
                                measureGroupNames.add(measureGroupName);
                            }
                        }
                        for (String measureGroupName : measureGroupNames) {
                            Row row = new Row();
                            row.set(CatalogName.name, catalog.getName());
                            row.set(SchemaName.name, schema.getName());
                            row.set(CubeName.name, cube.getName());
                            row.set(MeasureGroupName.name, measureGroupName);
                            row.set(Description.name, "");
                            row.set(IsWriteEnabled.name, false);
                            row.set(MeasureGroupCaption.name, measureGroupName);
                            addRow(row, rows);
                        }
                    }
                }
            }
        }
    }

    public static class MdschemaMeasureGroupsDimensionsRowset extends Rowset {

        private final Util.Predicate1<Catalog> catalogNameCond;
        private final Util.Predicate1<Schema> schemaNameCond;
        private final Util.Predicate1<Cube> cubeNameCond;
        private final Util.Predicate1<Measure> measureUnameCond;
        private final Util.Predicate1<Measure> measureNameCond;

        MdschemaMeasureGroupsDimensionsRowset(XmlaRequest request, XmlaHandler handler) {
            super(MDSCHEMA_MEASUREGROUP_DIMENSIONS, request, handler);
            catalogNameCond = makeCondition(CATALOG_NAME_GETTER, CatalogName);
            schemaNameCond = makeCondition(SCHEMA_NAME_GETTER, SchemaName);
            cubeNameCond = makeCondition(ELEMENT_NAME_GETTER, CubeName);
            measureNameCond = makeCondition(ELEMENT_NAME_GETTER, MeasureName);
            measureUnameCond = makeCondition(ELEMENT_UNAME_GETTER, MeasureUniqueName);
        }

        private static final Column CatalogName = new Column("CATALOG_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the catalog to which this measure group dimension belongs.");
        private static final Column SchemaName = new Column("SCHEMA_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the schema to which this measure group dimension belongs.");
        private static final Column CubeName = new Column("CUBE_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the cube to which this measure  group dimension belongs.");
        private static final Column MeasureGroupName = new Column("MEASUREGROUP_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the measure group dimension .");
        private static final Column MeasureGroupCardinality = new Column("MEASUREGROUP_CARDINALITY", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The cardinality of the measure group dimension .");
        private static final Column DimensionUniqueName = new Column("DIMENSION_UNIQUE_NAME", Type.String, null, Column.RESTRICTION,
                Column.OPTIONAL, "The name of the measure  group dimension.");
        private static final Column DimensionCardinality = new Column("DIMENSION_CARDINALITY", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The cardinality of the dimension.");
        private static final Column DimensionIsVisible = new Column("DIMENSION_IS_VISIBLE", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Whether the dimension is visible.");
        private static final Column DimensionIsFactDimension = new Column("DIMENSION_IS_FACT_DIMENSION", Type.Boolean, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "Whether the dimension is fact table.");
        private static final Column DimensionPath = new Column("DIMENSION_PATH", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, Column.UNBOUNDED, "The path of dimension.");
        private static final Column MeasureGroupDimension = new Column("MeasureGroupDimension", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The dimension of measure group.");
        private static final Column DimensionGranularity = new Column("DIMENSION_GRANULARITY", Type.String, null, Column.NOT_RESTRICTION,
                Column.OPTIONAL, "The dimension of measure group.");
        private static final Column DimensionVisibility = new Column("DIMENSION_VISIBILITY", Type.UnsignedShort, null, Column.RESTRICTION,
                Column.OPTIONAL, "The visibility of dimension");
        private static final Column MeasureName = new Column("MEASURE_NAME", Type.String, null, Column.RESTRICTION,
                Column.REQUIRED, "The name of the measure.");
        private static final Column MeasureUniqueName = new Column("MEASURE_UNIQUE_NAME", Type.String, null,
                Column.RESTRICTION, Column.REQUIRED, "The Unique name of the measure.");

        public void populateImpl(XmlaResponse response, OlapConnection connection, List<Row> rows)
                throws XmlaException, SQLException {
            final XmlaExtra extra = handler.connectionFactory.getExtra();
            for (Catalog catalog : catIter(connection, catNameCond(), catalogNameCond)) {
                for (Schema schema : filter(catalog.getSchemas(), schemaNameCond)) {
                    for (Cube cube : filter(sortedCubes(schema), cubeNameCond)) {
                        Set<String> measureGroupNames = new HashSet<>();
                        for (Measure measure : filter(cube.getMeasures(), measureNameCond, measureUnameCond)) {
                            String description = measure.getDescription();
                            String measureGroupName = null;
                            if (description != null && description.indexOf(':') != -1) {
                                String folder = description.split(":")[1];
                                if (folder != null) {
                                    measureGroupName = folder.split("@")[0];
                                }
                            }
                            if (measureGroupName == null || "".equals(measureGroupName) || "Calculated Measure".equals(measureGroupName)) {
                                measureGroupNames.add("Values");
                            } else {
                                measureGroupNames.add(measureGroupName);
                            }
                        }
                        for (String measureGroupName : measureGroupNames) {
                            Row row = new Row();
                            row.set(CatalogName.name, catalog.getName());
                            row.set(CubeName.name, cube.getName());
                            row.set(MeasureGroupName.name, measureGroupName);
                            row.set(DimensionUniqueName.name, "");
                            addRow(row, rows);
                        }
                    }
                }
            }
        }

    }

    public static final Util.Function1<Catalog, String> CATALOG_NAME_GETTER = new Util.Function1<Catalog, String>() {
        public String apply(Catalog catalog) {
            return catalog.getName();
        }
    };

    public static final Util.Function1<Schema, String> SCHEMA_NAME_GETTER = new Util.Function1<Schema, String>() {
        public String apply(Schema schema) {
            return schema.getName();
        }
    };

    public static final Util.Function1<MetadataElement, String> ELEMENT_NAME_GETTER = new Util.Function1<MetadataElement, String>() {
        public String apply(MetadataElement element) {
            return element.getName();
        }
    };

    public static final Util.Function1<MetadataElement, String> ELEMENT_UNAME_GETTER = new Util.Function1<MetadataElement, String>() {
        public String apply(MetadataElement element) {
            return element.getUniqueName();
        }
    };

    public static final Util.Function1<Member, Member.Type> MEMBER_TYPE_GETTER = new Util.Function1<Member, Member.Type>() {
        public Member.Type apply(Member member) {
            return member.getMemberType();
        }
    };

    public static final Util.Function1<PropertyDefinition, String> PROPDEF_NAME_GETTER = new Util.Function1<PropertyDefinition, String>() {
        public String apply(PropertyDefinition property) {
            return property.name();
        }
    };

    static void serialize(StringBuilder buf, Collection<String> strings) {
        int k = 0;
        for (String name : Util.sort(strings)) {
            if (k++ > 0) {
                buf.append(',');
            }
            buf.append(name);
        }
    }

    private static Level lookupLevel(Cube cube, String levelUniqueName) {
        for (Dimension dimension : cube.getDimensions()) {
            for (Hierarchy hierarchy : dimension.getHierarchies()) {
                for (Level level : hierarchy.getLevels()) {
                    if (level.getUniqueName().equals(levelUniqueName)) {
                        return level;
                    }
                }
            }
        }
        return null;
    }

    static Iterable<Cube> sortedCubes(Schema schema) throws OlapException {
        return Util.sort(schema.getCubes(), new Comparator<Cube>() {
            public int compare(Cube o1, Cube o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    static Iterable<Cube> filteredCubes(final Schema schema, Util.Predicate1<Cube> cubeNameCond) throws OlapException {
        final Iterable<Cube> iterable = filter(sortedCubes(schema), cubeNameCond);
        if (!cubeNameCond.test(new SharedDimensionHolderCube(schema))) {
            return iterable;
        }
        return Composite.of(Collections.singletonList(new SharedDimensionHolderCube(schema)), iterable);
    }

    private static XmlaRequest wrapRequest(XmlaRequest request, Map<Column, String> map) {
        final Map<String, Object> restrictionsMap = new HashMap<String, Object>(request.getRestrictions());
        for (Map.Entry<Column, String> entry : map.entrySet()) {
            restrictionsMap.put(entry.getKey().name, Collections.singletonList(entry.getValue()));
        }

        return new DelegatingXmlaRequest(request) {
            @Override
            public Map<String, Object> getRestrictions() {
                return restrictionsMap;
            }
        };
    }

    /**
     * Returns an iterator over the catalogs in a connection, setting the
     * connection's catalog to each successful catalog in turn.
     *
     * @param connection Connection
     * @param conds      Zero or more conditions to be applied to catalogs
     * @return Iterator over catalogs
     */
    private static Iterable<Catalog> catIter(final OlapConnection connection, final Util.Predicate1<Catalog>... conds) {
        return new Iterable<Catalog>() {
            public Iterator<Catalog> iterator() {
                try {
                    return new Iterator<Catalog>() {
                        final Iterator<Catalog> catalogIter = Util.filter(connection.getOlapCatalogs(), conds)
                                .iterator();

                        public boolean hasNext() {
                            return catalogIter.hasNext();
                        }

                        public Catalog next() {
                            Catalog catalog = catalogIter.next();
                            try {
                                connection.setCatalog(catalog.getName());
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                            return catalog;
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                } catch (OlapException e) {
                    throw new RuntimeException("Failed to obtain a list of catalogs form the connection object.", e);
                }
            }
        };
    }

    private static class DelegatingXmlaRequest implements XmlaRequest {
        protected final XmlaRequest request;

        public DelegatingXmlaRequest(XmlaRequest request) {
            this.request = request;
        }

        public XmlaConstants.Method getMethod() {
            return request.getMethod();
        }

        public Map<String, String> getProperties() {
            return request.getProperties();
        }

        public Map<String, Object> getRestrictions() {
            return request.getRestrictions();
        }

        @Override
        public boolean isCancel() {
            return request.isCancel();
        }

        public String getStatement() {
            return request.getStatement();
        }

        public String getRoleName() {
            return request.getRoleName();
        }

        public String getRequestType() {
            return request.getRequestType();
        }

        public boolean isDrillThrough() {
            return request.isDrillThrough();
        }

        @Override
        public Map<String, String> getParameters() {
            return request.getParameters();
        }

        public String getUsername() {
            return request.getUsername();
        }

        public String getPassword() {
            return request.getPassword();
        }

        public String getSessionId() {
            return request.getSessionId();
        }
    }

    /**
     * Dummy implementation of {@link Cube} that holds all shared dimensions
     * in a given schema. Less error-prone than requiring all generator code
     * to cope with a null Cube.
     */
    private static class SharedDimensionHolderCube implements Cube {
        private final Schema schema;

        public SharedDimensionHolderCube(Schema schema) {
            this.schema = schema;
        }

        public Schema getSchema() {
            return schema;
        }

        public NamedList<Dimension> getDimensions() {
            try {
                return schema.getSharedDimensions();
            } catch (OlapException e) {
                throw new RuntimeException(e);
            }
        }

        public NamedList<Hierarchy> getHierarchies() {
            final NamedList<Hierarchy> hierarchyList = new ArrayNamedListImpl<Hierarchy>() {
                public String getName(Object o) {
                    return ((Hierarchy) o).getName();
                }
            };
            for (Dimension dimension : getDimensions()) {
                hierarchyList.addAll(dimension.getHierarchies());
            }
            return hierarchyList;
        }

        public List<Measure> getMeasures() {
            return Collections.emptyList();
        }

        public NamedList<NamedSet> getSets() {
            throw new UnsupportedOperationException();
        }

        public Collection<Locale> getSupportedLocales() {
            throw new UnsupportedOperationException();
        }

        public Member lookupMember(List<IdentifierSegment> identifierSegments) throws org.olap4j.OlapException {
            throw new UnsupportedOperationException();
        }

        public List<Member> lookupMembers(Set<Member.TreeOp> treeOps, List<IdentifierSegment> identifierSegments)
                throws org.olap4j.OlapException {
            throw new UnsupportedOperationException();
        }

        public boolean isDrillThroughEnabled() {
            return false;
        }

        public String getName() {
            return "";
        }

        public String getUniqueName() {
            return "";
        }

        public String getCaption() {
            return "";
        }

        public String getDescription() {
            return "";
        }

        public boolean isVisible() {
            return false;
        }
    }

    public Column[] getColumnDefinitions() {
        return columnDefinitions;
    }

    public Column[] getDiscoverOutputColumns(boolean clientIsPowerBI) {
        return columnDefinitions;
    }
}

// End RowsetDefinition.java
