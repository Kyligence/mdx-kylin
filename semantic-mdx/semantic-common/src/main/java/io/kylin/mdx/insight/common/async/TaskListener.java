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
 * @author wanghui
 * Created 2016-12-04 上午6:01
 */
public interface TaskListener {

    /**
     * 任务开始
     *
     * @param status 状态
     */
    void taskStart(ServiceStatus status);

    /**
     * 任务结束
     *
     * @param status 状态
     */
    void taskFinish(ServiceStatus status);

    /**
     * 任务成功完成
     *
     * @param status 状态
     */
    void taskSucceed(ServiceStatus status);

    /**
     * 任务执行抛出异常
     *
     * @param status 状态
     * @param e      异常
     */
    void taskFailed(ServiceStatus status, Throwable e);

}

