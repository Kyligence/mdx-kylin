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
import { Collapse, LazyList, Tooltip } from 'kyligence-ui-react';
import classnames from 'classnames';

import './index.less';
import { strings } from '../../constants';
import { InjectIntl } from '../../store';

export default
@InjectIntl()
class CollapseList extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    rowKey: PropTypes.string.isRequired,
    activeItem: PropTypes.object.isRequired,
    title: PropTypes.string,
    rows: PropTypes.array,
    onClick: PropTypes.func,
    filter: PropTypes.string,
    emptyText: PropTypes.string,
    actions: PropTypes.array,
  };

  static defaultProps = {
    title: '',
    rows: [],
    onClick: () => {},
    filter: '',
    emptyText: '',
    actions: [],
  };

  get emptyText() {
    const { emptyText, intl } = this.props;
    return !emptyText
      ? intl.formatMessage(strings.NO_DATA)
      : emptyText;
  }

  get filteredRows() {
    const { rows } = this.props;
    return rows.filter(row => this.isRowMatchFilter(row));
  }

  getCollapseItemClass(row) {
    const { rowKey, activeItem } = this.props;
    if (activeItem) {
      return classnames({
        'collapse-item': true,
        active: row[rowKey] === activeItem[rowKey],
      });
    }
    return 'collapse-item';
  }

  handleClickItem(row) {
    const { onClick } = this.props;
    onClick(row);
  }

  isRowMatchFilter(row) {
    const { rowKey, filter } = this.props;
    const rowValue = row[rowKey].toString().toLowerCase();
    const filterValue = filter.toLowerCase();

    return rowValue.includes(filterValue) || filterValue === '';
  }

  /* eslint-disable react/no-array-index-key */
  render() {
    const { title, rowKey, actions } = this.props;
    const { emptyText, filteredRows } = this;

    return (
      <div className="collapse-list">
        <Collapse value="1">
          <Collapse.Item title={title} name="1">
            <LazyList renderItemSize={36} delayMs={300}>
              {filteredRows.length ? filteredRows.map((row, index) => (
                <div
                  key={index}
                  className={this.getCollapseItemClass(row)}
                  onClick={() => this.handleClickItem(row)}
                >
                  <span>{row[rowKey]}</span>
                  <div className="collapse-list-actions">
                    {actions.map(({ icon, handler, label }) => (
                      <Tooltip
                        positionFixed
                        className="collapse-list-action-item"
                        effect="dark"
                        placement="top"
                        key={index}
                        content={label}
                      >
                        <span
                          className={icon}
                          onClick={event => {
                            event.stopPropagation();
                            handler(row);
                          }}
                        />
                      </Tooltip>
                    ))}
                  </div>
                </div>
              )) : (
                <div className="empty-text">{emptyText}</div>
              )}
            </LazyList>
          </Collapse.Item>
        </Collapse>
      </div>
    );
  }
}
