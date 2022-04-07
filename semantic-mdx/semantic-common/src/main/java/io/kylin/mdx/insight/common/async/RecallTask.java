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

import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wanghui
 * Created 2017-09-01 上午11:18
 */
public abstract class RecallTask extends ParamsTask {

    private final ScheduledExecutorService service;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private long lastRunTime;

    public RecallTask(ScheduledExecutorService service) {
        this.service = service;
    }

    @Override
    public void run() {
        if (running.get()) {
            long startTime = System.currentTimeMillis();
            call();
            lastRunTime = System.currentTimeMillis() - startTime;
            runNext();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void startDelay(long delay, TimeUnit unit) {
        running.set(true);
        if (delay <= 0) {
            service.submit(this);
        } else {
            service.schedule(this, delay, unit);
        }
    }

    public void startNow() {
        startDelay(0, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running.set(false);
    }

    private void runNext() {
        Pair<Long, TimeUnit> entry = getPeriod();
        service.schedule(this, entry.getKey(), entry.getValue());
    }

    protected long getLastRunTime() {
        return lastRunTime;
    }

    /**
     * 执行任务
     */
    public abstract void call();

    /**
     * 时间间隔
     *
     * @return 下一次执行的时间间隔
     */
    public abstract Pair<Long, TimeUnit> getPeriod();

}
