/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2015-2015 Pentaho
// All Rights Reserved.
*/
package mondrian.spi.impl;

import mondrian.olap.Util;
import mondrian.rolap.SqlStatement;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link mondrian.spi.Dialect} for Kylin.
 *
 * @author Sebastien Jelsch
 * @since Jun 08, 2015
 */
public class KylinDialect extends JdbcDialectImpl {

    public static final Map<Types, SqlStatement.Type> KYLIN_TYPE_MAP;
    static {
        Map typeMapInitial = new HashMap<Types, SqlStatement.Type>();
        typeMapInitial.put(Types.TINYINT, SqlStatement.Type.INT);
        typeMapInitial.put(Types.SMALLINT, SqlStatement.Type.INT);
        typeMapInitial.put(Types.INTEGER, SqlStatement.Type.INT);

        typeMapInitial.put(Types.BOOLEAN, SqlStatement.Type.BOOLEAN);
        typeMapInitial.put(Types.BIT, SqlStatement.Type.BOOLEAN);

        typeMapInitial.put(Types.DOUBLE, SqlStatement.Type.DOUBLE);
        typeMapInitial.put(Types.FLOAT, SqlStatement.Type.DOUBLE);
        typeMapInitial.put(Types.REAL, SqlStatement.Type.DOUBLE);
        typeMapInitial.put(Types.NUMERIC, SqlStatement.Type.DOUBLE);
        typeMapInitial.put(Types.DECIMAL, SqlStatement.Type.DOUBLE);


        typeMapInitial.put(Types.BIGINT, SqlStatement.Type.LONG);

        typeMapInitial.put(Types.DATE, SqlStatement.Type.DATE);

        typeMapInitial.put(Types.TIMESTAMP, SqlStatement.Type.TIMESTAMP);


        typeMapInitial.put(Types.CHAR, SqlStatement.Type.STRING);
        typeMapInitial.put(Types.VARCHAR, SqlStatement.Type.STRING);
        typeMapInitial.put(Types.LONGVARCHAR, SqlStatement.Type.STRING);
        typeMapInitial.put(Types.NVARCHAR, SqlStatement.Type.STRING);
        typeMapInitial.put(Types.LONGNVARCHAR, SqlStatement.Type.STRING);
        KYLIN_TYPE_MAP = Collections.unmodifiableMap(typeMapInitial);
    }


    public static final JdbcDialectFactory FACTORY =
            new JdbcDialectFactory(KylinDialect.class, DatabaseProduct.KYLIN) {
                protected boolean acceptsConnection(Connection connection) {
                    return super.acceptsConnection(connection);
                }
            };

    /**
     * Creates a KylinDialect.
     *
     * @param connection Connection
     * @throws SQLException on error
     */
    public KylinDialect(Connection connection) throws SQLException {
        super(connection);
    }

    @Override
    public boolean allowsCountDistinct() {
        return true;
    }

    @Override
    public boolean allowsJoinOn() {
        return true;
    }

    @Override
    public SqlStatement.Type getType(ResultSetMetaData metaData, int columnIndex) throws SQLException {
        return KYLIN_TYPE_MAP.containsKey(metaData.getColumnType(columnIndex + 1)) ? KYLIN_TYPE_MAP.get(metaData.getColumnType(columnIndex + 1))
                : super.getType(metaData, columnIndex);
    }

    @Override
    public String generateOrderItem(
            String expr,
            boolean nullable,
            boolean ascending,
            boolean collateNullsLast)
    {
        if (ascending) {
            return expr + " ASC";
        } else {
            return expr + " DESC";
        }
    }

    /*
    * Kylin doesn't support UPPER()
    */
    @Override
    public String toUpper(String expr) {
        return expr;
    }

    @Override
    public boolean allowsRegularExpressionInWhereClause() {
        return true;
    }

    @Override
    public String generateRegularExpression(
            String source,
            String javaRegExp) {

        return  "lower({fn CONVERT(" + source + ", SQL_VARCHAR)}) LIKE '%"
                + (javaRegExp == null ? "null" : Util.escapeFuzzySearch(javaRegExp.toLowerCase())) + "%'";

    }
}
