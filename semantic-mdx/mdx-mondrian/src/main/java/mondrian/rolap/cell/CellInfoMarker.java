package mondrian.rolap.cell;

/**
 * 很明显在从 ROWS(1) -> COLUMNS(0) -> SLICERS(-1) 的循环过程中
 * 针对 (i,j), 当 j = j0 时, 如果 i = i0 ... in 皆已经 ready
 * 那么对于 j = j0 时可以直接跳过循环
 */
public class CellInfoMarker {

    private boolean ready;

    private final CellInfoContainer container;

    private final int[] axesLength;

    private final int[] startPos;

    private final int[] indexPos;

    public CellInfoMarker(CellInfoContainer container, int axes) {
        this.container = container;
        this.axesLength = new int[axes];
        this.startPos = new int[axes];
        this.indexPos = new int[axes];
    }

    public boolean isReady() {
        return this.ready;
    }

    /**
     * 这里是因为第一次时不知道各个轴 tuple 大小
     */
    public void setAxesLength(int axisOrdinal, int length) {
        if (this.ready) {
            return;
        }
        this.axesLength[axisOrdinal] = length;
        for (int axisLength : this.axesLength) {
            if (axisLength == 0) {
                return;
            }
        }
        this.ready = true;
    }

    public int[] getStartPos() {
        return startPos.clone();
    }

    public void calculatePos() {
        if (!ready) {
            return;
        }
        calculatePos(axesLength.length - 1);
    }

    private boolean calculatePos(int axisOrdinal) {
        if (axisOrdinal < 0) {
            CellInfo cellInfo = container.lookup(indexPos);
            return cellInfo.ready;
        }
        for (int i = startPos[axisOrdinal]; i < axesLength[axisOrdinal]; i++) {
            this.indexPos[axisOrdinal] = i;
            boolean result = calculatePos(axisOrdinal - 1);
            if (!result) {
                // 当前维度的坐标未准备就绪，停止
                return false;
            }
            // 当前维度的坐标就绪，迭代到下一个坐标，并且清除下一级维度的坐标值
            startPos[axisOrdinal] = i + 1;
            for (int j = axisOrdinal - 1; j >= 0; j--) {
                startPos[j] = 0;
            }
        }
        return true;
    }

}
