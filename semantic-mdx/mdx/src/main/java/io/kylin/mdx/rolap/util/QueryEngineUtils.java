/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.rolap.util;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.rolap.cache.CacheManager;
import mondrian.mdx.*;
import mondrian.olap.*;
import mondrian.olap.fun.DescendantsFunDef;
import mondrian.olap.fun.SetFunDef;
import mondrian.olap.fun.VisualTotalsFunDef;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;
import mondrian.olap.type.Type;
import mondrian.resource.MondrianResource;
import mondrian.rolap.*;

import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QueryEngineUtils {
    // Array / Collection utils
    /**
     * Returns the length of an array with the default value 0.
     */
    public static <T> int getArrayLength(T[] array) {
        if (array == null) {
            return 0;
        }
        return array.length;
    }

    /**
     * Returns the length of an array with the default value 0.
     */
    public static <T> int getArrayNonNullLength(T[] array) {
        if (array == null) {
            return 0;
        }
        int nonNullCount = 0;
        for (T item : array) {
            if (item != null) {
                nonNullCount++;
            }
        }
        return nonNullCount;
    }

    /**
     * Returns the size of a collection with the default value 0.
     */
    public static <T> int getCollectionSize(Collection<T> collection) {
        if (collection == null) {
            return 0;
        }
        return collection.size();
    }

    /**
     * Returns the count of non-null elements of a collection with a default value 0.
     */
    public static <T> int getCollectionNonNullSize(Collection<T> collection) {
        if (collection == null) {
            return 0;
        }
        int nonNullCount = 0;
        for (T item : collection) {
            if (item != null) {
                nonNullCount++;
            }
        }
        return nonNullCount;
    }

    /**
     * Applies a "T -> Integer" mapper to a collection, then calculates the product of the mapped values.
     * @return The product result.
     * @throws ResourceLimitExceededException If the product result is over {@link Long#MAX_VALUE}.
     */
    public static <T> long multiplyIntValues(Collection<T> collection,
                                             Function<T, Integer> intMapper) {
        if (collection == null || collection.isEmpty()) {
            return 0L;
        }

        long result = 1L;
        for (T item : collection) {
            int value = intMapper.apply(item);
            if (value == 0) {
                return 0L;
            }
            result = multiplyAndCheckLongOverFlow(result, value);
        }

        return result;
    }

    /**
     * Applies a "T -> Integer" mapper to an array, then calculates the product of the mapped values.
     * @return The product result.
     * @throws ResourceLimitExceededException If the product result is over {@link Long#MAX_VALUE}.
     */
    /*public static <T> long multiplyIntValues(T[] array,
                                             Function<T, Integer> intMapper) {
        if (array == null || array.length == 0) {
            return 0L;
        }

        long result = 1L;
        for (T item : array) {
            int value = intMapper.apply(item);
            if (value == 0) {
                return 0L;
            }
            result = multiplyAndCheckLongOverFlow(result, value);
        }

        return result;
    }
     */

    public static long multiplyAndCheckLongOverFlow(long previousResult, long value) {
        long result = previousResult * value;

        if (previousResult > 0 && value > 0 && (result < previousResult || result < value)) {
            BigDecimal bigPreviousResult = BigDecimal.valueOf(previousResult);
            BigDecimal bigValue = BigDecimal.valueOf(value);
            throw MondrianResource.instance().LimitExceededDuringCrossjoin.ex(
                    bigPreviousResult.multiply(bigValue), Long.MAX_VALUE);
        }

        return result;
    }

    // Bit calculation utils
    public static long compressTwoIntegers(int high, int low) {
        return ((long)high << Integer.SIZE) | low;
    }

    public static int extractHighBytes(long value) {
        return (int)(value >>> Integer.SIZE);
    }

    public static int extractLowBytes(long value) {
        return (int)(value & 0x00000000FFFFFFFFL);
    }

    public static int compareHighBytes(long value1, long value2) {
        return Integer.compare(extractHighBytes(value1), extractHighBytes(value2));
    }

    public static int compareLowBytes(long value1, long value2) {
        return Integer.compare(extractLowBytes(value1), extractLowBytes(value2));
    }


    // Sql result parsing utils
    public static Comparable getValueByKey(Map<Object, SqlStatement.Accessor> accessors, Integer key) throws SQLException {
        if (key == null) {
            return null;
        }

        return key >= 0 ? accessors.get(key).get() : null;
    }

    public static Comparable findFirstEquivalence(Comparable original, Comparable... alternatives) {
        if (original == null) {
            return null;
        }
        for (Comparable alternative : alternatives) {
            if (alternative == null) {
                continue;
            }
            if (original.equals(alternative)) {
                return alternative;
            }
        }
        return original;
    }

    public static Comparable[] findEquivalence(Comparable[] original, Comparable[] alternative) {
        return Arrays.equals(original, alternative) ? alternative : original;
    }

    public static RolapMemberBase makeLevelMember(
            RolapMember parent,
            String uniqueName,
            Comparable memberKey,
            Comparable memberName,
            Comparable memberValue,
            Comparable[] propertyValues) {
        RolapCubeLevel currentLevel = parent.getLevel().getChildLevel();

        memberKey = getNonNullSqlValue(memberKey);
        if (memberName == null) {
            memberName = memberKey;
        }
        if (memberValue == null) {
            memberValue = memberName;
        }
        final Larders.LarderBuilder builder = new Larders.LarderBuilder();
        builder.add(Property.NAME, memberName.toString());

        Comparable keyValue = memberKey;

        if (!parent.getLevel().isAll()) {
            List<Comparable> keyList = new ArrayList<>(parent.getKeyAsList());
            keyList.add(memberKey);
            memberKey = (Comparable)Util.flatList(keyList);
        }

        RolapMemberBase member = new RolapMemberBase(
                parent,
                currentLevel,
                memberKey,
                memberValue,
                Member.MemberType.REGULAR,
                uniqueName,
                builder.build());

        member.setOrdinal(parent.getOrdinal() + 1);

        member.setOrderKey(keyValue);

        if (propertyValues != null) {
            List<RolapProperty> properties = currentLevel.getAttribute().getProperties();
            for (int i = 0; i < properties.size(); i++) {
                if (propertyValues[i] == null) {
                    continue;
                }

                Property property = properties.get(i);
                member.setProperty(property, propertyValues[i]);
            }
        }
        return member;
    }

    public static RolapMemberBase makeLevelMember(
            RolapMember parent,
            Comparable memberKey,
            Comparable memberName,
            Comparable memberValue,
            Comparable[] propertyValues) {
        String uniqueName = RolapMemberBase.deriveUniqueName(
                parent,
                parent.getLevel().getChildLevel(),
                getNonNullSqlValue(memberKey).toString(),
                false);
        return makeLevelMember(parent, uniqueName, memberKey, memberName, memberValue, propertyValues);
    }

    public static boolean includesMember(Collection<RolapMember> memberCollection, RolapMember member) {
        for (; member != null; member = member.getParentMember()) {
            if (memberCollection.contains(member)) {
                return true;
            }
        }
        return false;
    }

    public static String getUniqueNameBuildingBase(RolapCubeHierarchy hierarchy) {
        List<? extends RolapCubeLevel> levels = hierarchy.getLevelList();
        if (levels.size() == 2) {
            return hierarchy.getUniqueName();
        } else {
            return levels.get(1).getUniqueName();
        }
    }

    public static Comparable getNonNullSqlValue(Comparable value) {
        return value != null ? value : RolapUtil.sqlNullValue;
    }

    public static SqlStatement.Type valueCategoryToSqlType(int category) throws IllegalArgumentException {
        switch (category) {
            case Category.Logical:
                return SqlStatement.Type.BOOLEAN;
            case Category.Member:
            case Category.Numeric:
                return SqlStatement.Type.DOUBLE;
            case Category.Integer:
                return SqlStatement.Type.INT;
            case Category.String:
                return SqlStatement.Type.STRING;
            default:
                throw new IllegalArgumentException("Category " + Category.instance().getDescription(category) + " cannot be cast to SQL type.");
        }
    }


    // Pattern groups extraction utils
    public static Member getMember(Exp exp) {
        assert exp instanceof MemberExpr;
        return ((MemberExpr)exp).getMember();
    }

    public static Member getAllMember(Exp exp) {
        Member member = getMember(exp);
        assert member.isAll();
        return member;
    }

    public static Hierarchy getHierarchy(Exp exp) {
        assert exp instanceof HierarchyExpr;
        return ((HierarchyExpr)exp).getHierarchy();
    }

    public static Level getLevel(Exp exp) {
        assert exp instanceof LevelExpr;
        return ((LevelExpr)exp).getLevel();
    }

    public static boolean isNonEmptySet(Exp exp) {
        return exp instanceof ResolvedFunCall
                && ((ResolvedFunCall)exp).getFunDef() instanceof SetFunDef
                && ((ResolvedFunCall)exp).getArgCount() > 0;
    }

    public static List<Member> getMembersFromSet(Exp exp) {
        assert isNonEmptySet(exp);
        Exp[] setElements = ((ResolvedFunCall)exp).getArgs();
        return Arrays.stream(setElements)
                .filter(element -> element instanceof MemberExpr)
                .map(element -> ((MemberExpr)element).getMember())
                .collect(Collectors.toList());
    }


    // Exp searching utils
    /**
     * Finds all members from an MDX AST in which Calculated Member nodes have been unpacked.
     */
    public static void extractAllBaseMembers(Exp exp, Set<Member> members) {
        Util.extractAllBaseMembers(exp, members);
    }

    /**
     * Find all members from an MDX AST.
     */
    public static void findLevelsFromExp(Exp exp, Set<Level> levels) {
        if (exp instanceof  MemberExpr) {
            levels.add(((MemberExpr) exp).getMember().getLevel());
            return;
        }
        if (exp instanceof LevelExpr) {
            levels.add(((LevelExpr) exp).getLevel());
            return;
        }
        if (exp instanceof ResolvedFunCall) {
            for (Exp arg : ((ResolvedFunCall) exp).getArgs()) {
                findLevelsFromExp(arg, levels);
            }
        }
        if (exp instanceof NamedSetExpr) {
            findLevelsFromExp(((NamedSetExpr) exp).getNamedSet().getExp(), levels);
        }
    }

    public static void findHierarchiesFromExp(Exp exp, Set<Hierarchy> hierarchies) {
        Set<Level> levels = new HashSet<>();
        Set<Member> members = new HashSet<>();
        findLevelsFromExp(exp, levels);
        RolapUtil.findMembersFromExp(exp, members);
        for (Level level : levels) {
            if (level instanceof RolapCubeLevel) {
                hierarchies.add(level.getHierarchy());
            }
        }
        for (Member member : members) {
            if (member instanceof RolapMember) {
                hierarchies.add(member.getHierarchy());
            }
        }

        if (exp instanceof HierarchyExpr) {
            hierarchies.add(((HierarchyExpr) exp).getHierarchy());
            return;
        }
        if (exp instanceof ResolvedFunCall) {
            for (Exp arg : ((ResolvedFunCall) exp).getArgs()) {
                findHierarchiesFromExp(arg, hierarchies);
            }
        }
        if (exp instanceof NamedSetExpr) {
            findHierarchiesFromExp(((NamedSetExpr) exp).getNamedSet().getExp(), hierarchies);
        }
    }


    // Exp processing utils
    /**
     * Replace nested formula, for example, [NamedSet] -> {[Member1], [Member2]}
     */
    public static Exp flattenNestedFormula(Exp exp) {
        if (exp instanceof NamedSetExpr) {
            return flattenNestedFormula(((NamedSetExpr) exp).getNamedSet().getExp());
        }
        if (exp instanceof ResolvedFunCall) {
            for (int i = 0; i < ((ResolvedFunCall) exp).getArgs().length; i++) {
                Exp arg = flattenNestedFormula(((ResolvedFunCall) exp).getArg(i));
                ((ResolvedFunCall) exp).replaceArg(arg, i);
            }
        } else if (exp instanceof MemberExpr) {
            Member member = ((MemberExpr) exp).getMember();
            if (member instanceof RolapCalculatedMember
                    && !(member instanceof RolapHierarchy.RolapCalculatedMeasure)) {
                exp = ((RolapCalculatedMember) ((MemberExpr) exp).getMember()).getFormula().getExpression();
                exp = flattenNestedFormula(exp);
            }
        }
        return exp;
    }

    public static Map<Level, Set<RolapMember>> splitMemberSetByLevel(Set<RolapMember> memberSet) {
        Map<Level, Set<RolapMember>> mapLevel2MemberSet = new HashMap<>();
        for (RolapMember currentMember : memberSet) {
            Level currentLevel = currentMember.getLevel();
            Set<RolapMember> currentLevelMemberSet = mapLevel2MemberSet.get(currentLevel);
            if (currentLevelMemberSet == null) {
                currentLevelMemberSet = new HashSet<>();
            }
            currentLevelMemberSet.add(currentMember);
            mapLevel2MemberSet.put(currentLevel, currentLevelMemberSet);
        }
        return mapLevel2MemberSet;
    }

    /**
     * This function is to generate set for helping generate sql condition statement in ExceptSlicerPattern and HybridSlicerPattern
     * @param exp : the expression to be resolved
     * @param resultToExclude : if the result of current expression is to be excluded (such as being the second argument of Except function)
     * @param toExclude : the set of RolapMember to be excluded in final result
     * @param fullSet : the fullset, not used yet
     * @return the RolapMember set generated from current expression
     * TODO: decompose this function to smaller functions and put them in a proper class
     */

   public static Set<RolapMember> generateSetFromExpr(Exp exp, boolean resultToExclude, Set<RolapMember> toExclude, Set<RolapMember> fullSet) {
        Set<RolapMember> result = new HashSet<>();
        if (exp instanceof MemberExpr) {
            result.add((RolapMember) ((MemberExpr) exp).getMember());
            return result;
        }
        if (exp instanceof NamedSetExpr) {
            exp = ((NamedSetExpr) exp).getNamedSet().getExp();
        }
        String funName = ((ResolvedFunCall) exp).getFunName();
        if (funName.contentEquals("{}")) {
            //simple member set
            for (Exp memberExpr : ((ResolvedFunCall) exp).getArgs()) {
                if (memberExpr instanceof MemberExpr) {
                    result.add((RolapMember) ((MemberExpr) memberExpr).getMember());
                } else if (memberExpr instanceof ResolvedFunCall) {
                    result.addAll(generateSetFromExpr(memberExpr, resultToExclude, toExclude, fullSet));
                }
            }
        } else if (funName.contentEquals("VisualTotals") || funName.contentEquals("Hierarchize")) {
            result.addAll(generateSetFromExpr(((ResolvedFunCall) exp).getArg(0), resultToExclude, toExclude, fullSet));
        } else if (funName.contentEquals("Descendants")) {
            //Descendants member set --> member, level
            RolapMember parentMember = null;
            Level toLevel = null;
            Hierarchy fromHierarchy = null;
            for (Exp arg : ((ResolvedFunCall) exp).getArgs()) {
                if (arg instanceof MemberExpr) {
                    parentMember = (RolapMember) ((MemberExpr) arg).getMember();
                } else if (arg instanceof LevelExpr) {
                    toLevel = ((LevelExpr) arg).getLevel();
                } else if (arg instanceof ResolvedFunCall && ((ResolvedFunCall) arg).getArg(0) instanceof HierarchyExpr) {
                    fromHierarchy = ((HierarchyExpr) ((ResolvedFunCall) arg).getArg(0)).getHierarchy();
                }
            }
            List<RolapMember> descendants = null;
            if (fromHierarchy == null) {
                try {
                    if (toLevel != null && parentMember != null) {
                        descendants = CacheManager.getCacheManager().getHierarchyCache().getMemberTree((RolapCubeHierarchy) toLevel.getHierarchy()).getChildrenMembers(parentMember.getUniqueName());
                    }
                } catch (ExecutionException | SQLException e) {
                    throw new MondrianException(e);
                }
                result.add(parentMember);
                assert descendants != null;
                result.addAll(descendants);
            } else {
                try {
                    if (toLevel != null) {
                        descendants = CacheManager.getCacheManager().getHierarchyCache().getMemberTree((RolapCubeHierarchy) toLevel.getHierarchy()).getMembersByLevelDepth(QueryEngineUtils.getLevelDepth(toLevel));
                    }
                } catch (ExecutionException | SQLException e) {
                    throw new MondrianException(e);
                }
                assert descendants != null;
                result.addAll(descendants);
            }

            //Descendants member set --> hierarchy, level

            try {
                if (parentMember != null) {
                    descendants = CacheManager.getCacheManager().getHierarchyCache().getMemberTree((RolapCubeHierarchy) toLevel.getHierarchy()).getChildrenMembers(parentMember.getUniqueName());
                }
            } catch (ExecutionException | SQLException e) {
                throw new MondrianException(e);
            }
            result.add(parentMember);
            assert descendants != null;
            result.addAll(descendants);
        } else if (funName.contentEquals("AllMembers")) {
            Exp levelExpr = ((ResolvedFunCall) exp).getArg(0);
            assert levelExpr instanceof LevelExpr;
            Level currentLevel = ((LevelExpr) levelExpr).getLevel();
            int levelDepth = 0;
            while (!currentLevel.isAll()) {
                currentLevel = currentLevel.getParentLevel();
                levelDepth++;
            }
            List<RolapMember> levelMembers = null;
            try {
                levelMembers = CacheManager.getCacheManager().getHierarchyCache().getMemberTree((RolapCubeHierarchy) currentLevel.getHierarchy()).getMembersByLevelDepth(levelDepth);
            } catch (ExecutionException | SQLException e) {
                throw new MondrianException(e);
            }
            assert levelMembers != null;
            result.addAll(levelMembers);
        } else if (funName.contentEquals("Members")) {
            Exp hierarchyOrLevelExpr = ((ResolvedFunCall) exp).getArg(0);
            if (hierarchyOrLevelExpr instanceof HierarchyExpr) {
                Hierarchy hierarchy = ((HierarchyExpr) hierarchyOrLevelExpr).getHierarchy();
                for (int i = 1; i < hierarchy.getLevelList().size(); i++) {
                    List<RolapMember> levelMembers = null;
                    try {
                        levelMembers = CacheManager.getCacheManager().getHierarchyCache().getMemberTree((RolapCubeHierarchy) hierarchy).getMembersByLevelDepth(i);
                    } catch (ExecutionException | SQLException e) {
                        throw new MondrianException(e);
                    }
                    assert levelMembers != null;
                    result.addAll(levelMembers);
                }
            }
        } else if (funName.contentEquals(":")) {
            assert ((ResolvedFunCall) exp).getArg(0) instanceof MemberExpr;
            assert ((ResolvedFunCall) exp).getArg(1) instanceof MemberExpr;
            Member startMember = ((MemberExpr) ((ResolvedFunCall) exp).getArg(0)).getMember();
            Member endMember = ((MemberExpr) ((ResolvedFunCall) exp).getArg(1)).getMember();
            Hierarchy hierarchy = startMember.getHierarchy();
            List<RolapMember> levelMembers = null;
            try {
                levelMembers = CacheManager.getCacheManager().getHierarchyCache().getMemberTree((RolapCubeHierarchy) hierarchy).getMembersBetween(startMember.getUniqueName(),
                        true,
                        endMember.getUniqueName(),
                        true);
            } catch (ExecutionException | SQLException e) {
                throw new MondrianException(e);
            }
            assert levelMembers != null;
            result.addAll(levelMembers);
        } else if (funName.contentEquals("AddCalculatedMembers")) {
            result.addAll(generateSetFromExpr(((ResolvedFunCall) exp).getArg(0), resultToExclude, toExclude, fullSet));
        } else if (funName.contentEquals("Except")) {
            Set<RolapMember> setFromArg0 = generateSetFromExpr(((ResolvedFunCall) exp).getArg(0), resultToExclude, toExclude, fullSet);
            Set<RolapMember> setFromArg1 = generateSetFromExpr(((ResolvedFunCall) exp).getArg(1), !resultToExclude, toExclude, fullSet);
            result.addAll(setFromArg0);
            result.removeAll(setFromArg1);
            if (resultToExclude) {
                toExclude.addAll(setFromArg0);
            } else {
                toExclude.addAll(setFromArg1);
            }
        } else if (funName.contentEquals("Union")) {
            Set<RolapMember> setFromArg0 = generateSetFromExpr(((ResolvedFunCall) exp).getArg(0), resultToExclude, toExclude, fullSet);
            Set<RolapMember> setFromArg1 = generateSetFromExpr(((ResolvedFunCall) exp).getArg(1), !resultToExclude, toExclude, fullSet);
            result.addAll(setFromArg0);
            result.addAll(setFromArg1);
            toExclude.removeAll(setFromArg0);
            toExclude.removeAll(setFromArg1);
        } else if (funName.contentEquals("Generate")) {
            Set<RolapMember> setFromArg0 = generateSetFromExpr(((ResolvedFunCall) exp).getArg(0), resultToExclude, toExclude, fullSet);
            result.addAll(applyRuleForGenerateFunction(((ResolvedFunCall) exp).getArg(1), fullSet, setFromArg0));
            if (resultToExclude) {
                toExclude.addAll(result);
            }
        }
        return result;
    }

    /**
     *
     * @param ruleExp : the second argument of generate function which determines the rule of constructing result set
     * @param fullSet : the fullset, not used yet
     * @param setFromArg0 : the RolapMember set from the first argument of generate function
     * @return the RolapMember set generated from current expression
     */
    public static Set<RolapMember> applyRuleForGenerateFunction(Exp ruleExp, Set<RolapMember> fullSet, Set<RolapMember> setFromArg0) {
        Set<RolapMember> result = new HashSet<>();
        assert ruleExp instanceof ResolvedFunCall;
        ResolvedFunCall rfc = (ResolvedFunCall) ruleExp;
        if (rfc.getFunName().contentEquals("{}")) {
            Exp arg0 = rfc.getArg(0);
            assert arg0 instanceof ResolvedFunCall;
            if (((ResolvedFunCall) arg0).getFunName().contentEquals("Ancestor")) {
                //{Ancestor([Hierarchy].CurrentMember, [Level])}
                assert ((ResolvedFunCall) arg0).getArg(1) instanceof LevelExpr;
                Level levelFromArg0 = ((LevelExpr) ((ResolvedFunCall) arg0).getArg(1)).getLevel();
                for (RolapMember currentMember0 : setFromArg0) {
                    Level currentLevel0 = currentMember0.getLevel();
                    if (currentLevel0.equals(levelFromArg0)) {
                        result.add(currentMember0);
                    } else {
                        RolapMember parentMember0 = currentMember0.getParentMember();
                        while (!parentMember0.getLevel().equals(levelFromArg0) && !parentMember0.getLevel().isAll()) {
                            parentMember0 = parentMember0.getParentMember();
                        }
                        if (parentMember0.getLevel().equals(levelFromArg0)) {
                            result.add(parentMember0);
                        }
                    }
                }
            } else if (((ResolvedFunCall) arg0).getFunName().contentEquals("IIf")) {
                //{IIf(([Hierarchy].CurrentMember.Level.Ordinal <= [Literal]),
                //  [Hierarchy].CurrentMember,
                //  Ancestor(
                //      [Hierarchy].CurrentMember,
                //      ([Hierarchy].CurrentMember.Level.Ordinal - [Literal])
                //   )
                // )}
                Exp[] args = ((ResolvedFunCall) arg0).getArgs();
                Exp condition = args[0];
                boolean lessThan = false;
                int levelDepth = 0;
                assert condition instanceof ResolvedFunCall;
                if (((ResolvedFunCall) condition).getFunName().contentEquals("<=")) {
                    lessThan = true;
                    assert ((ResolvedFunCall) condition).getArg(1) instanceof Literal;
                    levelDepth = ((Literal) ((ResolvedFunCall) condition).getArg(1)).getIntValue();
                }
                for (RolapMember member : setFromArg0) {
                    if (lessThan) {
                        if (member.getLevel().getDepth() <= levelDepth) {
                            result.add(member);
                        } else {
                            RolapMember parentMember0 = member;
                            for (int i = 0; i < levelDepth; i++) {
                                parentMember0 = parentMember0.getParentMember();
                            }
                            result.add(parentMember0);
                        }
                    }
                }

            }
        } else if (rfc.getFunName().contentEquals("Descendants")) {
            Set<RolapMember> parentMemberSet = null;
            Level toLevel = null;
            for (Exp arg :  rfc.getArgs()) {
                if (arg instanceof MemberExpr) {
                    if (parentMemberSet == null) {
                        parentMemberSet = new HashSet<>();
                    }
                    parentMemberSet.add((RolapMember) ((MemberExpr) arg).getMember());
                } else if (arg instanceof LevelExpr) {
                    toLevel = ((LevelExpr) arg).getLevel();
                } else if (arg instanceof ResolvedFunCall && ((ResolvedFunCall) arg).getFunName().contentEquals("CurrentMember")) {
                    parentMemberSet = setFromArg0;
                }
            }
            List<RolapMember> descendants = null;
            for (RolapMember parentMember : parentMemberSet) {
                try {
                    assert toLevel != null;
                    assert parentMember != null;
                    descendants = CacheManager.getCacheManager().getHierarchyCache().getMemberTree((RolapCubeHierarchy) toLevel.getHierarchy()).getChildrenMembers(parentMember.getUniqueName());
                } catch (ExecutionException | SQLException e) {
                    throw new MondrianException(e);
                }
                assert descendants != null;
                result.addAll(descendants);
            }
        } else if (rfc.getFunName().contentEquals("AddCalculatedMembers")) {
            result.addAll(applyRuleForGenerateFunction(rfc.getArg(0), fullSet, setFromArg0));
        }
        return result;
    }


    public static int getLevelDepth(Level level) {
        int levelDepth = 0;
        while(!level.isAll()) {
            levelDepth++;
            level = level.getParentLevel();
        }
        return levelDepth;
    }

    public static boolean isCurrentNode(String ip, String port) throws SocketException {
        String currentPort = SemanticConfig.getInstance().getMdxPort();
        if (!currentPort.equals(port)) {
            return false;
        }
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        NetworkInterface networkInterface;
        Enumeration<InetAddress> inetAddresses;
        InetAddress inetAddress;
        while (networkInterfaces.hasMoreElements()) {
            networkInterface = networkInterfaces.nextElement();
            inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                inetAddress = inetAddresses.nextElement();
                if (inetAddress != null && inetAddress instanceof Inet4Address) { // IPV4
                    String currentIp = inetAddress.getHostAddress();
                    if (currentIp.equals(ip) && currentPort.equals(port)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isLogicallySameLevel(RolapCubeLevel level1, RolapCubeLevel level2) {
        if (level1.isAll() || level2.isAll()) {
            return false;
        }
        int keyListLen1 = level1.getAttribute().getKeyList().size();
        int keyListLen2 = level2.getAttribute().getKeyList().size();
        RolapSchema.PhysColumn column1 = level1.getAttribute().getKeyList().get(keyListLen1 - 1);
        RolapSchema.PhysColumn column2 = level2.getAttribute().getKeyList().get(keyListLen2 - 1);
        return column1.equals(column2);
    }

    public static boolean isDescendantsOfOrSelf(Member target, Member toCheck) {
        return target.equals(toCheck) || isDescendantsOf(target, toCheck);
    }

    public static boolean isDescendantsOf(Member target, Member toCheck) {
        Member parent = toCheck.getParentMember();
        while (parent != null) {
            if (parent.equals(target)) {
                return true;
            }
            parent = parent.getParentMember();
        }
        return false;
    }

    public static boolean isAncestorsOfOrSelf(Member target, Member toCheck) {
        if (target.equals(toCheck)) {
            return true;
        }
        Member parent = target.getParentMember();
        while (parent != null) {
            if (parent.equals(toCheck)) {
                return true;
            }
            parent = parent.getParentMember();
        }
        return false;
    }

    /**
     * This method make visual total member for a certain target member according to childList in lowest level
     * @param targetMember the target member for which the visual total member is created
     * @param actualMemberList list of actual members that the visual total member represents
     */
    public static VisualTotalsFunDef.VisualTotalMember makeVisualTotalMemberByChildList(Member targetMember, Exp filterExp, List<Member> actualMemberList) {
        List<Member> currentLevelChildList = new ArrayList<>(actualMemberList);
        Member parentMember = currentLevelChildList.get(0).getParentMember();
        while (parentMember != null && parentMember.getDepth() >= targetMember.getDepth()) {
            Map<Member, List<Member>> mapVisualTotalMember2ChildList = new HashMap<>();
            for (Member child : currentLevelChildList) {
                List<Member> childList = mapVisualTotalMember2ChildList.get(child.getParentMember());
                if (childList == null) {
                    childList = new ArrayList<>();
                }
                childList.add(child);
                mapVisualTotalMember2ChildList.put(child.getParentMember(), childList);
            }

            //update child list for next level up
            currentLevelChildList.clear();
            for (Map.Entry<Member, List<Member>> currentEntry : mapVisualTotalMember2ChildList.entrySet()) {
                Exp currentExp = replaceAggregateFunArgs(filterExp, currentEntry.getValue());
                currentLevelChildList.add(new VisualTotalsFunDef.VisualTotalMember((RolapMember) currentEntry.getKey(), currentEntry.getKey().getName(), currentExp, currentEntry.getValue()));
            }
            parentMember = parentMember.getParentMember();
        }
        return (VisualTotalsFunDef.VisualTotalMember) currentLevelChildList.get(0);
    }

    /**
     * Replace the arguments of a filter expression to make visual total member
     * @param filterExp the filter expression which is like 'Aggregate({[MemberExpr], [MemberExpr]})'
     *                  or 'Aggregate(Distinct(StripCalculatedMembers({Descendants({[MemberExpr]}), Descendants({[MemberExpr]})})))'
     *                  or 'Aggregate(Distinct(StripCalculatedMembers({{[MemberExpr]}, {[MemberExpr]}})))'
     * @param argList the child list of visual total members
     */
    public static Exp replaceAggregateFunArgs(Exp filterExp, List<Member> argList) {
        ArrayDeque<Exp> visited = new ArrayDeque<>();
        Exp setExp = filterExp;
        while (setExp instanceof ResolvedFunCall
                && !(((ResolvedFunCall)setExp).getFunDef() instanceof SetFunDef)
                && ((ResolvedFunCall)setExp).getArgCount() > 0) {
            visited.push(setExp);
            setExp = ((ResolvedFunCall)setExp).getArg(0);
        }

        if (!(setExp instanceof ResolvedFunCall)) {
            return filterExp;
        }
        ResolvedFunCall setFunCall = (ResolvedFunCall)setExp;

        Exp[] newArgs = new Exp[argList.size()];
        int[] newArgTypes = new int[newArgs.length];
        for (int i = 0; i < argList.size(); i++) {
            newArgs[i] = replaceSingleMemberExp(setFunCall.getArg(0), argList.get(i));
            newArgTypes[i] = newArgs[i].getCategory();
        }
        Type setType;
        if (Utils.isCollectionEmpty(argList)) {
            setType = new SetType(MemberType.Unknown);
        } else if (newArgs[0] instanceof MemberExpr) {
            setType = new SetType(newArgs[0].getType());
        } else {
            setType = newArgs[0].getType();
        }

        ResolvedFunCall newFilterExp = new ResolvedFunCall(
                ((SetFunDef)setFunCall.getFunDef()).withNewArgTypes(newArgTypes),
                newArgs,
                setType);
        while (!visited.isEmpty()) {
            ResolvedFunCall currentExp = (ResolvedFunCall)visited.pop();
            FunDef funDef = currentExp.getFunDef();
            Exp[] args = new Exp[currentExp.getArgCount()];
            args[0] = newFilterExp;
            for (int i = 1; i < args.length; i++) {
                args[i] = currentExp.getArg(i);
            }
            Type type = currentExp.getType() instanceof SetType ? setType : currentExp.getType();
            newFilterExp = new ResolvedFunCall(funDef, args, type);
        }

        return newFilterExp;
    }

    private static Exp replaceSingleMemberExp(Exp exp, Member member) {
        if (exp instanceof ResolvedFunCall && ((ResolvedFunCall)exp).getFunDef() instanceof DescendantsFunDef) {
            ResolvedFunCall descendantsFunCall = (ResolvedFunCall)exp;
            if (descendantsFunCall.getArgCount() != 1) {
                return null;
            }

            Exp memberSetExp = descendantsFunCall.getArg(0);
            if (!(memberSetExp instanceof ResolvedFunCall)
                    || !(((ResolvedFunCall)memberSetExp).getFunDef() instanceof SetFunDef)
                    || ((ResolvedFunCall)memberSetExp).getArgCount() != 1) {
                return null;
            }

            MemberType newMemberType = new MemberType(member.getDimension(), member.getHierarchy(), member.getLevel(), member);
            SetType newSetType = new SetType(newMemberType);

            ResolvedFunCall memberSetFunCall = (ResolvedFunCall)memberSetExp;
            ResolvedFunCall newMemberSet = new ResolvedFunCall(
                    memberSetFunCall.getFunDef(),
                    new Exp[] {new MemberExpr(member)},
                    newSetType);

            return new ResolvedFunCall(
                    descendantsFunCall.getFunDef(),
                    new Exp[] {newMemberSet},
                    newSetType);
        } else if (exp instanceof ResolvedFunCall && ((ResolvedFunCall)exp).getFunDef() instanceof SetFunDef) {
            if (!isNonEmptySet(exp)) {
                return null;
            }
            ResolvedFunCall setFunCall = (ResolvedFunCall)exp;

            List<Member> members = getMembersFromSet(setFunCall);
            if (members.size() != 1) {
                return null;
            }

            return new ResolvedFunCall(
                    setFunCall.getFunDef(),
                    new Exp[] {new MemberExpr(member)},
                    new SetType(new MemberType(member.getDimension(), member.getHierarchy(), member.getLevel(), member)));
        } else if (exp instanceof MemberExpr) {
            return new MemberExpr(member);
        } else {
            return null;
        }
    }


    public static boolean contextConflictWithSlicer(Exp exp, Set<Hierarchy> slicerHierarchySet) {
        if (exp instanceof MemberExpr) {
            return false;
        } else if (exp instanceof ResolvedFunCall) {
            if (((ResolvedFunCall) exp).getFunDef().getName().equals("()")) {
                boolean containsMeasure = false;
                boolean contextConflicts = false;
                for (Exp arg : ((ResolvedFunCall) exp).getArgs()) {
                    if (arg instanceof ResolvedFunCall) {
                        return contextConflictWithSlicer(arg, slicerHierarchySet);
                    }
                    Type type = arg.getType();
                    if (type.getDimension() != null && type.getDimension().isMeasures()) {
                        containsMeasure = true;
                    }
                    if (type.getHierarchy() != null && slicerHierarchySet.contains(type.getHierarchy())) {
                        contextConflicts = true;
                    }
                    if (containsMeasure && contextConflicts) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static List<RolapCubeHierarchy> findSpecialFunctionInExp(Exp exp) {
        List<RolapCubeHierarchy> hierarchyList = new ArrayList<>();
        if (exp instanceof MemberExpr) {
            Member member = ((MemberExpr) exp).getMember();
            if (member instanceof RolapHierarchy.RolapCalculatedMeasure) {
                hierarchyList.addAll(findSpecialFunctionInExp(member.getExpression()));
                return hierarchyList;
            }
            return Collections.emptyList();
        } else if (exp instanceof ResolvedFunCall) {
            if (((ResolvedFunCall) exp).getFunName().equalsIgnoreCase("ParallelPeriod")
                || ((ResolvedFunCall) exp).getFunName().equalsIgnoreCase("CurrentMember")) {
                Exp arg0 = ((ResolvedFunCall) exp).getArg(0);
                hierarchyList.add((RolapCubeHierarchy) arg0.getType().getHierarchy());
            } else {
                for (Exp arg : ((ResolvedFunCall) exp).getArgs()) {
                    hierarchyList.addAll(findSpecialFunctionInExp(arg));
                }
            }
        } else if (exp instanceof NamedSetExpr){
            Exp namedSetExp = ((NamedSetExpr) exp).getNamedSet().getExp();
            hierarchyList.addAll(findSpecialFunctionInExp(namedSetExp));
        }
        return hierarchyList;
    }

}
