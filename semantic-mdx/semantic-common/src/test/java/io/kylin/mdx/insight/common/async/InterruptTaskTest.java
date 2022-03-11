package io.kylin.mdx.insight.common.async;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wanghui
 * Created 2016-09-02 上午7:04
 */
public class InterruptTaskTest {

    @Test
    public void test() throws InterruptedException {
        ExecutorService executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(), new NameThreadFactory("test-thread"));
        AsyncService asyncService = new AsyncService(executorService);
        Assert.assertEquals(executorService, asyncService.getExecutor());
        InterruptTaskExecutor taskList = new InterruptTaskExecutor(asyncService);
        for (int i = 0; i < 10; i++) {
            taskList.submit(new InterruptTask(i) {
                @Override
                public void run() {
                    int index = get(FIRST);
                    int count = 0;
                    while (isRunning()) {
                        try {
                            Thread.sleep(10);
                            count++;
                            if (count == 5 && index == 5) {
                                interruptAll();
                            }
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            });
        }
        taskList.execute();
        asyncService.shutdown();
    }

}