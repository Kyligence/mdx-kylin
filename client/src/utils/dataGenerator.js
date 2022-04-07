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
import { configs } from '../constants';
import { getDatasetKeyMap } from './datasetHelper';

export function generateDatasetJson(datasetData) {
  return {
    project: datasetData.project,
    dataset_name: datasetData.datasetName,
    type: datasetData.type,
    front_v: configs.getDatasetVersion(),
    access: datasetData.access ? 1 : 0,
    translation_types: datasetData.translationTypes,
    model_relations: datasetData.modelRelations.map(link => ({
      model_left: link.modelLeft,
      model_right: link.modelRight,
      relation: link.relation,
    })),
    models: datasetData.models.map(model => ({
      model_name: model.name,
      model_alias: model.alias,
      fact_table: model.factTableAlias,
      translation: model.translation,
      dimension_tables: model.dimensionTables.map(table => ({
        name: table.name,
        type: table.type,
        alias: table.alias,
        actual_table: table.actualTable,
        translation: table.translation,
        dim_cols: table.dimCols.map(column => ({
          name: column.name,
          desc: column.desc,
          type: column.type,
          data_type: column.dataType,
          alias: column.alias,
          is_visible: column.isVisible,
          invisible: column.invisible,
          visible: column.visible,
          name_column: column.nameColumn,
          value_column: column.valueColumn,
          translation: column.translation,
          subfolder: column.subfolder,
          default_member: column.defaultMember,
          properties: column.properties.map(property => ({
            name: property.name,
            col_name: property.columnName,
            col_alias: property.columnAlias,
          })),
        })),
        hierarchys: table.hierarchys.map(hierarchy => ({
          name: hierarchy.name,
          desc: hierarchy.desc,
          dim_cols: hierarchy.dimCols,
          weight_cols: hierarchy.weightCols,
          translation: hierarchy.translation,
        })),
      })),
      measures: model.measures.map(measure => ({
        name: measure.name,
        desc: measure.desc,
        subfolder: measure.subfolder,
        format: measure.format,
        format_type: measure.formatType,
        data_type: measure.dataType,
        expression: measure.expression,
        dim_column: measure.expressionParams,
        alias: measure.alias,
        is_visible: measure.isVisible,
        invisible: measure.invisible,
        visible: measure.visible,
        translation: measure.translation,
      })),
    })),
    calculate_measures: datasetData.calculateMeasures.map(cMeasure => ({
      name: cMeasure.name,
      format: cMeasure.format,
      format_type: cMeasure.formatType,
      desc: cMeasure.desc,
      folder: cMeasure.folder,
      subfolder: cMeasure.subfolder,
      expression: cMeasure.expression,
      is_visible: cMeasure.isVisible,
      invisible: cMeasure.invisible,
      visible: cMeasure.visible,
      translation: cMeasure.translation,
      non_empty_behavior: cMeasure.nonEmptyBehavior,
    })),
    named_sets: datasetData.namedSets.map(namedSet => ({
      name: namedSet.name,
      location: namedSet.location,
      expression: namedSet.expression,
      is_visible: namedSet.isVisible,
      invisible: namedSet.invisible,
      visible: namedSet.visible,
      translation: namedSet.translation,
    })),
    dim_table_model_relations: datasetData.dimTableModelRelations.map(link => ({
      model_name: link.modelName,
      table_relations: link.tableRelations.map(relation => ({
        table_name: relation.tableName,
        relation_type: relation.relationType,
        relation_fact_key: relation.relationFactKey,
        relation_bridge_table_name: relation.relationBridgeTableName,
      })),
    })),
    canvas: JSON.stringify(datasetData.canvas),
  };
}

export function generateExpressionSchema(datasetData) {
  const datasetKeyMap = getDatasetKeyMap({ dataset: datasetData });
  const allMeasureNames = [];
  const allTableNames = [];
  const allCMeasureNames = datasetData.calculateMeasures.map(cMeasure => cMeasure.name);
  const allNamedSets = datasetData.namedSets.map(namedSet => ({
    name: namedSet.name,
    location: namedSet.location,
    expression: namedSet.expression,
  }));

  for (const model of datasetData.models) {
    for (const table of model.dimensionTables) {
      allTableNames.push({
        alias: table.alias,
        type: table.type,
        model: model.name,
        dim_cols: table.dimCols.map(col => ({
          alias: col.alias,
          type: col.type,
        })),
        hierarchies: table.hierarchys.map(hierarchy => ({
          name: `${hierarchy.name}-Hierarchy`,
          dim_cols: hierarchy.dimCols.map(dimCol => {
            let dimColAlias = dimCol;
            // 将column name映射成alias发给后端做schema校验
            try { dimColAlias = datasetKeyMap[`${model.name}-${table.name}-c-${dimCol}`].alias; } catch {}
            return dimColAlias;
          }),
        })),
      });
    }
    for (const measure of model.measures) {
      allMeasureNames.push(measure.alias);
    }
  }

  return {
    dimension_tables: allTableNames,
    measures: allMeasureNames,
    calculate_measures: allCMeasureNames,
    named_sets: allNamedSets,
  };
}

export function generateAccessSchema(datasetData) {
  return {
    models: datasetData.models.map(model => ({
      model_name: model.name,
      model_alias: model.alias,
      fact_table: model.factTableAlias,
      dimension_tables: model.dimensionTables.map(table => ({
        name: table.name,
        alias: table.alias,
        actual_table: table.actualTable,
        dim_cols: table.dimCols.map(column => ({
          name: column.name,
          alias: column.alias,
          name_column: column.nameColumn,
          value_column: column.valueColumn,
          default_member: column.defaultMember,
          properties: column.properties.map(property => ({
            name: property.name,
            col_name: property.columnName,
            col_alias: property.columnAlias,
          })),
        })),
        hierarchys: table.hierarchys.map(hierarchy => ({
          name: hierarchy.name,
          dim_cols: hierarchy.dimCols,
          weight_cols: hierarchy.weightCols,
        })),
      })),
      measures: model.measures.map(measure => ({
        name: measure.name,
        alias: measure.alias,
        expression: measure.expression,
        dim_column: measure.expressionParams,
      })),
    })),
    calculate_measures: datasetData.calculateMeasures.map(cMeasure => ({
      name: cMeasure.name,
      expression: cMeasure.expression,
    })),
    named_sets: datasetData.namedSets.map(namedSet => ({
      name: namedSet.name,
      expression: namedSet.expression,
    })),
  };
}
