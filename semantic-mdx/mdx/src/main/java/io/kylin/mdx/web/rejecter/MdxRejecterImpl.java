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

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.ErrorCode;
import mondrian.xmla.MdxRejecter;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.olap4j.PreparedOlapStatement;

public class MdxRejecterImpl implements MdxRejecter {

    private final SemanticConfig semanticConfig = SemanticConfig.getInstance();

    private final RejectRuleManager manager = RejectRuleManager.getInstance();

    public MdxRejecterImpl() {
    }

    @Override
    public String redirect() {
        String redirectAddress = semanticConfig.getRejectRedirectAddress();
        if (StringUtils.isBlank(redirectAddress)) {
            throw new SemanticException(ErrorCode.REDIRECT_ADDRESS_NO_CONFIG);
        }
        String[] addressArray = redirectAddress.split(",");
        int index = RandomUtils.nextInt() % addressArray.length;
        return addressArray[index];
    }

    @Override
    public boolean isReject(PreparedOlapStatement statement) {
        for (RejectRule rule : manager.getAllRule()) {
            rule = rule.getOrNew();
            if (rule.reject(statement)) {
                return true;
            }
        }
        return false;
    }

}
