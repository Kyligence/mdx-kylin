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


package io.kylin.mdx.insight.engine.service.parser;

import io.kylin.mdx.insight.engine.bean.SimpleSchema;
import io.kylin.mdx.insight.engine.support.ExprParseException;
import mondrian.resource.MondrianResource;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;


@Service
public class DefaultMemberValidatorImpl {

    private static final Pattern memberValPattern = Pattern.compile("[&]?\\[[^\\[]+\\]");

    private final DefaultMemberParserImpl defaultMemberParserImpl = new DefaultMemberParserImpl();

    public void validateDefaultMember(String defaultMember, String dimensionPath, SimpleSchema simpleSchema) throws ExprParseException {
        String[] defaultMemberParts = splitMemberExpressionAndRemoveBrackets(defaultMember);
        if (defaultMemberParts != null) {
            if (!validateDefaultMemberExpression(defaultMemberParts, dimensionPath)) {
                throw MondrianResource.instance().MdxDefaultMemberExpInvalid.ex(defaultMember);
            }
            return;
        }

        defaultMemberParserImpl.parse(defaultMember, simpleSchema);
    }

    private String[] splitMemberExpressionAndRemoveBrackets(String defaultMember) {
        if (defaultMember == null) {
            return null;
        }

        String[] defaultMemberParts = defaultMember.split("\\.");
        if (defaultMemberParts.length < 3 || defaultMemberParts.length > 4) {
            return null;
        }

        for (int i = 0; i < defaultMemberParts.length - 1; i++) {
            String partName = removeBrackets(defaultMemberParts[i]);
            if (partName == null) {
                return null;
            }
            defaultMemberParts[i] = partName;
        }

        if (!memberValPattern.matcher(defaultMemberParts[defaultMemberParts.length - 1]).find()) {
            return null;
        }

        return defaultMemberParts;
    }

    private boolean validateDefaultMemberExpression(String[] defaultMemberParts, String dimensionPath) {
        if (dimensionPath == null) {
            return false;
        }

        // dimensionPath pattern is like model.dimension.attribute
        String[] dimPathParts = dimensionPath.split("[.]");
        if (dimPathParts.length != 3) {
            return false;
        }
        String dimensionName = dimPathParts[1];
        String attributeName = dimPathParts[2];

        // [Time].[Year].&[2019] or [Time].[Year].[2019]
        if (defaultMemberParts.length == 3) {
            // check dimension name
            return Objects.equals(dimensionName, defaultMemberParts[0])
                    && Objects.equals(attributeName, defaultMemberParts[1]);
        }

        // [Time].[Year].[Year].&[2019] or [Time].[Year].[Year].[2019]
        if (defaultMemberParts.length == 4) {
            return Objects.equals(dimensionName, defaultMemberParts[0])
                    && Objects.equals(attributeName, defaultMemberParts[1])
                    && Objects.equals(defaultMemberParts[1], defaultMemberParts[2]);
        }
        return false;
    }

    private String removeBrackets(String expression) {
        if (!expression.startsWith("[") || !expression.endsWith("]")) {
            return null;
        }
        return expression.substring(1, expression.length() - 1);
    }
}
