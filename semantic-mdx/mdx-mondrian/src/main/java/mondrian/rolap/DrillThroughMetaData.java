package mondrian.rolap;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class DrillThroughMetaData implements ResultSetMetaData {
    private final ResultSetMetaData _metaData;
    private final List<String> drillThroughTitles;

    public DrillThroughMetaData(ResultSetMetaData metaData, RolapStoredMeasure measure) {
        this._metaData = metaData;
        RolapMeasureGroup measureGroup = measure.getMeasureGroup();
        this.drillThroughTitles = new ArrayList<>(
                measureGroup.dimensionMap3.size() + measureGroup.measureList.size() + 1);
        drillThroughTitles.add(null);

//        RolapCubeDimension dimension = measureGroup.getFactTableDimension();
//        if (dimension != null) {
//            for (RolapCubeHierarchy hierarchy : dimension.getHierarchyList()) {
//                if (hierarchy.getLevelList().size() == 2) {
//                    drillThroughTitles.add(hierarchy.getLevelList().get(1).getAttribute().getUniqueName());
//                }
//            }
//        }
        for (RolapCubeDimension dimension : measureGroup.dimensionMap3.keySet()) {
            for (RolapCubeHierarchy hierarchy : dimension.getHierarchyList()) {
                if (hierarchy.getLevelList().size() == 2) {
                    drillThroughTitles.add(hierarchy.getLevelList().get(1).getAttribute().getUniqueName());
                }
            }
        }
        for (RolapStoredMeasure baseMeasure : measureGroup.measureList) {
            if (!measure.getStarMeasure().getTable().equals(baseMeasure.getStarMeasure().getTable())) {
                continue;
            }
            if (measure.equals(baseMeasure)) {
                continue;
            }
            drillThroughTitles.add(baseMeasure.getUniqueName());
        }
        drillThroughTitles.add(measure.getUniqueName());
    }

    @Override
    public int getColumnCount() throws SQLException {
        return _metaData.getColumnCount();
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        return _metaData.isAutoIncrement(column);
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        return _metaData.isCaseSensitive(column);
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        return _metaData.isSearchable(column);
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        return _metaData.isCurrency(column);
    }

    @Override
    public int isNullable(int column) throws SQLException {
        return _metaData.isNullable(column);
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        return _metaData.isSigned(column);
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        return _metaData.getColumnDisplaySize(column);
    }

    @Override
    public String getColumnLabel(int column) {
        return drillThroughTitles.get(column);
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return _metaData.getColumnName(column);
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        return _metaData.getSchemaName(column);
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        return _metaData.getPrecision(column);
    }

    @Override
    public int getScale(int column) throws SQLException {
        return _metaData.getScale(column);
    }

    @Override
    public String getTableName(int column) throws SQLException {
        return _metaData.getTableName(column);
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        return _metaData.getCatalogName(column);
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        return _metaData.getColumnType(column);
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        return _metaData.getColumnTypeName(column);
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        return _metaData.isReadOnly(column);
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        return _metaData.isWritable(column);
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        return _metaData.isDefinitelyWritable(column);
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        return _metaData.getColumnClassName(column);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return _metaData.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return _metaData.isWrapperFor(iface);
    }
}
