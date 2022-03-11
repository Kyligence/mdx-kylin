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
import React from 'react';
import { Form, Input } from 'kyligence-ui-react';
import { createSelector } from 'reselect';

import { strings, configs, storagePath, business } from '../../constants';
import { browserHelper, domHelper, antd } from '../../utils';

const { Select, Switch } = antd;
const { nodeTypes } = configs;
const { getStorage } = browserHelper;
const { DIFFER_DATASETS_MODAL_IS_SHOW_RULES } = storagePath;

export const ACTION_TYPE = {
  ADD_NEW: 'ADD_NEW',
  OVERRIDE: 'OVERRIDE',
  NOT_IMPORT: 'NOT_IMPORT',
};

export const DIFFER_TYPE = {
  NEW: 'NEW',
  DELETE: 'DELETE',
};

export function getDefaultState() {
  return {
    isShow: false,
    isShowRules: getStorage(DIFFER_DATASETS_MODAL_IS_SHOW_RULES) ?? true,
    isShowDiffer: true,
    callback: () => {},
    differences: [],
    token: '',
    host: '',
    port: '',
    form: {
      selectedId: '',
      datasets: [],
    },
  };
}

export const getSelectedDifference = createSelector(
  state => state.selectedId,
  state => state.differences,
  (selectedId, differences) => differences.find(record => record.id === selectedId) ?? {
    id: '',
    existed: false,
    dataset: '',
    new_items: [],
    reduce_items: [],
    brife_items: [],
    update_items: [],
  },
);

export const getSelectedType = createSelector(
  state => state.selectedId,
  state => state.datasets,
  (selectedId, datasets) => datasets.find(dataset => dataset.id === selectedId)?.type,
);

export const getSelectedBrife = createSelector(
  getSelectedDifference,
  difference => {
    const brifeTree = [];

    try {
      const relationships = {
        label: nodeTypes.RELATIONSHIP,
        nodeType: nodeTypes.RELATIONSHIP,
        children: [],
      };

      for (const item of difference.brife_items) {
        if (item.type === nodeTypes.MODEL) {
          brifeTree.push({ label: item.detail, nodeType: item.type, children: [] });
        }
        if (item.type === nodeTypes.RELATIONSHIP) {
          const [relationLeft, relationRight] = item.detail.split('-');
          const [modelLeft, tableLeft] = relationLeft.split('.');
          const [modelRight, tableRight] = relationRight.split('.');

          if (!relationships.children.length) {
            brifeTree.push(relationships);
          }
          const modelRelation = relationships.children
            .find(mr => mr.label === `${modelLeft}-${modelRight}`) ||
            { label: `${modelLeft}-${modelRight}`, nodeType: nodeTypes.MODEL_RELATION, children: [] };

          if (!modelRelation.children.length) {
            relationships.children.push(modelRelation);
          }

          modelRelation.children.push({ label: `${tableLeft}-${tableRight}`, nodeType: nodeTypes.COMMON_TABLE });
        }
      }
    } catch (e) {
      domHelper.error('getSelectedBrife', e);
    }

    return brifeTree;
  },
);

