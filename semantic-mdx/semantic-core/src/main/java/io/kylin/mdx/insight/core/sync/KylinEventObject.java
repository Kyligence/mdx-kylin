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

import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
public class KylinEventObject implements EventObject {

    private Object source;

    private KylinEventType eventType;

    @Override
    public EventType getEventType() {
        return eventType;
    }

    public KylinEventType getKeEventType() {
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
        this.eventType = (KylinEventType) eventType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KylinCubeChanged implements EventObject.Source {
        private String projectName;

        private Set<String> cubeNames;

        private List<KylinGenericModel> models;

    }


    public enum KylinEventType implements EventObject.EventType {

        /**
         *
         */
        PROJECT_NEWED,


        PROJECT_DELETEED,


        /**
         * cube newed in project
         */
        CUBE_NEWED,

        /**
         * cube deleted in project
         */
        CUBE_DELETED,
        /**
         * cube changed in project
         */
        CUBE_CHANGED,

        /**
         * make cubes latest in project
         */
        CUBE_LATEST;


        @Override
        public String getEventName() {
            return name();
        }
    }
}
