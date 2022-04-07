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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wanghui
 * Created 2016-12-04 上午6:05
 */
public class ServiceStatus {

    /**
     * 统计多少个任务已经提交
     */
    AtomicInteger total = new AtomicInteger(0);

    /**
     * 统计多少个任务已经完成
     */
    AtomicInteger finished = new AtomicInteger(0);

    /**
     * 统计多少个任务已经提交且未结束
     */
    AtomicInteger submitted = new AtomicInteger(0);

    /**
     * 统计正在执行的任务数
     */
    AtomicInteger running = new AtomicInteger(0);

    /**
     * 统计成功执行的任务数
     */
    AtomicInteger succeed = new AtomicInteger(0);

    /**
     * 统计失败的任务数
     */
    AtomicInteger failed = new AtomicInteger(0);

    // 统计计数,分别获取 total,succeed,failed,finished,running 计数

    public int getTotal() {
        return total.get();
    }

    public int getSucceed() {
        return succeed.get();
    }

    public int getFailed() {
        return failed.get();
    }

    public int getFinished() {
        return finished.get();
    }

    public int getRunning() {
        return running.get();
    }

    public int getSubmitted() {
        return submitted.get();
    }

}
