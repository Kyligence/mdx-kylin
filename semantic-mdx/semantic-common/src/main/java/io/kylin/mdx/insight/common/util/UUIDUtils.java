package io.kylin.mdx.insight.common.util;

import java.util.Random;

public class UUIDUtils {

    private static final ThreadLocal<Random> RANDOM = ThreadLocal.withInitial(Random::new);

    public static String randomUUID() {
        Random random = RANDOM.get();
        return String.format("%08x-%04x-%04x-%04x-%012x",
                random.nextInt(),
                random.nextInt() >>> 16,
                random.nextInt() >>> 16,
                random.nextInt() >>> 16,
                random.nextLong() >>> 16);
    }

}
