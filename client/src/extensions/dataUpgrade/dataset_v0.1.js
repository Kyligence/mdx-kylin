/*
  Copyright (C) 2021 Kyligence Inc. All rights reserved.

  http://kyligence.io

  This software is the confidential and proprietary information of
  Kyligence Inc. ("Confidential Information"). You shall not disclose
  such Confidential Information and shall use it only in accordance
  with the terms of the license agreement you entered into with
  Kyligence Inc.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
