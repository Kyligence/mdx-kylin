package io.kylin.mdx.insight.common.util;

import io.kylin.mdx.insight.common.async.AsyncService;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UUIDUtilsTest {

    public static void main(String[] args) throws InterruptedException {
        AsyncService service = new AsyncService(10);
        Map<String, Boolean> ids = new ConcurrentHashMap<>();
        for (int i = 0; i < 10; i++) {
            service.submit(() -> {
                for (int i1 = 0; i1 < 100000; i1++) {
                    String id = UUIDUtils.randomUUID();
                    if (ids.containsKey(id)) {
                        System.out.println("ID 重复 : " + id);
                    }
                    ids.put(id, true);
                }
            });
        }
        service.awaitLimit(0);
        System.out.println("ID 总数 : " + ids.size());
        service.shutdown();
    }

    @Test
    public void randomUUID() {
        UUIDUtils.randomUUID();
    }

}