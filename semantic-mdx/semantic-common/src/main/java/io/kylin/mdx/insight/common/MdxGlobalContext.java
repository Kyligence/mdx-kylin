package io.kylin.mdx.insight.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MdxGlobalContext {
    private static final AtomicBoolean INIT_STATUS = new AtomicBoolean(true);

    private static final AtomicBoolean SYNC_STATUS = new AtomicBoolean(false);

    private static final AtomicBoolean FIRST_VERIFY = new AtomicBoolean(true);

    private static final ConcurrentHashMap<String, String> USER_TO_PWD_MAP = new ConcurrentHashMap<>();

    private static final AtomicBoolean METADATA_STATUS = new AtomicBoolean(true);

    public static String getPassword(String username) {
        return USER_TO_PWD_MAP.get(username);
    }

    public static void setPassword(String username, String password) {
        USER_TO_PWD_MAP.put(username, password);
    }

    public static boolean isInitStatus() {
        return INIT_STATUS.get();
    }

    public static boolean getAndSetInitStatus(boolean oldEnable, boolean newEnable) {
        return INIT_STATUS.compareAndSet(oldEnable, newEnable);
    }

    public static boolean isMetadataStatus() {
        return METADATA_STATUS.get();
    }

    public static boolean getAndSetMetadataStatus(boolean oldEnable, boolean newEnable) {
        return METADATA_STATUS.compareAndSet(oldEnable, newEnable);
    }

    public static boolean isSyncStatus() {
        return SYNC_STATUS.get();
    }

    public static boolean getAndSetSyncStatus(boolean oldEnable, boolean newEnable) {
        return SYNC_STATUS.compareAndSet(oldEnable, newEnable);
    }

    public static String getSyncStatus() {
        return SYNC_STATUS.get() ? "active" : "inactive";
    }

    public static boolean isFirstVerify() {
        return FIRST_VERIFY.get();
    }

    public static void setFirstVerify(boolean firstVerify) {
        FIRST_VERIFY.set(firstVerify);
    }
}
