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


package io.kylin.mdx.insight.core.model.kylin;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class KylinCubeDesc {

    private String uuid;
    private String version;
    private String name;
    private String model_name;
    private Long last_modified;
    private String description;
    private String signature;
    private List<Dimension> dimensions;
    private List<Measure> measures;
    private List<AggregationGroup> aggregation_groups;

    @Data
    public static class Dimension {

        private String name;
        private String table;
        private String column;
        private List<String> derived;

    }

    @Data
    public static class Measure {

        private String name;
        private String description;
        private Function function;

        @Data
        public static class Function {

            private String expression;
            private Parameter parameter;
            private String returntype;

            @Data
            public static class Parameter {

                private String type;
                private String value;

            }
        }
    }

    @Data
    public static class AggregationGroup {

        private SelectRule select_rule;
        private List<String> includes;

        @Data
        public static class SelectRule {
            private List<List<String>> hierarchy_dims;
            private List<String> mandatory_dims;
            private List<List<String>> joint_dims;
        }
    }


}
