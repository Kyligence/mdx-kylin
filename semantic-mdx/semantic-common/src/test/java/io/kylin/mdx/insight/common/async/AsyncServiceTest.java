package io.kylin.mdx.insight.common.async;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author wanghui
 * Created 2016-08-18 上午12:36
 */
public class AsyncServiceTest {

    @Test
    public void testAsync() throws InterruptedException {
        final Random random = new Random();
        final AsyncService asyncService = new AsyncService();
        // 任务监听
        asyncService.setListener(new TaskListener() {
            @Override
            public void taskStart(ServiceStatus status) {
            }

            @Override
            public void taskFinish(ServiceStatus status) {
            }

            @Override
            public void taskSucceed(ServiceStatus status) {
            }

            @Override
            public void taskFailed(ServiceStatus status, Throwable e) {
            }
        });
        // 测试 10 个并发
        final int limit = 5;
        for (int i = 0; i < 2 * limit; i++) {
            asyncService.awaitLimit(limit);
            asyncService.submit(new ParamsTask(i) {
                @Override
                public void run() {
                    try {
                        Thread.sleep(Math.abs(random.nextInt() % (2 * limit)));
                    } catch (InterruptedException ignored) {
                    }
                    if (get(FIRST, Integer.class) == limit) {
                        throw new RuntimeException();
                    }
                }
            });
        }
        asyncService.awaitLimit(0);
        asyncService.shutdownNow();
    }

    @Test
    public void testCallable() throws ExecutionException, InterruptedException {
        final AsyncService asyncService = new AsyncService();
        Future<Integer> future1 = asyncService.submit(() -> 1000);
        int result1 = future1.get();
        Assert.assertEquals(1000, result1);
        Future<Integer> future2 = asyncService.submit(() -> {
            throw new RuntimeException();
        });
        future2.get();
        asyncService.shutdownNow();
    }

    /**
     * 注意，这里单个线程休眠 20 ms，中间停顿 50 ms
     * 如果 cpu 计时误差超过 5ms，将导致该 UT 失败
     */
    @Test
    public void testStatus() throws InterruptedException {
        final AsyncService asyncService = new AsyncService();
        for (int i = 1; i <= 5; i++) {
            final int j = i;
            asyncService.submit(() -> {
                try {
                    Thread.sleep(20 * j);
                } catch (InterruptedException ignored) {
                }
            });
        }
        asyncService.awaitLimit(5);
        Thread.sleep(50);
        Assert.assertEquals(5, asyncService.getTotal());
        Assert.assertTrue(asyncService.getFinished() > 0);
        Assert.assertTrue(asyncService.getRunning() > 0);
        Assert.assertTrue(asyncService.getSucceed() > 0);
        Assert.assertEquals(0, asyncService.getFailed());
        asyncService.awaitLimit(2);
        Assert.assertTrue(asyncService.getRunning() <= 2);
        asyncService.awaitLimit(0);
        Assert.assertEquals(0, asyncService.getRunning());
        asyncService.shutdown();
    }

    /**
     * MDX-2904
     */
    @Test
    public void testReject() throws InterruptedException {
        final AsyncService asyncService = new AsyncService(5);
        long start = System.currentTimeMillis();
        long first = -1;
        for (int i = 0; i <= 5; i++) {
            asyncService.submit(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            });
            if (first < 0) {
                first = System.currentTimeMillis() - start;
            }
        }
        asyncService.awaitLimit(0);
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue(cost + "should >= 200", cost >= 200);
        Assert.assertTrue(cost + "should <= 300", cost <= 300 + first);
        asyncService.shutdown();
    }

}