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


package io.kylin.mdx.web.rejecter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class RejectRuleManager {

    private static final RejectRuleManager INSTANCE = new RejectRuleManager();

    private final Map<String, RejectRule> rules = new LinkedHashMap<>();

    public static void newInstance() {
        // Just generate INSTANCE
    }

    public static RejectRuleManager getInstance() {
        return INSTANCE;
    }

    public RejectRuleManager() {
        // 加载外部规则
        ServiceLoader<ExtRuleManager> loader = ServiceLoader.load(ExtRuleManager.class);
        for (ExtRuleManager manager : loader) {
            manager.getAllRule().forEach(this::addNewRule);
        }
    }

    public Collection<RejectRule> getAllRule() {
        return rules.values();
    }

    public void addNewRule(RejectRule rule) {
        rules.put(rule.name(), rule);
    }

}
