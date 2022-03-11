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


package io.kylin.mdx.insight.core.sync;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.DatasetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class DatasetEventObject implements EventObject {

    private Object source;

    private DatasetEventType eventType;

    @Override
    public EventType getEventType() {
        return eventType;
    }

    public DatasetEventType getDatasetEventType() {
        return eventType;
    }

    @Override
    public Object getChanged() {
        return source;
    }

    @Override
    public void setChanged(Object source) {
        this.source = source;
    }

    @Override
    public void setEventType(EventType eventType) {
        this.eventType = (DatasetEventType) eventType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetChangedSource implements EventObject.Source {
        private String projectName;

        private String datasetName;

        public DatasetChangedSource(DatasetEntity selfFixDataset) throws SemanticException {
            this.projectName = selfFixDataset.getProject();
            this.datasetName = selfFixDataset.getDataset();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetInvalidateSource implements EventObject.Source {
        private List<BrokenDataset> brokenDatasets;

        private List<DatasetEntity> selfFixedDatasets;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BrokenDataset {

        private DatasetEntity brokenDataset;

        private DatasetBrokenInfo brokenInfo;

    }

    public enum DatasetEventType implements EventObject.EventType {

        /**
         * dataset新增
         */
        DATASET_NEWED,

        /**
         * dataset删除
         */
        DATASET_DELETED,

        /**
         * dataset变更
         */
        DATASET_CHANGED,

        /**
         * dataset恢复正常
         */
        DATASET_RETURN_NORMAL,

        /**
         * dataset invalidated
         */
        DATASET_INVALIDATED;


        @Override
        public String getEventName() {
            return name();
        }
    }
}
