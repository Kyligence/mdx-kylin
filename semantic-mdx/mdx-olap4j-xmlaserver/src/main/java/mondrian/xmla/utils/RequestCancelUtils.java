package mondrian.xmla.utils;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.async.InterruptTask;
import io.kylin.mdx.insight.common.async.InterruptTaskExecutor;
import io.kylin.mdx.insight.core.manager.AsyncManager;
import io.kylin.mdx.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author liang.xu
 */
@Slf4j
public class RequestCancelUtils {

    private RequestCancelUtils() {
    }

    private static final SemanticConfig CONFIG = SemanticConfig.getInstance();

    private static final String DISCOVER_SESSION_URL = "http://{cluster}{contextPath}api/discover/{sessionId}";

    public static boolean existCancelCluster(String sessionId) {
        RestTemplate restTemplate = RestTemplateManager.getInstance().getRestTemplate();

        XmlaRequestContext context = XmlaRequestContext.getContext();
        String clusterUrl = DISCOVER_SESSION_URL.replace("{sessionId}", sessionId)
                .replace("{contextPath}", CONFIG.getContextPath());

        InterruptTaskExecutor taskList = new InterruptTaskExecutor(AsyncManager.getInstance().getCancelQueryAsyncService());
        for (String cluster : getClusters()) {
            taskList.submit(new InterruptTask(cluster) {
                @Override
                public void run() {
                    String cluster = get(FIRST);
                    String discoverSessionUrl = clusterUrl.replace("{cluster}", cluster);
                    ResponseEntity<String> responseEntity;
                    try {
                        responseEntity = restTemplate.getForEntity(discoverSessionUrl, String.class);
                    } catch (Exception e) {
                        return;
                    }
                    String responseBody = responseEntity.getBody();
                    if (responseBody == null) {
                        return;
                    }
                    log.debug("discover cancel session id url: {}, response: {}", discoverSessionUrl, responseBody);
                    if (responseBody.contains("true")) {
                        context.redirectMdx = cluster;
                        interruptAll();
                    }
                }
            });
        }
        try {
            taskList.execute();
        } catch (InterruptedException e) {
            // 不影响正常流程
        }
        return StringUtils.isNotBlank(context.redirectMdx);
    }

    private static String[] getClusters() {
        String clustersInfo = CONFIG.getClustersInfo();
        if (StringUtils.isBlank(clustersInfo)) {
            throw new SemanticException(ErrorCode.CLUSTER_NODES_NO_CONFIG);
        }
        return clustersInfo.split(",");
    }

    static class RestTemplateManager {

        private static final RestTemplateManager INSTANCE = new RestTemplateManager();

        private final RestTemplate restTemplate;

        private RestTemplateManager() {
            HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
            httpRequestFactory.setConnectionRequestTimeout(5000);
            httpRequestFactory.setConnectTimeout(5000);
            httpRequestFactory.setReadTimeout(5000);
            restTemplate = new RestTemplate(httpRequestFactory);
        }

        public static RestTemplateManager getInstance() {
            return INSTANCE;
        }

        public RestTemplate getRestTemplate() {
            return restTemplate;
        }
    }
}
