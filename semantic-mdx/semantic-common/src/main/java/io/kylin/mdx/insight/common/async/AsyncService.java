/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.insight.common.async;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统计计数: total,succeed,failed,finished,running,submitted
 * 保证:
 * #   total = running + finished
 * #   finished = succeed + failed
 *
 * @author wanghui
 * Created 2016-03-22 下午4:24
 */
public class AsyncService {

    private static final String THREAD_NAME = "Async";

    private static final AtomicInteger THREAD_INDEX = new AtomicInteger(0);

    private static final long WAIT_TIME = 10;

    /**
     * AsyncService 实时状态
     */
    private final ServiceStatus status = new ServiceStatus();

    /**
     * 默认的线程池,用于执行并发任务
     */
    private final ExecutorService executor;

    /**
     * 监听器, 用于获知任务执行情况
     */
    private TaskListener listener;

    /**
     * 创建一个缓存线程池
     */
    public AsyncService() {
        this.executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new NameThreadFactory(THREAD_NAME, String.valueOf(THREAD_INDEX.getAndIncrement())));
    }

    /**
     * 创建一个最多 maxThread 线程的缓存线程池
     *
     * @param nThreads 最大线程数
     */
    public AsyncService(int nThreads) {
        this.executor = new ThreadPoolExecutor(nThreads, nThreads, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                new NameThreadFactory(THREAD_NAME, String.valueOf(THREAD_INDEX.getAndIncrement())));
    }

    /**
     * 使用传入的线程池执行任务
     *
     * @param executor 线程池
     */
    public AsyncService(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * 获取线程池
     *
     * @return 线程池
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * 设置监听器
     *
     * @param listener 监听器
     */
    public void setListener(TaskListener listener) {
        this.listener = listener;
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * 立即关闭线程池
     */
    public void shutdownNow() {
        executor.shutdownNow();
    }

    /**
     * 通知启动了新任务,不允许外部访问
     */
    private void submitOne() {
        status.total.incrementAndGet();
        status.submitted.incrementAndGet();
    }

    void startOne() {
        status.running.incrementAndGet();
        if (listener != null) {
            listener.taskStart(status);
        }
    }

    private void finishOne() {
        // 当前任务结束后，正在运行的线程数目
        status.running.decrementAndGet();
        status.submitted.decrementAndGet();
        status.finished.incrementAndGet();
        if (listener != null) {
            listener.taskFinish(status);
        }
    }

    /**
     * 通知任务成功,不允许外部访问
     */
    void successOne() {
        finishOne();
        status.succeed.incrementAndGet();
        if (listener != null) {
            listener.taskSucceed(status);
        }
    }

    /**
     * 通知任务失败,不允许外部访问
     */
    void failOne(Throwable e) {
        finishOne();
        status.failed.incrementAndGet();
        if (listener != null) {
            listener.taskFailed(status, e);
        }
    }

    /**
     * 提交一个任务, 任务执行抛出异常会记录
     *
     * @param runnable 任务
     * @return Future
     */
    public Future<?> submit(Runnable runnable) {
        Objects.requireNonNull(runnable);
        this.submitOne();
        return executor.submit(new AsyncRunnable(this, runnable));
    }

    /**
     * 提交一个任务, 任务执行抛出异常会记录
     *
     * @param callable 任务
     * @return Future
     */
    public <T> Future<T> submit(Callable<T> callable) {
        Objects.requireNonNull(callable);
        this.submitOne();
        return executor.submit(new AsyncCallable<>(this, callable));
    }

    // 统计计数,分别获取 total,succeed,failed,finished,running 计数

    public ServiceStatus getStatus() {
        return status;
    }

    public int getTotal() {
        return getStatus().getTotal();
    }

    public int getSucceed() {
        return getStatus().getSucceed();
    }

    public int getFailed() {
        return getStatus().getFailed();
    }

    public int getFinished() {
        return getStatus().getFinished();
    }

    public int getRunning() {
        return getStatus().getRunning();
    }

    public int getSubmitted() {
        return getStatus().getSubmitted();
    }

    /**
     * 等待, 直到运行的任务小于等于 limit 个时结束等待
     * <p>
     * 例如: awaitLimit(0), 执行后将阻塞直到全部提交的任务执行完毕
     *
     * @param limit 限制最多运行的任务数
     * @throws InterruptedException 当被中断时抛出该异常
     */
    public void awaitLimit(int limit) throws InterruptedException {
        limit = Math.max(0, limit);
        while (getSubmitted() > limit) {
            Thread.sleep(WAIT_TIME);
        }
    }

    /**
     * @author wanghui
     * Created 2016-04-01 下午5:53
     */
    static class AsyncCallable<T> implements Callable<T> {

        private final AsyncService asyncService;

        private final Callable<T> callable;

        AsyncCallable(AsyncService asyncService, Callable<T> callable) {
            this.asyncService = asyncService;
            this.callable = callable;
        }

        @Override
        public T call() {
            try {
                asyncService.startOne();
                T res = callable.call();
                asyncService.successOne();
                return res;
            } catch (Throwable e) {
                asyncService.failOne(e);
                return null;
            }
        }

    }

    /**
     * @author wanghui
     * Created 2016-04-01 下午5:50
     */
    static class AsyncRunnable implements Runnable {

        private final AsyncService asyncService;

        private final Runnable runnable;

        AsyncRunnable(AsyncService asyncService, Runnable runnable) {
            this.asyncService = asyncService;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                asyncService.startOne();
                runnable.run();
                asyncService.successOne();
            } catch (Throwable e) {
                asyncService.failOne(e);
            }
        }

    }

}
