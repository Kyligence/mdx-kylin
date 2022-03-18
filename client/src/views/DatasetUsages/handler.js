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
import React, { Fragment } from 'react';
import { Tag, Tooltip } from 'kyligence-ui-react';
import { strings } from '../../constants';

/* eslint-disable react/jsx-filename-extension */
export function getColumnDefination({ intl }, models = [], handleDimensionUsage) {
  const tableRelationStringMaps = {
    0: intl.formatMessage(strings.JOINT),
    1: intl.formatMessage(strings.NOT_JOINT),
    2: intl.formatMessage(strings.MANY_TO_MANY),
  };

  return [
    {
      label: intl.formatMessage(strings.DIMENSION_TABLES),
      prop: 'table',
      fixed: 'left',
      minWidth: 200,
      render: row => {
        const { table: rowTable } = row;
        const publicTableLabel = intl.formatMessage(strings.PUBLIC_DIMENSION_TABLE);

        const isPublicTable = rowTable.indexOf(`[${publicTableLabel}] `) === 0;
        const tableName = rowTable.replace(`[${publicTableLabel}] `, '');
        const style = isPublicTable ? { verticalAlign: 'middle' } : {};
        return (
          <Fragment>
            {isPublicTable && (
              <Tag className="public-table" type="success" style={style}>{publicTableLabel}</Tag>
            )}
            <span style={style}>{tableName}</span>
          </Fragment>
        );
      },
    },
    {
      label: (
        <div style={{ textAlign: 'center' }}>
          {intl.formatMessage(strings.MODEL_NAME)}
        </div>
      ),
      subColumns: models.map(model => ({
        label: model,
        prop: model,
        minWidth: model.length * 10 + 18 * 2,
        render: row => {
          const { [model]: rowModel } = row;
          return rowModel ? (
            <div className="clearfix relation">
              {rowModel.bridgeTableError || rowModel.factKeyError ? (
                <i className="error-icon icon-superset-alert" />
              ) : null}
              <span>{tableRelationStringMaps[rowModel.relationType]}</span>
              <span className="text-button pull-right" onClick={() => handleDimensionUsage(model, rowModel)}>
                <Tooltip appendToBody placement="top" content={intl.formatMessage(strings.EDIT)}>
                  <i className="icon-superset-table_edit" />
                </Tooltip>
              </span>
            </div>
          ) : (
            <span>{intl.formatMessage(strings.UNABLE_TO_LINK)}</span>
          );
        },
      })),
    },
  ];
}
