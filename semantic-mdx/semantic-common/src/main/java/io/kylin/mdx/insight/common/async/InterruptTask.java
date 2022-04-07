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

/**
 * 可全局中断的任务
 *
 * @author wanghui
 * Created 2016-06-01 上午11:39
 */
public abstract class InterruptTask extends ParamsTask {

    private InterruptTaskExecutor taskList;

    private volatile boolean running = true;

    public InterruptTask(Object... params) {
        super(params);
    }

    public void interruptAll() {
        if (taskList != null) {
            taskList.interruptAll();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void exit() {
        this.running = false;
    }

    public void setTaskList(InterruptTaskExecutor taskList) {
        this.taskList = taskList;
    }

}
