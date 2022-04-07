package mondrian.rolap.cell;

import java.util.Arrays;

/**
 * CellInfo 容器实现，用于在疏密矩阵情形下替代稀疏矩阵
 * 构造需要传入矩阵大小，例如： (6, 10, 5) 表示数组大小分配 300
 * 读取基于位置 (i,j,k)， pos(i,j,k) 描述 Cell 在数组中的位置
 * 例如：
 * #  pos(0, 0, 0) = 0 + 0 + 0 = 0
 * #  pos(5, 2, 1) = 5 + 2 * 6 + 1 * 10 * 6 = 77
 * #  pos(5, 9, 4) = 5 + 9 * 6 + 4 * 10 * 6 = 299
 */
public class CellInfoArray implements CellInfoContainer {

    private final int[] axes;
    private final CellInfo[] array;
    private int size;

    public CellInfoArray(int[] axes) {
        this.axes = axes;
        int size = 1;
        for (int axis : axes) {
            size *= axis + 1;
        }
        this.array = new CellInfo[size];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void trimToSize() {
    }

    @Override
    public void clear() {
        Arrays.fill(array, null);
        size = 0;
    }

    @Override
    public CellInfo create(int[] pos) {
        int key = index(pos);
        CellInfo ci = array[key];
        if (ci == null) {
            ci = new CellInfo(0);
            array[key] = ci;
            size++;
        }
        return ci;
    }

    @Override
    public CellInfo lookup(int[] pos) {
        return array[index(pos)];
    }

    int index(int[] pos) {
        int index = 0;
        int radix = 1;
        for (int i = 0; i < pos.length; i++) {
            index += pos[i] * radix;
            radix *= axes[i];
        }
        return index;
    }

}
