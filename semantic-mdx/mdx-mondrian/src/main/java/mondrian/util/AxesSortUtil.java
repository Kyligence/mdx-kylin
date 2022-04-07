
package mondrian.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mondrian.calc.TupleList;
import mondrian.calc.impl.ListTupleList;
import mondrian.olap.Axis;
import mondrian.olap.Member;
import mondrian.rolap.RolapAxis;

public class AxesSortUtil {

    public void sortAxes(Axis[] axes) {
        if (axes == null || axes.length == 0) {
            return;
        }
        for (int i = 0; i < axes.length; i++) {
            if (axes[i] instanceof RolapAxis) {
                RolapAxis axis = (RolapAxis) axes[i];
                if (axis == null || axis.getTupleList() == null || axis.getTupleList().size() == 0) {
                    continue;
                }
                RolapAxis sortedAxis = sortRolapAxis(axis);
                axes[i] = sortedAxis;
            }
        }
    }

    private RolapAxis sortRolapAxis(RolapAxis axis) {
        // 有序序列的结尾
        int startFindPosition = 0;
        // grand total 的位置
        int endFindPosition = 0;
        // 汇总的级别，0代表 grand total，1代表一级汇总
        int currentAggLevel = -1;
        List<List<Member>> tupleList = axis.getTupleList();
        // 如果只有一个维度，则不需要重排序
        if (tupleList.get(0).size() <= 1) {
            return axis;
        }
        Map<Integer, List<Integer>> insertIdxMap = new HashMap<>(axis.getTupleList().size());
        for (int i = 0; i < tupleList.size(); i++) {
            List<Member> tuple = tupleList.get(i);
            int allMemIdxOfTuple;
            if ((allMemIdxOfTuple = allMemIdxOfTuple(tuple)) != -1) {
                if (endFindPosition == 0) {
                    endFindPosition = i;
                }
                // 如果汇总级别变化，则需要从头查找，则需要从头开始找
                if (allMemIdxOfTuple > currentAggLevel) {
                    currentAggLevel = allMemIdxOfTuple;
                    startFindPosition = 0;
                }
                int insertPosition = findInsertPosition(tuple, tupleList, i, startFindPosition, endFindPosition, allMemIdxOfTuple, insertIdxMap);
                if (insertPosition == -2) {
                    // 如果没有找到位置，则表示无用的元素
                    if (insertPosition == -2) {
                        continue;
                    }
                }
                // 如果找到了，则从此位置开始找起
                startFindPosition = insertPosition;
            }
        }
        int grandTotalIdx = endFindPosition;
        return new RolapAxis(createTupleList(tupleList, insertIdxMap, grandTotalIdx));
    }

    private TupleList createTupleList(List<List<Member>> tupleList, Map<Integer, List<Integer>> insertIdxMap, int grandTotalIdx) {
        int arity = tupleList.get(0).size();
        List<Member> members = new ArrayList<>(arity * tupleList.size());
        for (int i = 0; i < grandTotalIdx; i++) {
            // insert all member first
            List<Integer> insertList = insertIdxMap.get(i);
            if (insertList != null) {
                for (int allIdx : insertList) {
                    members.addAll(tupleList.get(allIdx));
                }
            }
            members.addAll(tupleList.get(i));
        }
        return new ListTupleList(arity, members);
    }

    // 判断是否是一个 all member 元素，如果是返回第一个 all 的位置，如果不是，返回 -1
    private int allMemIdxOfTuple(List<Member> tuple) {
        if (tuple != null && tuple.size() > 0) {
            for (int i = 0; i < tuple.size(); i++) {
                if (isAllMember(tuple.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isAllMember(Member member) {
        return member.getLevel().toString().contains("[(All)]");
    }

    // 默认从有序序列的结尾开始查找插入的位置,返回插入位置的索引
    private int findInsertPosition(List<Member> tuple, List<List<Member>> tupleList, int currentTuplePosition,
                                   int startFindPosition, int endFindPosition, int allMemIdxOfTuple, Map<Integer, List<Integer>> insertIdxMap) {
        for (int i = startFindPosition; i < endFindPosition; i++) {
            if (i == currentTuplePosition) {
                // 如果没找到要插入的位置，可能是无用的元素，返回-2, 如果开头是 all ，则不需要移动位置
                return currentTuplePosition == 0 ? 0 : -2;
            }
            if (shouldInsertBefore(tuple, tupleList.get(i), allMemIdxOfTuple)) {
                List<Integer> insertList = insertIdxMap.get(i);
                if (insertList == null) {
                    insertList = new ArrayList<>(tuple.size());
                    insertIdxMap.put(i, insertList);
                }
                insertList.add(currentTuplePosition);
                return i;
            }
        }
        return -2;
    }

    /*
    *  (1, 2, All)  (1, 3, 1)    false
    *  (1, All, All)  (1, 3, 1)  true, but (1, 1 ,All) will insert before this
    *  (1, All, All) (1, 1, 1) ==> (1, 1, All)  (1, 1, 1)
    * */
    private boolean shouldInsertBefore(List<Member> tuple0, List<Member> tuple1, int allMemIdxOfTuple) {
        // only one axis, all member may be the first
        if (tuple1 == null) {
            return false;
        }
        for (int i = 0; i < allMemIdxOfTuple; i++) {
            if (!tuple0.get(i).getUniqueName().equals(tuple1.get(i).getUniqueName())) {
                return false;
            }
        }

        return true;
    }

}
