/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
export function upgradeDatasetV03(json) {
  return {
    ...json,
    models: json.models.map(model => ({
      ...model,
      dimension_tables: model.dimension_tables.map(table => ({
        ...table,
        dim_cols: table.dim_cols.map(column => ({
          ...column,
          visible: column.visible.map(visible => ({
            ...visible,
            name: visible.type === 'user' ? visible.name.toUpperCase() : visible.name,
          })),
        })),
      })),
      measures: model.measures.map(measure => ({
        ...measure,
        visible: measure.visible.map(visible => ({
          ...visible,
          name: visible.type === 'user' ? visible.name.toUpperCase() : visible.name,
        })),
      })),
    })),
    named_sets: json.named_sets.map(namedSet => ({
      ...namedSet,
      visible: namedSet.visible.map(visible => ({
        ...visible,
        name: visible.type === 'user' ? visible.name.toUpperCase() : visible.name,
      })),
    })),
    calculate_measures: json.calculate_measures.map(calculateMeasure => ({
      ...calculateMeasure,
      visible: calculateMeasure.visible.map(visible => ({
        ...visible,
        name: visible.type === 'user' ? visible.name.toUpperCase() : visible.name,
      })),
    })),
  };
}
