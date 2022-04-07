package io.kylin.mdx.insight.core.support;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefaultThreadFactoryTest {

    @Test
    public void test() {
        DefaultThreadFactory threadFactory = new DefaultThreadFactory("test");
        ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1, threadFactory);
        service.schedule(() -> Assert.assertEquals("test-thread-1", Thread.currentThread().getName()), 0, TimeUnit.MILLISECONDS);
        service.shutdown();
    }

}