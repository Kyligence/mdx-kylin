package mondrian.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread pool with an "Execution cancelled" status, "invokeAll" method will interrupt
 * if the status becomes true.
 */
public interface ExecutorServiceWithStatus extends ExecutorService {
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, AtomicBoolean executionCancelled, long timeout, TimeUnit unit) throws InterruptedException;
}
