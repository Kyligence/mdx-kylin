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


package io.kylin.mdx.insight.core.meta;

import io.kylin.mdx.insight.common.DatasourceTypeEnum;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.core.meta.mock.KylinConvertorMock;

public class ConvertorFactory {

    public static IConvertor createModelConvertor() {
        DatasourceTypeEnum datasourceType = SemanticConfig.getInstance().getDatasourceType();
        boolean convertorMock = SemanticConfig.getInstance().isConvertorMock();
        String convertType = convertorMock ? datasourceType.toString() + "_MOCK" : datasourceType.toString();
        return ConvertorType.valueOf(convertType).getConvertor();
    }

    public enum ConvertorType {
        KYLIN(new KylinConvertor()), KYLIN_MOCK(new KylinConvertorMock());

        private final IConvertor convertor;

        ConvertorType(IConvertor convertor) {
            this.convertor = convertor;
        }

        public IConvertor getConvertor() {
            return this.convertor;
        }
    }

}
