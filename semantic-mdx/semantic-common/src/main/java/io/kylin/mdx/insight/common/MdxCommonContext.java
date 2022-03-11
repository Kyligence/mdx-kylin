package io.kylin.mdx.insight.common;

import io.kylin.mdx.insight.common.util.Utils;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Data
@RequiredArgsConstructor
public class MdxCommonContext {

    /**
     * The global ID of every HTTP request.
     */
    private final String traceId;

    /**
     * The timeout of MDX query related threads.
     */
    private final long queryMonitoringTimeout;

    /**
     * The locale defined in headers of HTTP requests or properties of XML/A requests.
     */
    private Locale currentLocale = Utils.DEFAULT_LOCALE;
}
