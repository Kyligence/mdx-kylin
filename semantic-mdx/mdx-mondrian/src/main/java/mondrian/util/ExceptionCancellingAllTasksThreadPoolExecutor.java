package mondrian.util;

import com.alibaba.ttl.threadpool.TtlExecutors;
import lombok.extern.slf4j.Slf4j;
import mondrian.rolap.RolapUtil;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ExceptionCancellingAllTasksThreadPoolExecutor extends ThreadPoolExecutor implements ExecutorServiceWithStatus, ExecutorServiceWithTimeout {
    /** The thread pool to attach the monitor task for cancellation status and memory usage status. */
    private final ScheduledExecutorService tasksMonitor;

    public ExceptionCancellingAllTasksThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.tasksMonitor = TtlExecutors.getTtlScheduledExecutorService(new ScheduledThreadPoolExecutor(
                1,
                new BasicThreadFactory.Builder()
                        .namingPattern("ExceptionCancellingAllTasksThreadPoolExecutor_TasksMonitor_%d")
                        .daemon(true)
                        .build()));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         AtomicBoolean executionCancelled,
                                         long timeout,
                                         TimeUnit unit) throws InterruptedException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        long nanos = unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());

        // Start the periodic monitor task
        ScheduledFuture<?> monitorFuture = tasksMonitor.scheduleWithFixedDelay(
                new TaskCancellerWithStatus<>(futures, executionCancelled),
                0,
                1,
                TimeUnit.SECONDS);

        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                futures.add(newTaskFor(t));
            }

            final long deadline = System.nanoTime() + nanos;
            final int size = futures.size();

            // Interleave time checks and calls to execute in case
            // executor doesn't have any/much parallelism.
            for (int i = 0; i < size; i++) {
                execute((Runnable) futures.get(i));
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    return futures;
                }
            }

            for (int i = 0; i < size; i++) {
                Future<T> f = futures.get(i);
                if (!f.isDone()) {
                    if (nanos <= 0L) {
                        return futures;
                    }
                    try {
                        f.get(nanos, TimeUnit.NANOSECONDS);
                    } catch (CancellationException | ExecutionException ignore) {
                    } catch (TimeoutException toe) {
                        log.error("Query cancelled due to time out exception.");
                        return futures;
                    }
                    nanos = deadline - System.nanoTime();
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                for (Future<T> future : futures) {
                    future.cancel(true);
                }
            }
            monitorFuture.cancel(false);
        }
    }

    @Override
    public void submitAll(List<Runnable> commands, long timeout, TimeUnit unit) {
        List<Future<?>> futures = new ArrayList<>(commands.size());
        tasksMonitor.schedule(new TaskCanceller<>(futures), timeout, unit);
        for (Runnable command : commands) {
            futures.add(submit(command));
        }
    }

    @Override
    public void shutdown() {
        tasksMonitor.shutdown();
        super.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        tasksMonitor.shutdownNow();
        return super.shutdownNow();
    }

    private static class TaskCanceller<T> implements Runnable {
        private final List<? extends Future<? extends T>> storedFutures;

        private TaskCanceller(List<? extends Future<? extends T>> futures) {
            this.storedFutures = futures == null ? null : Collections.unmodifiableList(futures);
        }

        @Override
        public void run() {
            cancelAll();
        }

        private void cancelAll() {
            if (storedFutures == null) {
                return;
            }
            for (Future<? extends T> future : storedFutures) {
                if (future.isDone()) {
                    continue;
                }
                future.cancel(true);
            }
        }
    }

    private static class TaskCancellerWithStatus<T> extends TaskCanceller<T> {
        private final AtomicBoolean executionCancelled;

        private TaskCancellerWithStatus(List<? extends Future<? extends T>> futures, AtomicBoolean executionCancelled) {
            super(futures);
            this.executionCancelled = executionCancelled;
        }

        @Override
        public void run() {
            if (memoryLimitExceeded()) {
                executionCancelled.set(true);
            }
            if (executionCancelled.get()) {
                super.run();
            }
        }

        private static boolean memoryLimitExceeded() {
            NotificationMemoryMonitor memoryMonitor = new NotificationMemoryMonitor();
            double usedMemoryRatio = memoryMonitor.getUsedMemory() * 100D / memoryMonitor.getMaxMemory();
            if (usedMemoryRatio > memoryMonitor.getDefaultThresholdPercentage()) {
                String qid = "[Query " + XmlaRequestContext.getContext().runningStatistics.queryID + "] ";
                if (qid != null) {
                    MDC.put(RolapUtil.MDC_KEY, qid);
                }
                log.error("Memory limit exceeded : used {}, max {}", memoryMonitor.getUsedMemory(), memoryMonitor.getMaxMemory());
                System.gc();
                return true;
            }

            return false;
        }
    }
}
