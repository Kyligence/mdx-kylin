package io.kylin.mdx.insight.common.async;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RecallTaskTest {

    @Test
    public void test() {
        ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);
        final AtomicInteger count = new AtomicInteger(0);
        RecallTask recallTask = new RecallTask(service) {

            @Override
            public void call() {
                if (count.getAndIncrement() >= 10) {
                    stop();
                }
            }

            @Override
            public Pair<Long, TimeUnit> getPeriod() {
                return new ImmutablePair<>(10L - getLastRunTime(), TimeUnit.MILLISECONDS);
            }
        };
        recallTask.startNow();
        while (recallTask.isRunning()) {
            Thread.yield();
        }
        Assert.assertEquals(11, count.get());
        recallTask.startDelay(10, TimeUnit.MICROSECONDS);
        while (recallTask.isRunning()) {
            Thread.yield();
        }
        service.shutdown();
    }

}