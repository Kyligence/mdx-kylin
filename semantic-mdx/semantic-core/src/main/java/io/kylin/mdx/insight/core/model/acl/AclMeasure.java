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


package io.kylin.mdx.insight.core.model.acl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@NoArgsConstructor
public class AclMeasure extends AclDependColumn {

    private String alias;

    @JsonProperty("dim_column")
    private String dimColumn;

    public AclMeasure(SemanticDataset.AugmentedModel.AugmentMeasure measure) {
        this.setName(measure.getName());
        this.setExpression(measure.getExpression());
        this.alias = measure.getAlias();
        this.dimColumn = measure.getDimColumn();
    }

    public Pair<String, String> toDimAndCol() {
        String[] dimColumns = dimColumn.split("\\.");
        if (dimColumns.length == 1) {
            return new ImmutablePair<>(null, dimColumns[0]);
        } else if (dimColumns.length == 2) {
            return new ImmutablePair<>(dimColumns[0], dimColumns[1]);
        }
        return null;
    }

}
