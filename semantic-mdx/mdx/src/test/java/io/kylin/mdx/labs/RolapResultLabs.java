package io.kylin.mdx.labs;

import mondrian.rolap.RolapResultUtil;
import mondrian.rolap.cell.CellInfo;
import mondrian.rolap.cell.CellInfoContainer;
import mondrian.rolap.cell.CellInfoMarker;
import mondrian.rolap.cell.CellInfoPool;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class RolapResultLabs {

    private static final AtomicInteger count = new AtomicInteger();
    private static final AtomicInteger total = new AtomicInteger();
    private static final AtomicInteger single = new AtomicInteger(50);

    private static final int[] axes = new int[]{5, 10, 6};

    public static void main(String[] args) {
        total.set(axes[0] * axes[1] * axes[2]);
        CellInfoContainer container = new CellInfoPool(axes.length);
        CellInfoMarker marker = new CellInfoMarker(container, axes.length);
        int index = 1;
        while (true) {
            int[] startPos = marker.getStartPos();
            System.out.println("第 " + index + " 轮计算, 起始 : " + Arrays.toString(startPos));
            execute(container, marker, axes.length - 1, new int[axes.length], startPos);
            if (!phase()) {
                break;
            } else {
                single.set(50);
                marker.calculatePos();
            }
            index++;
            if (index > 10) {
                System.out.println("计算超出，计算结束！已就绪：" + count.get());
                break;
            }
        }
        print(container, axes);
    }

    private static boolean phase() {
        return count.get() < total.get();
    }

    private static void execute(CellInfoContainer container, CellInfoMarker marker, int axis, int[] pos, int[] startPos) {
        marker.setAxesLength(axis, axes[axis]);
        int i = startPos[axis];
        startPos[axis] = 0;
        for (; i < axes[axis]; i++) {
            pos[axis] = i;
            if (axis == 0) {
                execute(container, pos);
            } else {
                execute(container, marker, axis - 1, pos, startPos);
            }
        }
    }

    private static void execute(CellInfoContainer container, int[] pos) {
        CellInfo cellInfo = container.create(pos);
        if (cellInfo.ready) {
            return;
        }
        cellInfo.value = calculate();
        if (RolapResultUtil.isValueNotReady()) {
            RolapResultUtil.resetValueNotReady();
        } else {
            cellInfo.ready = true;
            count.incrementAndGet();
        }
    }

    private static int calculate() {
        int i = RandomUtils.nextInt() % 10000;
        if (i >= 100 && single.decrementAndGet() >= 0) {
            return Math.abs(RandomUtils.nextInt() % 55536) + 10000;
        } else {
            RolapResultUtil.markValueNotReady();
            return 0;
        }
    }

    private static void print(CellInfoContainer container, int[] axes) {
        int[] pos = new int[axes.length];
        for (int i = 0; i < axes[0]; i++) {
            pos[0] = i;
            System.out.println("行 : " + i);
            for (int j = 0; j < axes[1]; j++) {
                pos[1] = j;
                System.out.print("\t列 : " + j + "\t\t");
                for (int k = 0; k < axes[2]; k++) {
                    pos[2] = k;
                    CellInfo cellInfo = container.lookup(pos);
                    String value = cellInfo.ready ? String.valueOf(cellInfo.value) : "-----";
                    System.out.print(value + "\t");
                }
                System.out.println();
            }
        }
    }

}
