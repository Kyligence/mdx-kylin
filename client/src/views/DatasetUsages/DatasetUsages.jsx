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
import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import { Layout, Table } from 'kyligence-ui-react';

import './index.less';
import { getColumnDefination } from './handler';
import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { dataHelper } from '../../utils';
import Pagination from '../../components/Pagination/Pagination';
import { getPublicTableMap } from '../../utils/datasetHelper';

export default
@Connect({
  mapState: {
    dataset: state => state.workspace.dataset,
    modelRelations: state => state.workspace.dataset.modelRelations,
  },
})
@InjectIntl()
class DatasetUsages extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    modelRelations: PropTypes.array.isRequired,
  };

  state = {
    pageOffset: 0,
    pageSize: 10,
  };

  constructor(props) {
    super(props);
    this.handlePagination = this.handlePagination.bind(this);
    this.handleDimensionUsage = this.handleDimensionUsage.bind(this);
  }

  get models() {
    const { modelRelations } = this.props;
    const allModelsMap = modelRelations.reduce((map, relation) => (
      { ...map, [relation.modelLeft]: true, [relation.modelRight]: true }
    ), {});
    return Object.keys(allModelsMap).filter(m => m);
  }

  get columns() {
    return getColumnDefination(this.props, this.models, this.handleDimensionUsage);
  }

  get tableData() {
    const { dataset, intl } = this.props;
    const { pageOffset, pageSize } = this.state;
    const relationMap = {};

    const publicTableMap = getPublicTableMap(dataset);

    for (const modelUsage of dataset.dimTableModelRelations) {
      const { modelName } = modelUsage;
      for (const tableRelation of modelUsage.tableRelations) {
        const { tableName } = tableRelation;
        const publicTableName = publicTableMap[`${modelName}.${tableName}`];

        let tableAlias;
        // 如果不是公共维度表，则从models中查找table的别名
        if (!publicTableName) {
          tableAlias = dataset.models
            .find(m => m.name === modelName).dimensionTables
            .find(t => t.name === tableName).alias;
        } else {
          // 如果是公共维度表，则用公共维表的真名，去models中查找table的别名
          for (const model of dataset.models) {
            const currentModel = model.dimensionTables.find(t => `${model.name}.${t.name}` === publicTableName);
            if (currentModel) {
              tableAlias = currentModel.alias;
              break;
            }
          }
        }

        const publicTableLabel = intl.formatMessage(strings.PUBLIC_DIMENSION_TABLE);
        const displayTableName = publicTableName
          ? `[${publicTableLabel}] ${tableAlias}`
          : `${modelName}.${tableAlias}`;

        if (!relationMap[displayTableName]) {
          relationMap[displayTableName] = { [modelName]: tableRelation };
        } else {
          relationMap[displayTableName][modelName] = tableRelation;
        }
      }
    }

    const datas = Object.entries(relationMap).map(([table, links]) => ({ table, ...links }))
      .sort((itemA, itemB) => {
        if (itemA.table[0] === '[' && itemB.table[0] !== '[') {
          return -1;
        }
        if (itemA.table[0] !== '[' && itemB.table[0] === '[') {
          return 1;
        }
        return itemA.table < itemB.table ? -1 : 1;
      });

    const { data, ...other } = dataHelper.getPaginationTable({ datas, pageOffset, pageSize });
    return {
      ...other,
      data,
    };
  }

  handleDimensionUsage(model, relation) {
    const { boundModalActions } = this.props;

    boundModalActions.setModalData('DimensionUsageModal', { model });
    boundModalActions.showModal('DimensionUsageModal', relation);
  }

  handlePagination(pageOffset, pageSize) {
    this.setState({ pageOffset, pageSize });
  }

  render() {
    const { tableData, columns } = this;
    const { intl } = this.props;

    return (
      <Layout.Row className="dimension-usages">
        <Layout.Col span={24}>
          <Table
            border
            style={{ width: '100%' }}
            columns={columns}
            data={tableData.data}
            emptyText={intl.formatMessage(strings.NO_DATA)}
          />
          <Pagination
            style={{ marginTop: '20px' }}
            totalCount={tableData.totalCount}
            pageSize={tableData.pageSize}
            pageOffset={tableData.pageOffset}
            onPagination={this.handlePagination}
          />
        </Layout.Col>
      </Layout.Row>
    );
  }
}
