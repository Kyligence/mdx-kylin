package io.kylin.mdx.insight.common;

import io.kylin.mdx.insight.common.util.NetworkUtils;
import io.kylin.mdx.insight.common.util.Utils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import static io.kylin.mdx.insight.common.constants.SystemConstants.LOCAL_HOST;
import static io.kylin.mdx.insight.common.constants.SystemConstants.LOCAL_IP;

public class MdxInstance {

    private static class MDXInstanceHolder {
        private static final MdxInstance INSTANCE = new MdxInstance();
    }

    private static final SemanticConfig CONFIG = SemanticConfig.getInstance();

    private static final String PG_TYPE = "pg";

    @Getter
    private final String mdxNodeName;

    @Getter
    private final String sessionName;

    private MdxInstance() {
        this.mdxNodeName = generateMdxNodeName();
        this.sessionName = generateSessionName();
    }

    public static MdxInstance getInstance() {
        return MDXInstanceHolder.INSTANCE;
    }

    public String getDiscoverCatalogUrl(String project) {
        return CONFIG.getMdxProtocol() + "://" + CONFIG.getMdxHost() + ":" + CONFIG.getMdxPort() + CONFIG.getContextPath()
                + Utils.startWithoutSlash(CONFIG.getMdxServletPath()) + project;
    }

    private String generateMdxNodeName() {
        String hostIp = CONFIG.getMdxHost();
        if (StringUtils.isBlank(hostIp) || LOCAL_HOST.equals(hostIp) || LOCAL_IP.equals(hostIp)) {
            hostIp = NetworkUtils.getLocalIP();
        }
        return hostIp + ":" + CONFIG.getMdxPort();
    }

    private String generateSessionName() {
        boolean optimize = CONFIG.getBooleanValue("insight.semantic.cookie-optimize", true);
        if (CONFIG.isConvertorMock() || !optimize) {
            return "mdx_session";
        }

        String databaseType = CONFIG.getDatabaseType();
        String sessionBase;
        if (PG_TYPE.equalsIgnoreCase(databaseType)) {
            sessionBase = String.join("_", databaseType, CONFIG.getDatabaseIp(), CONFIG.getDatabasePort(),
                    CONFIG.getDatabaseName(), CONFIG.getPostgresqlSchema());
        } else {
            sessionBase = String.join("_", databaseType, CONFIG.getDatabaseIp(), CONFIG.getDatabasePort(),
                    CONFIG.getDatabaseName());
        }

        return "mdx_session_" + Utils.genMD5(sessionBase).substring(0, 8);
    }
}
