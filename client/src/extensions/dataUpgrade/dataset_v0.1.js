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
export function upgradeDatasetV01(json, upgradeData) {
  return {
    ...json,
    // 升级model.fact_table
    models: json.models.map(model => {
      const upgradeModel = upgradeData.find(m => m.modelName === model.model_name);
      return {
        ...model,
        fact_table: `${upgradeModel.factTableSchema}.${upgradeModel.factTableAlias}`,
        // 升级table.actual_table
        dimension_tables: model.dimension_tables.map(table => {
          const upgradeTable = upgradeModel.dimensionTables.find(t => t.name === table.name);
          return {
            ...table,
            alias: table.name,
            actual_table: upgradeTable.actualTable,
            // 升级column.data_type
            dim_cols: table.dim_cols.map(column => {
              const upgradeColumn = upgradeTable.dimensionColumns
                .find(c => c.columnName === column.name);
              return {
                ...column,
                data_type: upgradeColumn.dataType,
              };
            }),
          };
        }),
        // 升级measure.expression和measure.dim_column
        measures: model.measures.map(measure => {
          const upgradeMeasure = upgradeModel.cubeMeasures
            .find(m => m.measureName === measure.name);

          let expressionParams = '';
          if (upgradeMeasure.colMeasured) {
            const { tableAlias, colName } = upgradeMeasure.colMeasured;
            expressionParams = tableAlias !== 'constant' ? `${tableAlias}.${colName}` : colName;
          }

          return {
            ...measure,
            data_type: upgradeMeasure.dataType,
            expression: upgradeMeasure.expression,
            dim_column: expressionParams,
          };
        }),
      };
    }),
  };
}
