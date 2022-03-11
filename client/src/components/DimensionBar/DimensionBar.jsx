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
import React, { PureComponent, Fragment } from 'react';
import PropTypes from 'prop-types';
import { injectIntl } from 'react-intl';
import { Card, Tree, Input, Select, Tooltip, OverflowTooltip } from 'kyligence-ui-react';
import classnames from 'classnames';

import './index.less';
import { Connect } from '../../store';
import { strings, configs } from '../../constants';
import { dataHelper, domHelper, datasetHelper } from '../../utils';
import { getNodeIcon, getFilteredNodeTypes, findDefaultTable, findDefaultNamedSetRoot, checkValidNodeType, hasHierarchyError, hasNameColumnError, hasValueColumnError, hasPropertiesError, checkIsCreatedNode, checkIsEditedNode, hasDefaultMemberError } from './handler';

const { translatableNodeTypes } = configs;
@Connect({
  mapState: {
    dataset: state => state.workspace.dataset,
  },
  options: {
    forwardRef: true,
  },
})
class DimensionBar extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    onClick: PropTypes.func,
    onEdit: PropTypes.func,
    onCreate: PropTypes.func,
    shouldNodeRender: PropTypes.func,
    hideArrowWhenNoLeaves: PropTypes.bool,
    editableNodeTypes: PropTypes.array,
    isOnlyTree: PropTypes.bool,
    isLazy: PropTypes.bool,
    currentNode: PropTypes.object,
    renderEmpty: PropTypes.oneOfType([PropTypes.func, PropTypes.string, PropTypes.node]),
  };

  static defaultProps = {
    editableNodeTypes: [],
    shouldNodeRender: () => true,
    hideArrowWhenNoLeaves: false,
    isOnlyTree: false,
    currentNode: null,
    isLazy: false,
    renderEmpty: undefined,
    onClick: () => {},
    onEdit: () => {},
    onCreate: () => {},
  };

  $tree = React.createRef();
  $treeWrapper = React.createRef();
  scrollTop = 0;

  state = {
    isShowTree: true,
    expandedNodeKeysMap: {},
    currentNode: null,
    filter: {
      nodeType: 'column',
      filteredNodeTypes: getFilteredNodeTypes({ nodeType: 'column' }),
      name: '',
    },
  };

  constructor(props) {
    super(props);
    this.state.currentNode = props.currentNode;
    this.renderNode = this.renderNode.bind(this);
    this.handleScroll = this.handleScroll.bind(this);
    this.handleClickNode = this.handleClickNode.bind(this);
    this.handleFilterType = this.handleFilterType.bind(this);
    this.handleFilterName = this.handleFilterName.bind(this);
    this.handleFilterNode = this.handleFilterNode.bind(this);
  }

  componentDidMount() {
    this.refreshTree(() => {
      this.heightlightCurrentNode();
    });
  }

  /* eslint-disable camelcase */
  async UNSAFE_componentWillReceiveProps(nextProps) {
    const { dataset: oldDataset, currentNode: oldCurrentNode } = this.props;
    const { dataset: newDataset, currentNode: newCurrentNode } = nextProps;
    const isCreatedNode = checkIsCreatedNode(oldCurrentNode, newCurrentNode);
    const isEditedNode = checkIsEditedNode(oldCurrentNode, newCurrentNode);
    const isValidNodeType = checkValidNodeType(newCurrentNode);

    if (isValidNodeType) {
      // 如果是合法节点，处理从父级展开到叶子节点
      await this.setState({ currentNode: newCurrentNode });
      this.setExpandNodes();
    } else {
      // 如果是不合法节点，节点清空高亮
      await this.setState({ currentNode: null });
      this.$tree.current.clearCurrentNodeKey();
    }
    // 处理树刷新逻辑
    if (isCreatedNode || isEditedNode || oldDataset !== newDataset) {
      await this.refreshTree(isValidNodeType);
      this.heightlightCurrentNode();
    }
  }

  componentWillUnmount() {
    this.removeEventListener();
  }

  get expandedKeys() {
    const { expandedNodeKeysMap } = this.state;
    return Object
      .values(expandedNodeKeysMap)
      .reduce((allExpandedKeys, expandedKeys) => (
        [...allExpandedKeys, ...expandedKeys]
      ), []);
  }

  get filterOptions() {
    const { intl } = this.props;
    return [
      {
        label: intl.formatMessage(strings.DIMENSION_TABLE),
        value: 'table',
        icon: getNodeIcon({ nodeType: 'table' }),
      },
      {
        label: intl.formatMessage(strings.DIMENSION),
        value: 'column',
        icon: getNodeIcon({ nodeType: 'column' }),
      },
      {
        label: intl.formatMessage(strings.HIERARCHY),
        value: 'hierarchy',
        icon: getNodeIcon({ nodeType: 'hierarchy' }),
      },
      {
        label: intl.formatMessage(strings.NAMEDSET),
        value: 'namedSet',
        icon: getNodeIcon({ nodeType: 'namedSet' }),
      },
    ];
  }

  get dimensionTree() {
    const { dataset } = this.props;
    return datasetHelper.getDimensionTree({ dataset });
  }

  setExpandModel(item) {
    const modelKey = item.model;
    const expandModel = { nodeType: 'model', key: modelKey };
    return this.handleNodesExpand([expandModel]);
  }

  setExpandTable(item) {
    const modelKey = item.model;
    const tableKey = `${item.model}-${item.table}`;

    const expandModel = { nodeType: 'model', key: modelKey };
    const expandTable = { nodeType: 'table', key: tableKey };

    return this.handleNodesExpand([expandModel, expandTable]);
  }

  setExpandNamedSet(item) {
    const isInRootSet = item.location === 'Named Set';
    // 用于维度树展示全局命名集
    if (isInRootSet) {
      const expandNamedSetRoot = { nodeType: 'namedSetRoot', key: 'namedSetRoot' };
      return this.handleNodesExpand([expandNamedSetRoot]);
    }
    // 用于维度树展示表级命名集
    if (!isInRootSet) {
      const [model, table] = item.location.split('.');
      const tableKey = `${model}-${table}`;
      const rootSetKey = `${tableKey}-namedSetRoot`;

      const expandModel = { nodeType: 'model', key: model };
      const expandTable = { nodeType: 'table', key: tableKey };
      const expandNamedSetRoot = { nodeType: 'namedSetRoot', key: rootSetKey };

      return this.handleNodesExpand([expandModel, expandTable, expandNamedSetRoot]);
    }
    return false;
  }

  setExpandNodes() {
    const { currentNode } = this.state;
    const isEditingNode = currentNode && !!currentNode.name;

    if (isEditingNode) {
      switch (currentNode.nodeType) {
        // 维度表：在维度列表展开到模型
        case 'table': return this.setExpandModel(currentNode);
        // 维度列 / 层级维度：在维度列表展开到表
        case 'column':
        case 'hierarchy': return this.setExpandTable(currentNode);
        // 命名集：在维度列表展开到命名集根节点
        case 'namedSet': return this.setExpandNamedSet(currentNode);
        default: return false;
      }
    }
    return false;
  }

  heightlightCurrentNode() {
    const $treeRef = this.$tree.current;
    // 获取需要高亮的节点
    const { currentNode: heightlightNode } = this.state;
    // 树组件未挂载/没有高亮节点 防止调用函数报错
    if ($treeRef && heightlightNode) {
      const { props = {} } = $treeRef.getCurrentNode() || {};
      const { nodeModel = {} } = props;
      const { data = {} } = nodeModel;

      // 如果 树高亮节点 与 需要被高亮节点 相同，则不调用高亮函数，防止当前节点被折叠
      if (data.key !== heightlightNode.key) {
        $treeRef.setCurrentNodeKey(heightlightNode.key);
      }
    }
  }

  scrollToNode() {
    const { currentNode } = this.state;
    const { $tree, $treeWrapper } = this;
    if (currentNode && $treeWrapper.current) {
      if ($tree.current) {
        // 组件库bug：root.expanded默认为关，然后就不动了，这边手动打开一下
        $tree.current.state.store.root.expanded = true;
        const position = $tree.current.getNodePosition(currentNode.key);
        domHelper.scrollTo($treeWrapper.current, position);
      }
    }
  }

  scrollToLastPosition() {
    const { $treeWrapper, scrollTop } = this;
    if ($treeWrapper.current) {
      domHelper.scrollTo($treeWrapper.current, scrollTop);
    }
  }

  addEventListener() {
    const $treeWrapperRef = this.$treeWrapper.current;
    if ($treeWrapperRef) {
      $treeWrapperRef.addEventListener('scroll', this.handleScroll);
    }
  }

  removeEventListener() {
    const $treeWrapperRef = this.$treeWrapper.current;
    if ($treeWrapperRef) {
      $treeWrapperRef.removeEventListener('scroll', this.handleScroll);
    }
  }

  refreshTree(isValidNodeType) {
    return new Promise(resolve => {
      // 销毁树，并且移除事件监听
      this.removeEventListener();
      this.setState({ isShowTree: false }, () => {
        // 展现树，并且添加事件监听
        this.setState({ isShowTree: true }, () => {
          this.addEventListener();
          setTimeout(() => {
            // 滚动到高亮节点
            if (isValidNodeType) {
              this.scrollToNode();
            } else {
              // 否则滚动到上一次的位置
              this.scrollToLastPosition();
            }
            resolve();
          }, 300);
        });
      });
    });
  }

  async selectDefaultTable() {
    const { dimensionTree } = this;
    const defaultTable = findDefaultTable(dimensionTree);

    if (defaultTable) {
      this.setExpandModel(defaultTable);

      await this.refreshTree();
      this.$tree.current.setCurrentNodeKey(defaultTable.key);
    }
  }

  async selectDefaultNamedSet() {
    const { dimensionTree } = this;
    const defaultNamedSetRoot = findDefaultNamedSetRoot(dimensionTree);

    if (defaultNamedSetRoot) {
      this.setExpandTable(defaultNamedSetRoot);

      await this.refreshTree();
      this.$tree.current.setCurrentNodeKey(defaultNamedSetRoot.key);
    }
  }

  handleScroll() {
    const $treeWrapperRef = this.$treeWrapper.current;
    if ($treeWrapperRef) {
      this.scrollTop = $treeWrapperRef.scrollTop;
    }
  }

  handleClickNew(type) {
    const { onCreate } = this.props;
    if (onCreate) {
      onCreate(type);
    }
  }

  handleClickNode(data, isEdit, event) {
    const { onClick, onEdit } = this.props;
    const onClickFunc = isEdit ? onEdit : onClick;

    if (isEdit && event) {
      event.stopPropagation();
    }

    if (onClickFunc) {
      onClickFunc(data);
    }
  }

  handleNodesExpand(nodesData = []) {
    let isNodesNotExpanded = false;
    let { expandedNodeKeysMap } = this.state;

    for (const nodeData of nodesData) {
      const { nodeType, key } = nodeData;

      if (!expandedNodeKeysMap[nodeType]) {
        expandedNodeKeysMap[nodeType] = [];
      }

      const expendedNodeKeys = expandedNodeKeysMap[nodeType];
      const isNotExpanded = !expendedNodeKeys.includes(key);

      if (isNotExpanded) {
        expandedNodeKeysMap = {
          ...expandedNodeKeysMap,
          [nodeType]: [...expendedNodeKeys, key],
        };
      }

      isNodesNotExpanded = isNodesNotExpanded || isNotExpanded;
    }

    this.setState({ expandedNodeKeysMap });

    return isNodesNotExpanded;
  }

  handleNodesCollapse(nodesData = []) {
    let isNodesExpanded = false;
    let { expandedNodeKeysMap } = this.state;

    for (const nodeData of nodesData) {
      const { nodeType, key } = nodeData;

      if (!expandedNodeKeysMap[nodeType]) {
        expandedNodeKeysMap[nodeType] = [];
      }

      const expendedNodeKeys = expandedNodeKeysMap[nodeType];
      const isExpanded = expendedNodeKeys.includes(key);

      if (isExpanded) {
        expandedNodeKeysMap = {
          ...expandedNodeKeysMap,
          [nodeType]: expendedNodeKeys.filter(nodeKey => nodeKey !== key),
        };
      }

      isNodesExpanded = isNodesExpanded || isExpanded;
    }

    this.setState({ expandedNodeKeysMap });

    return isNodesExpanded;
  }

  handleFilterName(name) {
    const { filter: oldFilter } = this.state;
    const filter = { ...oldFilter, name };
    this.setState({ filter }, () => {
      const isSearchLeaf = filter.nodeType !== 'table';
      this.$tree.current.filter(filter, isSearchLeaf);
    });
  }

  handleFilterType(nodeType) {
    const { filter: oldFilter } = this.state;
    const filteredNodeTypes = getFilteredNodeTypes({ nodeType });
    const filter = { ...oldFilter, nodeType, filteredNodeTypes };
    this.setState({ filter }, () => {
      if (filter.name) {
        const isSearchLeaf = filter.nodeType !== 'table';
        this.$tree.current.filter(filter, isSearchLeaf);
      }
    });
  }

  handleFilterNode(filter, data) {
    const { intl } = this.props;
    const { filteredNodeTypes = [], name: input, nodeType } = filter;
    const isFilteredNode = filteredNodeTypes
      .filter(n => n !== nodeType)
      .includes(data.nodeType);

    if (!input) {
      return true;
    }

    if (data.alias) {
      const translateAlias = dataHelper.translate(intl, data.alias);
      const isFilteredAlias = translateAlias.toLowerCase().includes(input.toLowerCase());
      return isFilteredNode || (nodeType === data.nodeType && isFilteredAlias);
    }

    const translateName = dataHelper.translate(intl, data.name || data.label);
    const isFilteredName = translateName.toLowerCase().includes(input.toLowerCase());
    return isFilteredNode || (nodeType === data.nodeType && isFilteredName);
  }

  renderCardHeader() {
    const { intl } = this.props;
    return (
      <div className="clearfix">
        <div className="card-title">{intl.formatMessage(strings.DIMENSION)}</div>
        <div className="card-actions">
          <Tooltip
            appendToBody
            className="card-action"
            placement="top"
            content={intl.formatMessage(strings.ADD_HIERARCHY)}
          >
            <i className="mdx-it-add-hierarchy icon-superset-add_hierachy" onClick={() => this.handleClickNew('hierarchy')} />
          </Tooltip>
          <Tooltip
            appendToBody
            className="card-action"
            placement="top"
            content={intl.formatMessage(strings.ADD_NAMEDSET)}
          >
            <i className="mdx-it-add-namedset icon-superset-add_named_set" onClick={() => this.handleClickNew('namedSet')} />
          </Tooltip>
        </div>
      </div>
    );
  }

  renderNode(node, data) {
    const { editableNodeTypes, intl } = this.props;
    const isShowEdit = editableNodeTypes.includes(data.nodeType);
    const label = translatableNodeTypes.includes(data.nodeType)
      ? dataHelper.translate(intl, data.label)
      : data.label;

    let hasError = false;
    switch (data.nodeType) {
      case 'model': {
        hasError = data.children.some(table => (
          table.children.some(hierarchy => hasHierarchyError(hierarchy)) ||
          table.children.some(namedSetRoot => namedSetRoot.children && (
            namedSetRoot.children.some(({ error }) => error)
          )) ||
          table.children.some(column => (
            hasNameColumnError(column) ||
            hasValueColumnError(column) ||
            hasPropertiesError(column) ||
            hasDefaultMemberError(column)
          ))
        ));
        break;
      }
      case 'table': {
        hasError = data.children.some(hierarchy => hasHierarchyError(hierarchy)) ||
          data.children.some(namedSetRoot => namedSetRoot.children && (
            namedSetRoot.children.some(({ error }) => error)
          )) ||
          data.children.some(column => (
            hasNameColumnError(column) ||
            hasValueColumnError(column) ||
            hasPropertiesError(column) ||
            hasDefaultMemberError(column)
          ));
        break;
      }
      case 'hierarchy': {
        hasError = hasHierarchyError(data);
        break;
      }
      case 'namedSetRoot': {
        hasError = data.children.some(({ error }) => error);
        break;
      }
      case 'namedSet': {
        hasError = !!data.error;
        break;
      }
      case 'column': {
        hasError = hasNameColumnError(data) ||
          hasValueColumnError(data) ||
          hasPropertiesError(data) ||
          hasDefaultMemberError(data);
        break;
      }
      default: break;
    }

    return (
      <Fragment>
        <span className="node-content" id={data.key}>
          <OverflowTooltip content={label}>
            <span>
              {hasError && <i className="error-icon icon-superset-alert" />}
              <i className={classnames(['perfix-icon', getNodeIcon(data)])} />
              <span style={{ whiteSpace: 'pre' }}>{label}</span>
            </span>
          </OverflowTooltip>
        </span>
        {isShowEdit && (
          <i
            className="action-item icon-superset-table_edit"
            onClick={event => this.handleClickNode(data, true, event)}
          />
        )}
      </Fragment>
    );
  }

  /* eslint-disable max-len */
  render() {
    const { intl, isOnlyTree, shouldNodeRender, hideArrowWhenNoLeaves, isLazy, renderEmpty } = this.props;
    const { isShowTree, filter } = this.state;
    const { $tree, $treeWrapper, filterOptions, dimensionTree } = this;
    const filterIcon = getNodeIcon({ nodeType: filter.nodeType });

    return !isOnlyTree ? (
      <Card className="dimension-bar" header={this.renderCardHeader()} bodyStyle={{ padding: null }}>
        <div className="actions clearfix">
          <Select
            className="icon-mode filter-type mdx-it-dimension-bar-type-filter"
            prefixIcon={filterIcon}
            value={filter.nodeType}
            onChange={this.handleFilterType}
          >
            {filterOptions.map(option => (
              <Select.Option key={option.value} value={option.value}>
                <Tooltip
                  appendToBody
                  className="option-item"
                  popperClass="option-item-tooltip"
                  placement="top"
                  content={option.label}
                >
                  <i className={option.icon} />
                </Tooltip>
              </Select.Option>
            ))}
          </Select>
          <Input
            className="filter-name mdx-it-dimension-bar-name-filter"
            prefixIcon="icon-superset-search"
            value={filter.name}
            onChange={this.handleFilterName}
            placeholder={intl.formatMessage(strings.FILTER)}
          />
        </div>
        {isShowTree && (
          <div className="dimension-tree" ref={$treeWrapper}>
            <Tree
              isLazy={isLazy}
              ref={$tree}
              highlightCurrent
              nodeKey="key"
              autoExpandParent={false}
              data={dimensionTree}
              defaultExpandedKeys={this.expandedKeys}
              renderContent={this.renderNode}
              emptyText={intl.formatMessage(strings.NO_DATA)}
              renderEmpty={renderEmpty}
              hideArrowWhenNoLeaves={hideArrowWhenNoLeaves}
              filterNodeMethod={this.handleFilterNode}
              shouldNodeRender={shouldNodeRender}
              onNodeClicked={data => this.handleClickNode(data, false)}
              onNodeExpand={data => this.handleNodesExpand([data])}
              onNodeCollapse={data => this.handleNodesCollapse([data])}
            />
          </div>
        )}
      </Card>
    ) : isShowTree && (
      <div className="dimension-bar is-only-tree" ref={$treeWrapper}>
        <Tree
          isLazy={isLazy}
          ref={$tree}
          highlightCurrent
          nodeKey="key"
          autoExpandParent={false}
          data={dimensionTree}
          defaultExpandedKeys={this.expandedKeys}
          renderContent={this.renderNode}
          emptyText={intl.formatMessage(strings.NO_DATA)}
          renderEmpty={renderEmpty}
          hideArrowWhenNoLeaves={hideArrowWhenNoLeaves}
          filterNodeMethod={this.handleFilterNode}
          shouldNodeRender={shouldNodeRender}
          onNodeClicked={data => this.handleClickNode(data, false)}
          onNodeExpand={data => this.handleNodesExpand([data])}
          onNodeCollapse={data => this.handleNodesCollapse([data])}
        />
      </div>
    );
  }
  /* eslint-enable */
}

export default injectIntl(DimensionBar, { forwardRef: true });
