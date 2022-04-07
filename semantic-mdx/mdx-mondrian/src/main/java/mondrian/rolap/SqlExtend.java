package mondrian.rolap;

import lombok.Getter;
import mondrian.server.Locus;

import java.util.Objects;

@Getter
public class SqlExtend {

    private final String sql;
    private final int hash;

    public SqlExtend(Locus locus, String sql) {
        this.sql = sql;
        this.hash = locus.hashCode();
    }

    public int hashCode() {
        int h = hash;
        if (sql != null) {
            h = 31 * sql.hashCode();
        }
        return h;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SqlExtend)) {
            return false;
        }
        SqlExtend that = (SqlExtend) obj;
        return hash == that.hash && Objects.equals(sql, that.sql);
    }

    public String toString() {
        return sql;
    }
}
