
package mondrian.xmla;

import com.alibaba.ttl.TransmittableThreadLocal;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.ExceptionUtils;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class XmlaRequestContext {

    public static TransmittableThreadLocal<XmlaRequestContext> localContext = new TransmittableThreadLocal<>();

    public Map<String, String> parameters = new HashMap<>();

    public RunningStatistics runningStatistics = new RunningStatistics();

    public Locale locale;

    @Setter
    public String currentUser;

    public String delegateUser;

    public boolean invalidPassword;

    public String currentProject;

    public String currentCatalog;

    public String mdxQuery;

    public String clientType;

    public boolean fromGateway;

    public boolean useMondrian;

    public String executorClass;

    public boolean hasRefreshedSchema;

    public boolean excelClient;

    public QueryPage queryPage;

    public MdxRewriter mdxRewriter;

    public String mdxQueryRewritten;

    public MdxOptimizer mdxOptimizer;

    public String mdxQueryOptimized;

    public MdxRejecter mdxRejecter;

    public String redirectMdx;

    public String errorMsg;

    public boolean skipValidateMember;

    public boolean doValidateCM;

    public boolean tableauFlag;

    public boolean filterRowLimitFlag;

    public boolean compactResult;

    public boolean errorFilled;

    public boolean isExpandingFilterInExcel;

    public boolean calculatedCellCountPerIterationNonIncreasing = true;

    public boolean notLogRequest;

    public boolean debugMode;

    public XmlaRequestContext() {
        localContext.set(this);
    }

    public static XmlaRequestContext getContext() {
        return localContext.get();
    }

    public static XmlaRequestContext newContext() {
        return new XmlaRequestContext();
    }

    public void clear() {
        localContext.remove();
    }

    public void addParameters(Map<String, String> properties) {
        parameters.putAll(properties);
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * 获取登录用户名称
     */
    public String getLoginUser() {
        return currentUser;
    }

    /**
     * 获取查询用户名称
     */
    public String getQueryUser() {
        return delegateUser != null ? delegateUser : currentUser;
    }

    public String getApplication() {
        if (clientType != null && !"".equals(clientType)) {
            if (ClientType.MSOLAP.equals(clientType)) {
                if (tableauFlag) {
                    return "Tableau";
                } else {
                    return "Excel";
                }
            } else if (ClientType.XMLA_CONNECT.equals(clientType)) {
                return "Arquery";
            }
        }
        return clientType;
    }

    public boolean shouldPreserveRegularFormat() {
        return SemanticConfig.getInstance().isFormatStringDefaultValueForceReturned()
                || excelClient
                || ClientType.MICRO_STRATEGY.equals(getApplication());
    }

    public static class QueryPage {
        public int queryStart;

        public int queryEnd;

        public int pageSize;

        public int startPage;

        public int endPage;

        public int pageStart;

        public int pageEnd;

        public boolean inOnePage;
    }

    public static class ExecuteSql {
        public String sql;
        public Long runTime;
        private boolean cacheUsed;
        private String kylinQueryId;
        private int resultRows = -1;

        public ExecuteSql(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }

        public void setRunTime(Long runTime) {
            this.runTime = runTime;
        }

        public Long getRunTime() {
            return runTime;
        }

        public void setCacheUsed(boolean cacheUsed) {
            this.cacheUsed = cacheUsed;
        }

        public boolean getCacheUsed() {
            return this.cacheUsed;
        }

        public void setKeQueryId(String kylinQueryId) {
            this.kylinQueryId = kylinQueryId;
        }

        public String getKylinQueryId() {
            return kylinQueryId;
        }

        public void setResultRows(int resultRows) {
            this.resultRows = resultRows;
        }

        public int getResultRows() {
            return resultRows;
        }

        public String getResultRowsStr() {
            if (resultRows < 0) {
                return "-";
            } else {
                return String.valueOf(resultRows);
            }
        }
    }

    public class RunningStatistics {

        public String queryID;
        public long start;
        public Long calcAxesTime;
        public Long calcCellValueTime;
        public Long createRolapResultTime;
        public Long unparseMultiDimDatasetTime;
        public Long marshallSoapMessageTime;
        public Map<Object, ExecuteSql> mapExtendExecuteSql = new ConcurrentHashMap<>();
        public long mdxRunTotalTime;
        public long cellRequestNum;
        public int networkPackage;
        public boolean timeout;
        public boolean success;
        public boolean mdxTimeout;
        public Long olapLayoutTime;
        public Long aggQueriesConstructionTime;
        public Long aggQueriesExecutionTime;
        public Long otherQueryEngineResultConstructionTime;
        public Long hierarchyLoadTime;
        public Long beforeConnectionTime;
        public Long connectionTime;
        public boolean gatewayUsed;
        public String datasetName;
        public long marshallSoapMessageTimeStart;

        public void checkException(Throwable t) {
            String msg = ExceptionUtils.getRootCause(t);
            if (msg != null && msg.contains("Query timeout")) {
                timeout = true;
            }
        }

        public void setSqlCacheUse(Object sqlExtend, boolean cacheUsed) {
            ExecuteSql executeSql = getExecuteSql(sqlExtend);
            executeSql.setCacheUsed(cacheUsed);
        }

        public void setSqlRunTime(Object sqlExtend, Long runTime) {
            ExecuteSql executeSql = getExecuteSql(sqlExtend);
            executeSql.setRunTime(runTime);
        }

        public void setKeQueryId(Object sqlExtend, String kylinQueryId) {
            ExecuteSql executeSql = getExecuteSql(sqlExtend);
            executeSql.setKeQueryId(kylinQueryId);
        }

        public void setResultRows(Object sqlExtend, int resultRows) {
            ExecuteSql executeSql = getExecuteSql(sqlExtend);
            executeSql.setResultRows(resultRows);
        }

        private ExecuteSql getExecuteSql(Object sqlExtend) {
            ExecuteSql executeSql = mapExtendExecuteSql.get(sqlExtend);
            if (executeSql == null) {
                String sql = sqlExtend.toString();
                executeSql = new ExecuteSql(sql);
                mapExtendExecuteSql.put(sqlExtend, executeSql);
                throw new RuntimeException("Get ExecuteSql failed!");
            }
            return executeSql;
        }

        public boolean getMdxCacheUsed() {
            if (!success) {
                return false;
            }
            if (mapExtendExecuteSql.size() == 0) {
                return true;
            }
            for (Map.Entry<Object, ExecuteSql> entry : mapExtendExecuteSql.entrySet()) {
                if (!entry.getValue().cacheUsed) {
                    return false;
                }
            }
            return true;
        }

        public String getTimeStr(Long t) {
            if (t == null) {
                return "-";
            } else {
                return t + "ms";
            }
        }

        public String getReportString() {
            if (mdxQuery == null || mdxQuery.length() == 0) {
                return null;
            }

            String ls = System.getProperty("line.separator");
            StringBuilder sb = new StringBuilder();
            sb.append(ls);
            sb.append("************************* MDX Running Statistics **************************").append(ls);
            sb.append("MDX Query ID: ").append(queryID).append(ls);
            sb.append("MDX: ").append(mdxQuery).append(ls);
            sb.append(ls);
            sb.append("Total Execution time: ").append(getTimeStr(mdxRunTotalTime)).append(ls);
            sb.append("User: ").append(currentUser).append(ls);
            if (delegateUser != null) {
                sb.append("Delegate: ").append(delegateUser).append(ls);
            }
            sb.append("Success: ").append(success).append(ls);
            sb.append("Project: ").append(currentProject).append(ls);
            sb.append("Dataset: ").append(currentCatalog).append(ls);
            sb.append("Application: ").append(getApplication()).append(ls);
            sb.append("MDX Cache Used: ").append(getMdxCacheUsed()).append(ls);
            sb.append("Other Used: ").append(!useMondrian).append(ls);
            sb.append("Gateway Used: ").append(gatewayUsed).append(ls);
            sb.append("Network Package: ").append(networkPackage).append(ls);
            sb.append("Timeout: ").append(timeout).append(ls);
            sb.append("Message: ").append(errorMsg).append(ls);
            sb.append("Before Connection: ").append(getTimeStr(beforeConnectionTime)).append(ls);
            sb.append("Connection: ").append(getTimeStr(connectionTime)).append(ls);

            if (hierarchyLoadTime != null) {
                sb.append("Hierarchies load: ").append(getTimeStr(hierarchyLoadTime)).append(ls);
                hierarchyLoadTime = 0L;
            }

            int sqlExeCount = mapExtendExecuteSql.size();
            if (sqlExeCount > 0) {
                sb.append("SQL Execution: ").append(sqlExeCount).append(" sql executed, respectively, ");
                int i = 1;
                StringBuilder bufTemp = new StringBuilder();
                Iterator<Map.Entry<Object, ExecuteSql>> iterator = mapExtendExecuteSql.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Object, ExecuteSql> entry = iterator.next();
                    ExecuteSql executeSql = entry.getValue();
                    sb.append(getTimeStr(executeSql.runTime)).append(", ");
                    bufTemp.append("SQL_").append(i).append(": ").append(executeSql.sql).append(ls);
                    bufTemp.append("SQL_").append(i).append(" Execution Time: ").append(getTimeStr(executeSql.runTime)).append(ls);
                    bufTemp.append("SQL_").append(i).append(" Cache Used: ").append(executeSql.cacheUsed).append(ls);
                    bufTemp.append("SQL_").append(i).append(" Result Rows: ").append(executeSql.getResultRowsStr()).append(ls);
                    bufTemp.append("SQL_").append(i).append(" KYLIN Query Id: ").append(executeSql.getKylinQueryId()).append(ls);
                    bufTemp.append(ls);
                    i ++;
                }
                sb.delete(sb.length() - 2, sb.length());
                sb.append(ls).append(ls);
                sb.append(bufTemp);
            } else {
                sb.append("SQL Execution: 0 sql executed").append(ls).append(ls);
            }

            if (useMondrian) {
                sb.append("Calculate Axes : ").append(getTimeStr(calcAxesTime)).append(ls);
                sb.append("Calculate Cell : ").append(getTimeStr(calcCellValueTime)).append(ls);
                sb.append("Calculate CellRequest num : ").append(success ? cellRequestNum : "-").append(ls);
                sb.append("Create RolapResult : ").append(success ? getTimeStr(createRolapResultTime) : "-").append(ls);
            }
            sb.append("Create MultiDimensional Dataset: ").append(success ? getTimeStr(unparseMultiDimDatasetTime) : "-").append(ls);
            sb.append("Marshall Soap Message: ").append(success ? getTimeStr(marshallSoapMessageTime) : "-").append(ls);
            sb.append("********************************************************************");
            return sb.toString();
        }

        public Map<String, Object> getMdxQuery() {
            Map<String, Object> mapMdxQuery = new HashMap<>();

            // truncate error message when more than text length
            if (StringUtils.isNotBlank(errorMsg) && errorMsg.length() > 65535) {
                errorMsg = errorMsg.substring(0, 65535);
            }

            mapMdxQuery.put("mdxQueryId", queryID);
            mapMdxQuery.put("mdxText", mdxQuery);
            mapMdxQuery.put("start", start);
            mapMdxQuery.put("totalExecutionTime", mdxRunTotalTime);
            mapMdxQuery.put("username", getQueryUser());
            mapMdxQuery.put("success", success);
            mapMdxQuery.put("project", currentProject);
            mapMdxQuery.put("application", getApplication());
            mapMdxQuery.put("mdxCacheUsed", getMdxCacheUsed());
            mapMdxQuery.put("beforeConnectionTime", beforeConnectionTime);
            mapMdxQuery.put("connectionTime", connectionTime);
            mapMdxQuery.put("hierarchyLoadTime", hierarchyLoadTime);
            mapMdxQuery.put("olapLayoutTime", olapLayoutTime);
            mapMdxQuery.put("aggQueriesConstructionTime", aggQueriesConstructionTime);
            mapMdxQuery.put("aggQueriesExecutionTime", aggQueriesExecutionTime);
            mapMdxQuery.put("otherQueryEngineResultConstructionTime", otherQueryEngineResultConstructionTime);
            mapMdxQuery.put("networkPackage", networkPackage);
            mapMdxQuery.put("timeout", timeout);
            mapMdxQuery.put("message", errorMsg);
            mapMdxQuery.put("calculateAxes", calcAxesTime);
            mapMdxQuery.put("calculateCell", calcCellValueTime);
            mapMdxQuery.put("calculateCellRequestNum", cellRequestNum);
            mapMdxQuery.put("createRolapResult", createRolapResultTime);
            mapMdxQuery.put("createMultiDimensionalDataset", unparseMultiDimDatasetTime);
            mapMdxQuery.put("marshallSoapMessage", marshallSoapMessageTime);
            mapMdxQuery.put("otherUsed", !useMondrian);
            mapMdxQuery.put("datasetName", currentCatalog == null ? datasetName : currentCatalog);

            return mapMdxQuery;
        }

        public List<Map<String, Object>> getSqlQueryList() {
            List<Map<String, Object>> sqlQueryList = new ArrayList<>();
            for (Map.Entry<Object, ExecuteSql> entry : mapExtendExecuteSql.entrySet()) {
                ExecuteSql executeSql = entry.getValue();
                Map<String, Object> mapSqlQuery = new HashMap<>();
                mapSqlQuery.put("mdxQueryId", queryID);
                if (executeSql == null) {
                    return sqlQueryList;
                }
                mapSqlQuery.put("sqlText", executeSql.getSql());
                mapSqlQuery.put("sqlExecutionTime", executeSql.getRunTime());
                mapSqlQuery.put("sqlCacheUsed", executeSql.getCacheUsed());
                mapSqlQuery.put("kylinQueryId", executeSql.getKylinQueryId());
                mapSqlQuery.put("resultRows", executeSql.getResultRows());
                if (null == executeSql.getRunTime()) {
                    mapSqlQuery.put("execStatus", false);
                } else {
                    mapSqlQuery.put("execStatus", true);
                }
                sqlQueryList.add(mapSqlQuery);
            }
            return sqlQueryList;
        }
    }

    public interface Parameter {

        /**
         * 指定精简结果（非压缩），默认格式化输出
         */
        String NEED_COMPACT_RESULT = "needCompactResult";

        /**
         * 指定使用 Mondrian 引擎，默认 Mondrian
         */
        String USE_MONDRIAN_ENGINE = "useMondrianEngine";

        /**
         * 指定是否优化 MDX 语句，默认 true
         */
        String ENABLE_OPTIMIZE_MDX = "enableOptimizeMDX";

        /**
         * 指定是否计算 Total，默认 true
         */
        String NEED_CALCULATE_TOTAL = "needCalculateTotal";

        /**
         * 指定查询启用 DEBUG 模式，默认 false
         */
        String ENABLE_DEBUG_MODE = "enableDebugMode";

    }

    public interface ClientType {

        String SMARTBI = "SmartBI";

        String MSOLAP = "MSOLAP";

        String XMLA_CONNECT = "Xmla-Connect";

        String POWERBI = "PowerBI";

        String POWERBI_DESKTOP = "PowerBI Desktop";

        String MICRO_STRATEGY = "MicroStrategy";

        String OLAP4J = "Olap4j";

        String UNKNOWN = "Unknown";

        static String fromUserAgent(String userAgent) {
            String clientType;
            if (userAgent.startsWith("Java")) {
                clientType = XmlaRequestContext.ClientType.SMARTBI;
            } else if (userAgent.startsWith("gSOAP")) {
                clientType = XmlaRequestContext.ClientType.XMLA_CONNECT;
            } else if (userAgent.startsWith("XmlaClient")) {
                clientType = XmlaRequestContext.ClientType.POWERBI;
            } else if (userAgent.startsWith("ADOMD.NET")) {
                clientType = XmlaRequestContext.ClientType.POWERBI_DESKTOP;
            } else if (userAgent.startsWith("MicroStrategy")) {
                clientType = XmlaRequestContext.ClientType.MICRO_STRATEGY;
            } else if (userAgent.startsWith("Olap4j")) {
                clientType = XmlaRequestContext.ClientType.OLAP4J;
            } else if (userAgent.startsWith("MSOLAP")) {
                clientType = XmlaRequestContext.ClientType.MSOLAP;
            } else {
                clientType = XmlaRequestContext.ClientType.UNKNOWN;
            }
            return clientType;
        }

    }

}
