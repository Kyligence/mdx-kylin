//package io.kylin.mdx.web.rejecter.rule;
//
//import com.alibaba.fastjson.JSON;
//import SemanticConfig;
//import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
//import io.kylin.mdx.insight.core.service.SemanticContext;
//import io.kylin.mdx.insight.core.support.SpringHolder;
//import io.kylin.mdx.insight.core.sync.DimensionCardinality;
//import CacheManager;
//import HierarchyCache;
//import HierarchyMemberTree;
//import BaseRejectRule;
//import lombok.extern.slf4j.Slf4j;
//import mondrian.mdx.HierarchyExpr;
//import mondrian.mdx.MemberExpr;
//import mondrian.mdx.NamedSetExpr;
//import mondrian.mdx.ResolvedFunCall;
//import mondrian.olap.*;
//import mondrian.olap4j.MondrianOlap4jCellSetAxisMetaData;
//import mondrian.olap4j.MondrianOlap4jCellSetMetaData;
//import mondrian.rolap.RolapCubeHierarchy;
//import mondrian.xmla.XmlaRequestContext;
//import org.olap4j.CellSetAxisMetaData;
//import org.olap4j.PreparedOlapStatement;
//import org.olap4j.metadata.NamedList;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * 控制参数：
// * #  insight.semantic.reject.dimension.cardinality
// * #  insight.mdx.mondrian.query.filter-pushdown.in-clause-max-size
// */
//@Slf4j
//public class HighCardinalityRule extends BaseRejectRule {
//
//    private final SemanticConfig semanticConfig = SemanticConfig.getInstance();
//
//    private final boolean debug = semanticConfig.getBooleanValue("insight.semantic.reject.high-cardinality.debug", false);
//
//    @Override
//    public String name() {
//        return "high_cardinality";
//    }
//
//    @Override
//    public boolean reject(PreparedOlapStatement statement) {
//        CardinalityContext context = new CardinalityContext();
//        try {
//            context.table2Databases = getDatabaseTableMap();
//            MondrianOlap4jCellSetMetaData metaData = (MondrianOlap4jCellSetMetaData) statement.getMetaData();
//            NamedList<CellSetAxisMetaData> cells = metaData.getAxesMetaData();
//            for (CellSetAxisMetaData cell : cells) {
//                QueryAxis queryAxis = ((MondrianOlap4jCellSetAxisMetaData) cell).getQueryAxis();
//                extractDimension(context, queryAxis.getExp());
//            }
//            return checkCardinality(context);
//        } catch (Exception e) {
//            log.error("Failed to check mdx dimension information.", e);
//            return false;
//        }
//    }
//
//    /**
//     * 从 Exp 抽取到维度字符串
//     */
//    protected void extractDimension(CardinalityContext context, Exp expression) {
//        Exp[] args = ((ResolvedFunCall) expression).getArgs();
//        for (Exp arg : args) {
//            if (arg instanceof ResolvedFunCall) {
//                extractDimension(context, arg);
//            }
//
//            // named set
//            if (arg instanceof NamedSetExpr) {
//                Exp nameSetExp = ((NamedSetExpr) arg).getNamedSet().getExp();
//                if (nameSetExp instanceof ResolvedFunCall) {
//                    for (Exp nameSetArg : ((ResolvedFunCall) nameSetExp).getArgs()) {
//                        if (nameSetArg instanceof ResolvedFunCall) {
//                            extractDimension(context, nameSetArg);
//                        }
//                    }
//                }
//            }
//
//            // hierarchy
//            if (arg instanceof HierarchyExpr) {
//                extractHierarchy(context, arg);
//            }
//
//            // member
//            if (arg instanceof MemberExpr) {
//                Member member = ((MemberExpr) arg).getMember();
//                if (member.isMeasure()) {
//                    continue;
//                }
//                extractHierarchy(context, arg);
//            }
//        }
//    }
//
//    protected void extractHierarchy(CardinalityContext context, Exp expression) {
//        Hierarchy hierarchy = expression.getType().getHierarchy();
//        if (!(hierarchy instanceof RolapCubeHierarchy)) {
//            return;
//        }
//        RolapCubeHierarchy cubeHierarchy = (RolapCubeHierarchy) hierarchy;
//        if (cubeHierarchy.getLevelList().size() > 2) {
//            context.hierarchies.add(cubeHierarchy);
//            return;
//        }
//        String dimensionName = hierarchy.getUniqueName();
//        String table = dimensionName.split("]\\.\\[")[0].substring(1);
//        String databaseName = context.table2Databases.get(table);
//        context.dimensions.add("[" + databaseName + "]." + dimensionName);
//    }
//
//    protected boolean checkCardinality(CardinalityContext context) {
//        XmlaRequestContext mdxContext = XmlaRequestContext.getContext();
//        String project = mdxContext.currentProject;
//        String datasetName = mdxContext.currentCatalog;
//        String queryId = mdxContext.runningStatistics.queryID;
//        if (debug) {
//            log.info("[{}] contain dimension : {}", queryId, JSON.toJSONString(context.dimensions));
//            log.info("[{}] contain hierarchy : {}", queryId, JSON.toJSONString(context.hierarchies.stream()
//                    .map(HierarchyBase::getUniqueName).collect(Collectors.toList())));
//        }
//
//        Map<String, Long> cardinalityMap = DimensionCardinality.getCardinalityMap(project, datasetName);
//        if (debug) {
//            log.info("[{}] based cardinality : {}", queryId, JSON.toJSONString(cardinalityMap));
//        }
//        long cardinalitySum = 1L;
//        long threshold = semanticConfig.getDimensionHighCardinalitySize();
//        // 优先判断普通维度
//        for (String dimension : context.dimensions) {
//            long cardinality = cardinalityMap.getOrDefault(dimension, 0L);
//            if (cardinality > 0) {
//                cardinalitySum *= cardinality;
//            }
//            if (cardinalitySum > threshold) {
//                return true;
//            }
//        }
//        // 自定义维度通过 member tree 加载基数
//        HierarchyCache hierarchyCache = CacheManager.getCacheManager().getHierarchyCache();
//        for (RolapCubeHierarchy hierarchy : context.hierarchies) {
//            try {
//                HierarchyMemberTree tree = hierarchyCache.getMemberTree(hierarchy);
//                int cardinality = tree.getMembersCountByLevelDepth(hierarchy.getLevelList().size() - 1);
//                if (cardinality > 0) {
//                    cardinalitySum *= cardinality;
//                }
//                if (debug) {
//                    log.info("[{}] based hierarchy {} : {}", queryId, hierarchy.getName(), cardinality);
//                }
//                if (cardinalitySum > threshold) {
//                    return true;
//                }
//            } catch (Exception e) {
//                log.error("{} get hierarchy cache cause exception.", name(), e);
//                return false;
//            }
//        }
//        return false;
//    }
//
//    /**
//     * 获取 table -> database
//     */
//    protected Map<String, String> getDatabaseTableMap() {
//        XmlaRequestContext context = XmlaRequestContext.getContext();
//        SemanticContext semanticContext = SpringHolder.getBean(SemanticContext.class);
//        SemanticDataset semanticDataset = semanticContext.getSemanticDataset(context.getQueryUser(), context.currentProject, context.currentCatalog);
//        if (semanticDataset == null) {
//            return Collections.emptyMap();
//        }
//        HashMap<String, String> databaseTableMap = new HashMap<>();
//        for (SemanticDataset.AugmentedModel augmentedModel : semanticDataset.getModels()) {
//            for (SemanticDataset.AugmentedModel.AugmentDimensionTable augmentDimensionTable : augmentedModel.getDimensionTables()) {
//                databaseTableMap.put(augmentDimensionTable.getAlias(), augmentDimensionTable.getActualTable().split("\\.")[0]);
//            }
//        }
//        return databaseTableMap;
//    }
//
//    public static class CardinalityContext {
//
//        // 输入信息
//
//        public Map<String, String> table2Databases;
//
//        // 抽取信息
//
//        /**
//         * 普通维度信息
//         */
//        public Set<String> dimensions = new HashSet<>();
//
//        /**
//         * 用户定义维度信息
//         */
//        public Set<RolapCubeHierarchy> hierarchies = new HashSet<>();
//
//    }
//
//}
