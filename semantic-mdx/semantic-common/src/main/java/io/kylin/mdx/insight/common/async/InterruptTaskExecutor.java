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
 * 使用流程:
 * 1. 创建 AsyncService :
 * #   AsyncService asyncService = new AsyncService();
 * <p>
 * 2. 创建一个 InterruptTaskExecutor :
 * #   InterruptTaskExecutor taskExecutor = new InterruptTaskExecutor();
 * <p>
 * 3. 添加任务 :
 * #   taskExecutor.submit(new InterruptTask() {...});
 * <p>
 * 4. 启动执行 :
 * #   taskExecutor.executeAsync();
 * <p>
 * 5. 清理现场 :
 * # 有两种清理现场的方式,第一种:
 * #    使线程执行完毕:    asyncService.awaitLimit(0);
 * #    立即关闭线程池:    asyncService.shutdownNow()
 * # 第二种:
 * #    关闭线程池:       asyncService.shutdown();
 * <p>
 * 中断方式:
 * #    任意一个任务中均可通过调用下列方法将全部的任务中断。
 * #         interruptAll();
 * #    约定: 所有的任务都需要时刻通过调用 isRunning 检测是否有任务启动了中断。
 *
 * @author wanghui
 * Created 2016-06-01 下午1:18
 */
public class InterruptTaskExecutor implements Executable {

    private final AsyncService asyncService;

    private final List<InterruptTask> taskList;

    private final long timeout;

    private CountDownLatch latch;

    public InterruptTaskExecutor(AsyncService asyncService, long timeout) {
        this.asyncService = asyncService;
        this.timeout = timeout;
        this.taskList = new ArrayList<>();
    }

    public InterruptTaskExecutor(AsyncService asyncService) {
        this(asyncService, Long.MAX_VALUE);
    }

    public InterruptTaskExecutor submit(InterruptTask task) {
        task.setTaskList(this);
        taskList.add(task);
        return this;
    }

    public boolean interruptAll() {
        assert latch != null;
        for (InterruptTask task : taskList) {
            task.exit();
        }
        while (latch.getCount() > 0) {
            latch.countDown();
        }
        return true;
    }

    @Override
    public CountDownLatch executeAsync() {
        Objects.requireNonNull(asyncService);
        latch = new CountDownLatch(taskList.size());
        for (InterruptTask task : taskList) {
            asyncService.submit(new LatchRunnable(task, latch));
        }
        return latch;
    }

    @Override
    public boolean execute() throws InterruptedException {
        CountDownLatch latch = executeAsync();
        return latch.await(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean executeWithThis() {
        throw new UnsupportedOperationException();
    }

}
