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

import java.util.concurrent.CountDownLatch;

/**
 * @author wanghui
 * Created 2017-07-08 下午8:36
 */
public interface Executable {

    /**
     * 异步执行，返回一个表示当前表示还有多少个任务未完成的计数器
     *
     * @return 计数器
     */
    CountDownLatch executeAsync();

    /**
     * 同步执行
     *
     * @return 执行结果
     * @throws InterruptedException 执行时被中断
     */
    boolean execute() throws InterruptedException;

    /**
     * 同步执行, 并且会利用自身执行第一个任务
     *
     * @return 执行结果
     * @throws InterruptedException 执行时被中断
     */
    boolean executeWithThis() throws InterruptedException;

}
