

package mondrian.rolap.sql;

import mondrian.rolap.RolapSchema;
import mondrian.rolap.RolapStar;
import mondrian.spi.Dialect;
import mondrian.util.Pair;

import java.util.*;

public abstract class CustomizeSqlQuery extends SqlQueryBuilder.ProtectedSqlQuery {

    public abstract void setM2mPk(String m2mPk);

    public abstract void setBridgeTable(String bridgeTable);

    public abstract void setFactAndPath(Pair<RolapStar.Table, Map<String, RolapSchema.PhysPath>> factAndPath);

    public abstract void addDimension(SqlQueryBuilder.Column dimensionCol);

    public abstract void addFilter(SqlQueryBuilder.Column filterCol);

    public abstract void addMeasure(RolapStar.Measure measure);

    public abstract void setWeightedColumns(Collection<RolapSchema.PhysColumn> weightColumns);

    public abstract void initNonQueryingM2MTable();

    public abstract void addNonQueryingM2MTable(RolapSchema.PhysRelation table);

    public abstract void removeNonQueryingM2MTable(RolapSchema.PhysRelation table);

    public CustomizeSqlQuery(Dialect dialect) {
        super(dialect);
    }

}
