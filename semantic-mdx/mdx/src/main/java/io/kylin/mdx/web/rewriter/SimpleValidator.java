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



package io.kylin.mdx.web.rewriter;

import lombok.Getter;
import mondrian.olap.Exp;
import mondrian.olap.QueryPart;
import mondrian.parser.JavaccParserValidatorImpl;
import mondrian.parser.MdxParserValidator;
import mondrian.parser.QueryPartFactoryImpl;
import mondrian.rolap.RolapConnection;
import mondrian.rolap.RolapSchema;
import mondrian.server.Statement;
import mondrian.server.StatementImpl;

import java.util.Collections;

/**
 * @author hui.wang
 */
public class SimpleValidator {

    private final RolapSchema.RolapSchemaFunctionTable funTable = new RolapSchema.RolapSchemaFunctionTable(Collections.emptyList());

    @Getter
    private final Statement mockStatement = new MockInternalStatement();

    private final MdxParserValidator javaccParser = new JavaccParserValidatorImpl(new QueryPartFactoryImpl(true));

    public SimpleValidator() {
        funTable.init();
    }

    public QueryPart parseInternal(String queryString) {
        return javaccParser.parseInternal(mockStatement, queryString, false, funTable, true);
    }

    public Exp parseExpression(String queryString) {
        return javaccParser.parseExpression(mockStatement, queryString, false, funTable);
    }

    private static class MockInternalStatement extends StatementImpl {

        @Override
        public void close() {
        }

        @Override
        public RolapConnection getMondrianConnection() {
            throw new UnsupportedOperationException();
        }

    }

}