function getNewOrDeleteDiffer(difference, differType) {
  const diffTree = [];
  let diffCount = 0;

  try {
    const relationships = {
      label: nodeTypes.RELATIONSHIP,
      nodeType: nodeTypes.RELATIONSHIP,
      children: [],
    };

    const newModelItems = [];
    const newTableItems = [];
    const newOtherItems = [];

    // 遍历缓存新增的模型和表，以便去除这些模型下的实体
    for (const item of difference[differType]) {
      if (item.type === nodeTypes.MODEL) {
        newModelItems.push(item);
      } else if (item.type === nodeTypes.TABLE) {
        newTableItems.push(item);
      } else {
        newOtherItems.push(item);
      }
    }

    for (const item of [...newModelItems, ...newTableItems, ...newOtherItems]) {
      // 模型
      if (item.type === nodeTypes.MODEL) {
        diffTree.push({ label: item.detail, nodeType: item.type, children: [] });
        diffCount += 1;
      }
      // 模型关系
      if (item.type === nodeTypes.RELATIONSHIP) {
        const [relationLeft, relationRight] = item.detail.split('-');
        const [modelLeft, tableLeft] = relationLeft.split('.');
        const [modelRight, tableRight] = relationRight.split('.');

        if (!relationships.children.length) {
          diffTree.push(relationships);
        }
        const modelRelation = relationships.children
          .find(mr => mr.label === `${modelLeft}-${modelRight}`) ??
          { label: `${modelLeft}-${modelRight}`, nodeType: nodeTypes.MODEL_RELATION, children: [] };

        if (!modelRelation.children.length) {
          relationships.children.push(modelRelation);
        }

        modelRelation.children.push({ label: `${tableLeft}-${tableRight}`, nodeType: nodeTypes.COMMON_TABLE });
        diffCount += 1;
      }
      // 表、度量
      if ([nodeTypes.TABLE, nodeTypes.MEASURE].includes(item.type)) {
        const [modelName, entityName] = item.detail.split('.');
        // 如果新增表属于新模型，则不加进tree
        if (!newModelItems.some(mItem => mItem.detail === modelName)) {
          const model = diffTree
            .find(node => node.nodeType === nodeTypes.MODEL && node.label === modelName) ||
            { label: modelName, nodeType: nodeTypes.MODEL, children: [] };
          if (!model.children.length) {
            diffTree.push(model);
          }
          model.children.push({ label: entityName, nodeType: item.type, children: [] });
          diffCount += 1;
        }
      }
      // 维度列、层级维度
      if ([nodeTypes.DIM_COLUMN, nodeTypes.HIERARCHY].includes(item.type)) {
        const [modelName, tableName, entityName] = item.detail.split('.');
        // 如果新增项目属于新模型且属于新表，则不加进tree
        if (
          !newModelItems.some(mItem => mItem.detail === modelName) &&
          !newTableItems.some(tItem => tItem.detail === `${modelName}.${tableName}`)
        ) {
          const model = diffTree
            .find(node => node.nodeType === nodeTypes.MODEL && node.label === modelName) ||
            { label: modelName, nodeType: nodeTypes.MODEL, children: [] };
          const table = model.children
            .find(node => node.nodeType === nodeTypes.TABLE && node.label === tableName) ||
            { label: tableName, nodeType: nodeTypes.TABLE, children: [] };
          if (!model.children.length) {
            diffTree.push(model);
          }
          if (!table.children.length) {
            model.children.push(table);
          }
          table.children.push({ label: entityName, nodeType: item.type });
          diffCount += 1;
        }
      }
      // 命名集
      if ([nodeTypes.NAMEDSET].includes(item.type)) {
        const detailItems = item.detail.split('.');
        if (detailItems.length === 2) {
          const [rootName, namedSetName] = detailItems;
          const namedSetRoot = diffTree
            .find(node => node.nodeType === nodeTypes.NAMEDSET_ROOT) ||
            { label: rootName, nodeType: nodeTypes.NAMEDSET_ROOT, children: [] };
          if (!namedSetRoot.children.length) {
            diffTree.push(namedSetRoot);
          }
          namedSetRoot.children.push({ label: namedSetName, nodeType: item.type });
          diffCount += 1;
        } else if (detailItems.length === 3) {
          const [modelName, tableName, entityName] = detailItems;
          // 如果新增项目属于新模型且属于新表，则不加进tree
          if (
            !newModelItems.some(mItem => mItem.detail === modelName) &&
            !newTableItems.some(tItem => tItem.detail === `${modelName}.${tableName}`)
          ) {
            const model = diffTree
              .find(node => node.nodeType === nodeTypes.MODEL && node.label === modelName) ||
              { label: modelName, nodeType: nodeTypes.MODEL, children: [] };
            const table = model.children
              .find(node => node.nodeType === nodeTypes.TABLE && node.label === tableName) ||
              { label: tableName, nodeType: nodeTypes.TABLE, children: [] };
            if (!model.children.length) {
              diffTree.push(model);
            }
            if (!table.children.length) {
              model.children.push(table);
            }
            table.children.push({ label: entityName, nodeType: item.type });
            diffCount += 1;
          }
        }
      }
      // 计算度量
      if ([nodeTypes.CALCULATE_MEASURE].includes(item.type)) {
        const [folderGroup, cMeasureName] = item.detail.split('.');
        if (folderGroup === 'Calculated Measure') {
          const cMeasureRoot = diffTree
            .find(node => node.nodeType === nodeTypes.CALCULATE_MEASURE_ROOT) ||
            { label: 'Calculated Measure', nodeType: nodeTypes.CALCULATE_MEASURE_ROOT, children: [] };
          if (!cMeasureRoot.children.length) {
            diffTree.push(cMeasureRoot);
          }
          cMeasureRoot.children.push({ label: cMeasureName, nodeType: item.type });
          diffCount += 1;
        } else if (!newModelItems.some(mItem => mItem.detail === folderGroup)) {
          // 如果新增项目属于新模型，则不加进tree
          const model = diffTree
            .find(node => node.nodeType === nodeTypes.MODEL && node.label === folderGroup) ||
            { label: folderGroup, nodeType: nodeTypes.MODEL, children: [] };
          if (!model.children.length) {
            diffTree.push(model);
          }
          model.children.push({ label: cMeasureName, nodeType: item.type });
          diffCount += 1;
        }
      }
    }
  } catch (e) {
    domHelper.error(`getNewOrDeleteDiffer(${differType})`, e);
  }

  return {
    diffTree,
    diffCount,
  };
}

