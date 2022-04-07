package io.kylin.mdx.insight.core.support;

import io.kylin.mdx.insight.core.entity.KylinPermission;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PermissionUtils {

    private static final Set<String> ACL_ADMIN = new HashSet<>(Arrays.asList(
            KylinPermission.GLOBAL_ADMIN.name(),
            KylinPermission.ADMINISTRATION.name()
    ));

    private static final Set<String> ACL_QUERY = new HashSet<>(Arrays.asList(
            KylinPermission.GLOBAL_ADMIN.name(),
            KylinPermission.ADMINISTRATION.name(),
            KylinPermission.MANAGEMENT.name(),
            KylinPermission.OPERATION.name(),
            KylinPermission.READ.name()
    ));

    public static boolean hasAdminPermission(String accessInfo) {
        return ACL_ADMIN.contains(accessInfo);
    }

    public static boolean hasQueryPermission(String accessInfo) {
        return ACL_QUERY.contains(accessInfo);
    }

}
