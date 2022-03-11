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
import classNames from 'classnames';
import { Alert, AutoComplete, Form, Tree, Popover } from 'kyligence-ui-react';

import './index.less';
import FolderOption from './FolderOption';
import { getPopoverFolderTree, getSuggestionsFolderTree } from './handler';
import { InjectIntl } from '../../store';
import { configs, strings } from '../../constants';

const { nodeIconMaps } = configs;

export default
@InjectIntl()
class FolderInput extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    name: PropTypes.string.isRequired,
    nodeType: PropTypes.string.isRequired,
    prop: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    options: PropTypes.arrayOf(PropTypes.shape({
      label: PropTypes.node.isRequired,
      value: PropTypes.string.isRequired,
    })).isRequired,
  };

  get popoverFolderTree() {
    const { intl, name, nodeType } = this.props;
    return getPopoverFolderTree({ intl, name, nodeType });
  }

  get suggestionsFolderTree() {
    const { value, name, nodeType } = this.props;
    return getSuggestionsFolderTree({ value, name, nodeType });
  }

  getFolderTree = usage => (
    // 为了解决不能一个data用在两个树组件上的问题
    usage === 'popover' ? this.popoverFolderTree : this.suggestionsFolderTree
  )

  handleInput = value => {
    const { prop, onChange } = this.props;
    onChange(prop, value);
  }

  handleSelect = ({ value }) => {
    const { prop, onChange } = this.props;
    onChange(prop, value);
  }

  handleClearInput = () => {
    const { prop, onChange } = this.props;
    onChange(prop, '');
  }

  fetchSuggestions = (queryString, cb) => {
    const { options } = this.props;
    cb(options);
  }

  renderTreeNode = (node, data) => (
    <span className="node-content">
      <i className={classNames('node-icon', nodeIconMaps[data.nodeType])} />
      {data.label}
    </span>
  )

  renderFolderTree = usage => {
    const { intl } = this.props;
    const { formatMessage } = intl;

    return (
      <Tree
        className="display-folder-tree"
        defaultExpandAll
        emptyText={formatMessage(strings.NO_FOLDER)}
        data={this.getFolderTree(usage)}
        renderContent={this.renderTreeNode}
      />
    );
  }

  renderPopoverContent = () => {
    const { intl } = this.props;
    const { formatMessage } = intl;

    const example = formatMessage(strings.FOLDER_DESC_EXAMPLE);

    return (
      <>
        <div>
          {formatMessage(strings.FOLDER_DESC, { example })}
        </div>
        {this.renderFolderTree('popover')}
      </>
    );
  }

  renderLabel = () => {
    const { intl } = this.props;
    const { formatMessage } = intl;

    return (
      <>
        {formatMessage(strings.FOLDER)}
        <Popover placement="right" trigger="hover" width={366} popperClass="display-folder-popover" content={this.renderPopoverContent()}>
          <i className="icon-superset-what el-form-item__description" />
        </Popover>
      </>
    );
  }

  renderSuggestions = options => {
    const { intl } = this.props;
    const { formatMessage } = intl;
    return (
      <>
        <div className="folder-popover-header">
          {formatMessage(strings.FOLDER_PREVIEW)}
        </div>
        {this.renderFolderTree('suggestions')}
        <hr />
        <div className="folder-popover-header">
          {formatMessage(strings.RECENTLY_USED)}
        </div>
        {!options.length ? (
          <p className="el-select-dropdown__empty">
            {formatMessage(strings.NO_RECENTLY_USED)}
          </p>
        ) : <div className="option-list">{options}</div>}
      </>
    );
  }

  render() {
    const { intl, prop, value, nodeType } = this.props;
    const { formatMessage } = intl;

    return (
      <Form.Item
        prop={prop}
        className="folder-input"
        label={this.renderLabel()}
      >
        <Alert
          type="info"
          icon="icon-superset-infor"
          closable={false}
          title={formatMessage(strings.FOLDER_NAME_TIP)}
        />
        <AutoComplete
          value={value}
          className={`mdx-it-${nodeType}-folder-input`}
          popperProps={configs.disablePopperAutoFlip}
          onChange={this.handleInput}
          onSelect={this.handleSelect}
          fetchSuggestions={this.fetchSuggestions}
          renderSuggestions={this.renderSuggestions}
          customItem={FolderOption}
          closeWhenSelect={false}
          suffixIcon="icon-superset-error_01"
          onSuffixIconClick={this.handleClearInput}
        />
      </Form.Item>
    );
  }
}
