package io.kylin.mdx.insight.common;

import com.alibaba.ttl.TransmittableThreadLocal;
import io.kylin.mdx.insight.common.util.UUIDUtils;
import io.kylin.mdx.insight.common.util.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
public class MdxContext extends MdxGlobalContext {
    public static final TransmittableThreadLocal<MdxCommonContext> MDX_COMMON_CONTEXT = new TransmittableThreadLocal<>();

    public static void setMdxContext(MdxCommonContext mdxContext) {
        MDX_COMMON_CONTEXT.set(mdxContext);
    }

    public static MdxCommonContext getMdxContext() {
        return MDX_COMMON_CONTEXT.get();
    }

    public static void newMdxContext() {
        MDX_COMMON_CONTEXT.set(new MdxCommonContext(UUIDUtils.randomUUID(), SemanticConfig.getInstance().getQueryTimeout()));
    }

    public static Locale getCurrentLocale() {
        MdxCommonContext mdxCommonContext = MDX_COMMON_CONTEXT.get();
        if (mdxCommonContext == null) {
            return Utils.DEFAULT_LOCALE;
        }
        return mdxCommonContext.getCurrentLocale();
    }

    public static String getCurrentLanguage() {
        String currentLanguage = getCurrentLocale().getLanguage();
        return Locale.CHINESE.getLanguage().equals(currentLanguage) ? "cn" : currentLanguage;
    }

    /**
     * Try to fetch the trace ID from {@link MdxContext#MDX_COMMON_CONTEXT}.
     * If fetching failed, generate a new one.
     *
     * @return The stored or generated trace ID
     */
    public static String getTraceId() {
        MdxCommonContext mdxCommonContext = MDX_COMMON_CONTEXT.get();
        if (mdxCommonContext == null) {
            return UUIDUtils.randomUUID();
        } else {
            return mdxCommonContext.getTraceId();
        }
    }
}