export const getSelectedDiffer = createSelector(
  getSelectedDifference,
  difference => {
    const { diffTree: new_items, diffCount: new_item_count } = getNewOrDeleteDiffer(difference, 'new_items');
    const { diffTree: reduce_items, diffCount: reduce_item_count } = getNewOrDeleteDiffer(difference, 'reduce_items');
    const { update_items } = difference;
    return {
      new_items,
      new_item_count,
      reduce_items,
      reduce_item_count,
      update_items,
    };
  },
);

export const getColumns = createSelector(
  state => state.intl,
  state => state.handleChangeType,
  state => state.handleChangeACL,
  state => state.handleChangeName,
  (intl, handleChangeType, handleChangeACL, handleChangeName) => {
    const { formatMessage } = intl;
    return [
      {
        label: formatMessage(strings.DATASET_NAME),
        prop: 'name',
        className: 'dataset-name',
        render: (row, column, idx) => {
          const { type, id, name } = row;

          let status = <i className="icon-superset-error_01 dataset-status" />;
          let display = <span>{name}</span>;

          if (type === ACTION_TYPE.ADD_NEW) {
            status = null;
            display = (
              <Form.Item prop={`datasets:${idx}`}>
                <i className="icon-superset-good_health dataset-status" />
                <Input value={name} onChange={value => handleChangeName(id, value)} />
              </Form.Item>
            );
          } else if (type === ACTION_TYPE.OVERRIDE) {
            status = <i className="icon-superset-warning_16 dataset-status" />;
          }

          return <>{status}{display}</>;
        },
      },
      {
        label: formatMessage(strings.ACTION),
        width: 120,
        className: 'dataset-action',
        render: row => {
          const { canCreate, canOverride, type, id } = row;

          return (
            <Select defaultValue={type} size="small" onChange={value => handleChangeType(id, value)}>
              {canCreate && (
                <Select.Option value={ACTION_TYPE.ADD_NEW}>
                  {formatMessage(strings.ADD_NEW)}
                </Select.Option>
              )}
              {canOverride && (
                <Select.Option value={ACTION_TYPE.OVERRIDE}>
                  {formatMessage(strings.REPLACE)}
                </Select.Option>
              )}
              <Select.Option value={ACTION_TYPE.NOT_IMPORT}>
                {formatMessage(strings.NOT_IMPORT)}
              </Select.Option>
            </Select>
          );
        },
      },
      {
        label: formatMessage(strings.IMPORT_ACL),
        width: 100,
        className: 'dataset-acl',
        render: row => {
          const { canCreate, canOverride, acl, id } = row;
          return (
            <Switch
              disabled={!canCreate && !canOverride}
              defaultChecked={acl}
              onChange={value => handleChangeACL(id, value)}
            />
          );
        },
      },
    ];
  },
);

export const validator = {
  datasetName(props, idx) {
    return async (rule, value, callback) => {
      const { intl, boundDatasetActions, form, currentProject } = props;
      const { datasets } = form;
      const datasetName = datasets[idx].name;
      const { name: project } = currentProject;
      const otherDatasets = datasets.filter((dataset, datasetIdx) => datasetIdx !== idx);

      if (!datasetName) {
        callback(new Error(intl.formatMessage(strings.SHOULD_HAVE_DATASET_NAME)));
      } else if (!business.nameRegExpInDatasetName.test(datasetName)) {
        callback(new Error(intl.formatMessage(strings.INVALID_NAME_WITH_CHINESE)));
      } else if (datasetName.length > configs.datasetMaxLength.datasetName) {
        const maxLength = configs.datasetMaxLength.datasetName;
        callback(new Error(intl.formatMessage(strings.DATASET_NAME_TOO_LONG, { maxLength })));
      } else if (
        otherDatasets.some(dataset => dataset.name === datasetName) ||
        await boundDatasetActions.checkDatasetName({ datasetName, project, type: 'MDX' })
      ) {
        callback(new Error(intl.formatMessage(strings.DATASET_NAME_DUPLICATE)));
      } else {
        callback();
      }
    };
  },
};
