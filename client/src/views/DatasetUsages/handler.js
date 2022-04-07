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
