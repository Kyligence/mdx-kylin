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
