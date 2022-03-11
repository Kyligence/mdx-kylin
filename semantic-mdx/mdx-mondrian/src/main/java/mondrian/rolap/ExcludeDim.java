
package mondrian.rolap;

import java.util.ArrayList;
import java.util.List;

public class ExcludeDim {

    private String dimension;

    private boolean excludeAll;

    private List<String> excludeCols = new ArrayList<>();

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public boolean isExcludeAll() {
        return excludeAll;
    }

    public void setExcludeAll(boolean excludeAll) {
        this.excludeAll = excludeAll;
    }

    public List<String> getExcludeCols() {
        return excludeCols;
    }

    public void setExcludeCols(List<String> excludeCols) {
        this.excludeCols = excludeCols;
    }

    public void addExcludeCols(String colName) {
        this.excludeCols.add(colName);
    }
}