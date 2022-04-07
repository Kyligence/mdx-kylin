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


package io.kylin.mdx.insight.core.support;

import io.kylin.mdx.insight.common.util.Utils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Execution {

    private long startTime;

    private String point;

    public Execution() {
    }

    public Execution(String point) {
        this.point = point;
        this.startTime = System.currentTimeMillis();
    }

    public Execution(String template, Object... args) {
        this.point = Utils.formatStr(template, args);
        this.startTime = System.currentTimeMillis();
    }

    public void logTimeConsumed() {
        log.debug(Utils.formatStr("Execution point: [%s], time:[%d]", point, (System.currentTimeMillis() - startTime)));
    }

    public void logTimeConsumed(String msg) {
        log.debug(Utils.formatStr("Execution point: [%s], time:[%d]", msg, (System.currentTimeMillis() - startTime)));
    }

    public void logTimeConsumed(long infoThreshold, long warnThreshold) {
        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed > warnThreshold) {
            log.warn(Utils.formatStr("Execution point: [%s], time:[%d]", point, elapsed));
        } else if (elapsed > infoThreshold) {
            log.debug(Utils.formatStr("Execution point: [%s], time:[%d]", point, elapsed));
        } else {
            log.info(Utils.formatStr("Execution point: [%s], time:[%d]", point, elapsed));
        }
    }
}
