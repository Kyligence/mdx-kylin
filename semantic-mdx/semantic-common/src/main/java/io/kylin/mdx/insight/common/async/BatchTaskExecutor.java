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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author wanghui
 * Created 2016-04-06 下午9:01
 */
public class BatchTaskExecutor implements Executable {

    private final AsyncService asyncService;

    private final List<Runnable> taskList;

    private final long timeout;

    public BatchTaskExecutor(AsyncService asyncService, long timeout) {
        this.asyncService = asyncService;
        this.timeout = timeout;
        this.taskList = new ArrayList<>();
    }

    public BatchTaskExecutor(AsyncService asyncService) {
        this(asyncService, Long.MAX_VALUE);
    }

    public BatchTaskExecutor submit(Runnable runnable) {
        taskList.add(runnable);
        return this;
    }

    @Override
    public CountDownLatch executeAsync() {
        Objects.requireNonNull(asyncService);
        CountDownLatch latch = new CountDownLatch(taskList.size());
        for (Runnable runnable : taskList) {
            asyncService.submit(new LatchRunnable(runnable, latch));
        }
        return latch;
    }

    @Override
    public boolean execute() throws InterruptedException {
        CountDownLatch latch = executeAsync();
        return latch.await(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean executeWithThis() throws InterruptedException {
        Objects.requireNonNull(asyncService);
        int size = taskList.size();
        if (size <= 0) {
            return true;
        }
        if (size == 1) {
            // directly execute task 0
            taskList.get(0).run();
            return true;
        }
        CountDownLatch latch = new CountDownLatch(size - 1);
        // submit task 1 ... [n-1]
        for (int i = 1; i < size; i++) {
            asyncService.submit(new LatchRunnable(taskList.get(i), latch));
        }
        // execute task 0
        taskList.get(0).run();
        // await execute task 1 ... [n-1]
        return latch.await(timeout, TimeUnit.MILLISECONDS);
    }

}
