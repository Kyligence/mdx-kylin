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


package io.kylin.mdx.insight.core.model.semantic;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kylin.mdx.insight.core.entity.CalculateMeasure;
import io.kylin.mdx.insight.core.entity.NamedMeasure;
import io.kylin.mdx.insight.core.entity.NamedProperty;
import io.kylin.mdx.insight.core.entity.NamedSet;
import io.kylin.mdx.insight.core.entity.CalculatedMemberNonEmptyBehaviorMeasure;
import io.kylin.mdx.insight.core.model.generic.RawJoinTable;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class SemanticDataset {

    private String project;

    @JsonProperty("dataset_name")
    private String datasetName;

    private String type;

    private Integer access;

    @JsonProperty("datasource_ip")
    private String datasourceIp;

    @JsonProperty("datasource_port")
    private Integer datasourcePort;

    @JsonProperty("created_time")
    private Long createTime;

    @JsonProperty("last_modified")
    private Long lastModified;

    @JsonProperty("model_relations")
    private List<ModelRelation> modelRelations;

    private List<AugmentedModel> models;

    @JsonProperty("calculate_measures")
    private List<CalculateMeasure0> calculateMeasures;

    @JsonProperty("named_sets")
    private List<NamedSet0> namedSets;

    @JsonProperty("dim_table_model_relations")
    private List<DimTableModelRelation> dimTableModelRelations;

    public SemanticDataset(String project, String datasetName, Integer access,
                           String datasourceIp, Integer datasourcePort, Long createTime, Long lastModified) {
        this.project = project;
        this.datasetName = datasetName;
        this.access = access;
        this.datasourceIp = datasourceIp;
        this.datasourcePort = datasourcePort;
        this.createTime = createTime;
        this.lastModified = lastModified;
    }

    public void addCalculateMeasure(CalculateMeasure0 calculateMeasure) {
        if (calculateMeasures == null) {
            calculateMeasures = new LinkedList<>();
        }
        calculateMeasures.add(calculateMeasure);
    }

    public void addNamedSet(NamedSet0 namedSet) {
        if (namedSets == null) {
            namedSets = new LinkedList<>();
        }
        namedSets.add(namedSet);
    }

    @Data
    @NoArgsConstructor
    public static class DimTableModelRelation {

        @JsonProperty("model_name")
        private String modelName;

        @JsonProperty("table_relations")
        private List<DimTableRelation> tableRelations;

        public DimTableModelRelation(String modelName) {
            this.modelName = modelName;
        }

        public void addTableRelation(DimTableRelation relation) {
            if (tableRelations == null) {
                tableRelations = new LinkedList<>();
            }

            tableRelations.add(relation);
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class DimTableRelation {

            @JsonProperty("table_name")
            private String tableName;

            /**
             * 0:joint | 1:not joint | 2: many to many"
             */
            @JsonProperty("relation_type")
            private Integer relationType;

            @JsonProperty("relation_fact_key")
            private String relationFactKey;

            @JsonProperty("relation_bridge_table_name")
            private String relationBridgeTableName;

        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculateMeasure0 {

        private String name;

        private String format;

        private String expression;

        private String folder;

        private String desc;

        private boolean invisible;

        private String subfolder;

        @JsonProperty("custom_translations")
        private List<AugmentCustomTranslation> customTranslations;

        @JsonProperty("non_empty_behavior")
        private List<CalculatedMemberNonEmptyBehaviorMeasure> nonEmptyBehaviorMeasures;

        public CalculateMeasure0(CalculateMeasure calculateMeasure) {
            this.name = calculateMeasure.getName();
            this.format = calculateMeasure.getFormat();
            this.expression = calculateMeasure.getExpression();
            this.folder = calculateMeasure.getMeasureFolder();
            this.desc = SemanticUtils.getDescFromExtend(calculateMeasure.buildExtend());
            this.subfolder = calculateMeasure.getSubfolder();
        }

        public void addCustomTranslation(AugmentCustomTranslation customTranslation) {
            if (customTranslations == null) {
                customTranslations = new LinkedList<>();
            }
            customTranslations.add(customTranslation);
        }
    }

    @Data
    @NoArgsConstructor
    public static class NamedSet0 {

        private String name;

        private String expression;

        private String folder;

        private String location;

        private boolean invisible;

        public NamedSet0(NamedSet namedSet) {
            this.name = namedSet.getName();
            this.expression = namedSet.getExpression();
            this.folder = namedSet.getFolder();
            this.location = namedSet.getLocation();
        }
    }

    @Data
    public static class AugmentedModel {

        @JsonProperty("model_name")
        private String modelName;

        @JsonProperty("model_alias")
        private String modelAlias;

        @JsonProperty("fact_table")
        private String factTable;

        private List<RawJoinTable> lookups;

        @JsonProperty("dimension_tables")
        private List<AugmentDimensionTable> dimensionTables = new LinkedList<>();

        private List<AugmentMeasure> measures = new LinkedList<>();

        public void addAugmentDimensionTable(AugmentDimensionTable augmentDimensionTable) {
            dimensionTables.add(augmentDimensionTable);
        }

        public void addAugmentMeasure(AugmentMeasure augmentMeasure) {
            measures.add(augmentMeasure);
        }

        @Data
        @NoArgsConstructor
        public static class AugmentMeasure {

            private String name;

            private String expression;

            @JsonProperty("data_type")
            private String dataType;

            /**
             * like: KYLIN_SALE.PRICE
             */
            @JsonProperty("dim_column")
            private String dimColumn;

            private String alias;

            private String formatString;

            private boolean invisible;

            private String desc;

            @JsonProperty("custom_translations")
            private List<AugmentCustomTranslation> customTranslations;

            private String subfolder;

            public AugmentMeasure(NamedMeasure namedMeasure) {
                this.name = namedMeasure.getName();
                this.expression = namedMeasure.getExpression();
                this.dataType = namedMeasure.getDataType();
                this.dimColumn = namedMeasure.getDimColumn();
                this.alias = namedMeasure.getAlias();
                this.formatString = namedMeasure.getFormat();
                this.desc = SemanticUtils.getDescFromExtend(namedMeasure.buildExtend());
                this.subfolder = namedMeasure.getSubfolder();
            }

            public void addCustomTranslation(AugmentCustomTranslation customTranslation) {
                if (customTranslations == null) {
                    customTranslations = new LinkedList<>();
                }
                customTranslations.add(customTranslation);
            }
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AugmentDimensionTable {

            private String name;

            @JsonProperty("actual_table")
            private String actualTable;

            /**
             * time | regular
             */
            private String type;

            private String alias;

            @JsonProperty("dim_columns")
            private List<AugmentDimensionCol> dimColumns = new LinkedList<>();

            @JsonProperty("hierarchies")
            private List<Hierarchy0> hierarchys;

            @JsonProperty("custom_translations")
            private List<AugmentCustomTranslation> customTranslations;

            public void addAugmentDimensionCol(AugmentDimensionCol col) {
                if (dimColumns == null) {
                    dimColumns = new LinkedList<>();
                }
                dimColumns.add(col);
            }

            public void addHierarchy(Hierarchy0 hierarchy) {
                if (hierarchys == null) {
                    hierarchys = new LinkedList<>();
                }
                hierarchys.add(hierarchy);
            }

            public void addCustomTranslation(AugmentCustomTranslation customTranslation) {
                if (customTranslations == null) {
                    customTranslations = new LinkedList<>();
                }
                customTranslations.add(customTranslation);
            }

            @Data
            @NoArgsConstructor
            public static class Hierarchy0 {

                private String name;

                @JsonProperty("dim_columns")
                private List<String> dimColumns;

                @JsonProperty("weight_columns")
                private List<String> weightColumns;

                @JsonProperty("custom_translations")
                private List<AugmentCustomTranslation> customTranslations;

                private String desc;

                public Hierarchy0(String name) {
                    this.name = name;
                }

                public void addCol(String col, String weightCol) {
                    if (dimColumns == null) {
                        dimColumns = new LinkedList<>();
                    }
                    dimColumns.add(col);

                    if (weightColumns == null) {
                        weightColumns = new LinkedList<>();
                    }
                    weightColumns.add(weightCol);
                }

                public void addCustomTranslation(AugmentCustomTranslation customTranslation) {
                    if (customTranslations == null) {
                        customTranslations = new LinkedList<>();
                    }
                    customTranslations.add(customTranslation);
                }
            }

            @Data
            @NoArgsConstructor
            public static class AugmentDimensionCol {

                private String name;

                /**
                 * 0:default, 1:levelYears, 2:levelQuarters, 3:levelMonths, 4:levelWeeks, 5:levelDays
                 */
                private Integer type;

                @JsonProperty("data_type")
                private String dataType;

                private String alias;

                private boolean invisible;

                private String desc;

                private String nameColumn;

                private String valueColumn;

                @JsonProperty("properties")
                private List<AugmentProperty> properties;

                @JsonProperty("custom_translations")
                private List<AugmentCustomTranslation> customTranslations;

                private String subfolder;

                private String defaultMember;

                public AugmentDimensionCol(String name, Integer type, String dataType, String alias, String desc, String nameColumn, String valueColumn, List<AugmentProperty> properties, String subfolder) {
                    this.name = name;
                    this.type = type;
                    this.dataType = dataType;
                    this.alias = alias;
                    this.desc = desc;
                    this.nameColumn = nameColumn;
                    this.valueColumn = valueColumn;
                    this.properties = properties;
                    this.subfolder = subfolder;
                }

                public void addCustomTranslation(AugmentCustomTranslation customTranslation) {
                    if (customTranslations == null) {
                        customTranslations = new LinkedList<>();
                    }
                    customTranslations.add(customTranslation);
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                public static class AugmentProperty {

                    private String name;

                    private String attribute;

                    public AugmentProperty(NamedProperty namedProperty) {
                        this.name = namedProperty.getName();
                        this.attribute = namedProperty.getAttribute();
                    }

                }
            }
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelRelation {

        @JsonProperty("model_left")
        private String modelLeft;

        @JsonProperty("model_right")
        private String modelRight;

        private List<Relation0> relations;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Relation0 {
            private String left;

            private String right;

        }

    }

    @Data
    @NoArgsConstructor
    public static class AugmentCustomTranslation {
        private String name;
        private String caption;
        private String description;

        public AugmentCustomTranslation(String name) {
            this.name = name;
        }

    }

}
