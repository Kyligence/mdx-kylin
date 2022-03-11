package io.kylin.mdx.insight.common.async;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class BatchTaskTest {

    @Test
    public void execute() throws InterruptedException {
        AsyncService asyncService = new AsyncService();
        BatchTaskExecutor executor = new BatchTaskExecutor(asyncService);
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            executor.submit(new ParamsTask(i) {
                @Override
                public void run() {
                    count.getAndIncrement();
                    if (get(FIRST, Integer.class) == 5) {
                        throw new RuntimeException();
                    }
                }
            });
        }
        boolean result = executor.execute();
        Assert.assertTrue(result);
        Assert.assertEquals(10, count.get());
        asyncService.shutdownNow();
    }

    @Test
    public void executeWithThis() throws InterruptedException {
        AsyncService asyncService = new AsyncService();
        BatchTaskExecutor executor = new BatchTaskExecutor(asyncService);
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            executor.submit(new ParamsTask(i) {
                @Override
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    if (threadName.startsWith("Async")) {
                        count.getAndIncrement();
                    }
                }
            });
        }
        boolean result = executor.executeWithThis();
        Assert.assertTrue(result);
        Assert.assertEquals(9, count.get());
        asyncService.shutdownNow();
    }

}
