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
import React, { Fragment, PureComponent } from 'react';
import PropTypes from 'prop-types';
import { DropTarget } from 'react-dnd';
import { jsPlumb } from 'jsplumb';
import classnames from 'classnames';
import { Tooltip, MessageBox } from 'kyligence-ui-react';
import throttle from 'lodash/throttle';

import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import RelationMapModel from './RelationMapModel';
import DraggableElement from '../../components/DraggableElement/DraggableElement';

const ItemTypes = {
  DragableItem: 'DragableModelItem',
};

@Connect({
  mapState: {
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class DropModelContainer extends PureComponent {
  static wrapperSelector = '.dataset-relation .drop-model-container';
  static containerSelector = '.dataset-relation .drop-model-container .operation-area';

  $operationArea = React.createRef();

  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
    connectDropTarget: PropTypes.func.isRequired,
    isEditMode: PropTypes.bool.isRequired,
    intl: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    deleteModel: PropTypes.func.isRequired,
    content: PropTypes.array.isRequired,
    canDrop: PropTypes.bool.isRequired,
    isOver: PropTypes.bool.isRequired,
  };

  state = {
    instance: null,
    width: null,
    height: null,
  };

  constructor(props) {
    super(props);
    this.containerEl = null;
    this.handleDrag = this.handleDrag.bind(this);
    this.handleDragMove = this.handleDragMove.bind(this);
    this.handleDeleteModel = this.handleDeleteModel.bind(this);
    this.handleDestoryCanvas = this.handleDestoryCanvas.bind(this);
    this.handleBeforeConnect = this.handleBeforeConnect.bind(this);
    this.handleEditConnection = this.handleEditConnection.bind(this);
    this.handleDeleteConnection = this.handleDeleteConnection.bind(this);
    this.handleConnectionChanged = this.handleConnectionChanged.bind(this);
    this.renderConnectStyle = this.renderConnectStyle.bind(this);
    this.renderConnectOverlay = this.renderConnectOverlay.bind(this);
    this.setContainerStyleThrottle = throttle(this.setContainerStyle, 500);
  }

  componentDidMount() {
    this.updateDragDropContainer();
  }

  // If component is updated, the HtmlElement will recreated. It's crazy...
  componentDidUpdate() {
    this.updateDragDropContainer();
  }

  get containerClass() {
    const { canDrop, isOver } = this.props;
    return classnames('drop-model-container', canDrop && isOver && 'hover');
  }

  get containerStyle() {
    const { width, height } = this.state;
    return {
      width: `${width}px`,
      height: `${height}px`,
    };
  }

  setContainerStyle = maxSize => {
    const { x, y, width, height } = maxSize;
    const { width: containerW, height: containerH } = this.containerEl.getBoundingClientRect();

    const newW = x + width >= containerW ? x + width + 100 : containerW;
    const newH = y + height >= containerH ? y + height + 100 : containerH;

    this.setState({ width: newW, height: newH });
  }

  setContainerStyleThrottle = () => {};

  getModelCanvas(modelData) {
    const { dataset } = this.props;
    return dataset.canvas.models.find(item => item.name === modelData.name) || {};
  }

  updateDragDropContainer() {
    const { containerSelector } = DropModelContainer;
    const newContainerEl = document.querySelector(containerSelector);
    if (newContainerEl !== this.containerEl) {
      this.containerEl = newContainerEl;
      const instance = jsPlumb.getInstance();
      instance.setContainer(this.containerEl);
      this.setState({ instance: null }, () => {
        this.setState({ instance });
      });
    }
  }

  showDeleteAlert() {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.CONFIRM_DELETE_RELATIONS);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  async handleBeforeConnect({ source, target }, connect) {
    const { boundModalActions } = this.props;
    const form = {
      modelLeft: source.name,
      modelRight: target.name,
      name: `${source.name}&&${target.name}`,
      relation: [{ left: '', right: '' }],
    };
    const { isSubmit, relation } = await boundModalActions.showModal('AddDatasetRelationshipModal', form);
    if (isSubmit && relation.relation.length) { connect(); }
  }

  handleConnectionChanged({ source, connections }) {
    const { boundDatasetActions } = this.props;
    boundDatasetActions.setCanvasConnection(source.name, connections);
  }

  async handleEditConnection({ source, target }) {
    const { boundModalActions, dataset, isEditMode } = this.props;
    const form = dataset.modelRelations.find(
      relation => relation.modelLeft === source.name && relation.modelRight === target.name,
    );
    boundModalActions.setModalData('AddDatasetRelationshipModal', { disabled: isEditMode });
    await boundModalActions.showModal('AddDatasetRelationshipModal', form);
  }

  async handleDeleteConnection({ source, target }, disconnect) {
    const { boundDatasetActions, dataset } = this.props;
    await this.showDeleteAlert();
    const form = dataset.modelRelations.filter(
      relation => relation.modelLeft !== source.name || relation.modelRight !== target.name,
    );
    boundDatasetActions.updateRelationship(form);
    disconnect();
  }

  handleDestoryCanvas({ source }) {
    const { boundDatasetActions } = this.props;
    boundDatasetActions.deleteCanvasModel(source.name);
  }

  handleDrag(data, { x, y }) {
    const { boundDatasetActions } = this.props;
    const { name } = data;
    boundDatasetActions.setCanvasPosition(name, { x, y });
  }

  handleDragMove(data, maxPosition) {
    this.setContainerStyleThrottle(maxPosition);
  }

  async handleDeleteModel(modelName) {
    const { deleteModel } = this.props;
    const { store } = DraggableElement;
    const deleteEl = Object.values(store).find(el => el.data.name === modelName);
    await deleteModel(modelName);
    deleteEl.destroyCanvas();
  }

  renderConnectStyle({ source, target }, connect) {
    const { dataset } = this.props;
    const relations = dataset.modelRelations.find(relation => (
      relation.modelLeft === source.name && relation.modelRight === target.name
    ));
    if (relations) {
      const isErrorRelation = relations.relation.some(relation => (
        relation.leftError || relation.rightError
      ));
      if (isErrorRelation) {
        connect.setPaintStyle({ stroke: '#e73371' });
      }
    }
  }

  renderConnectOverlay({ source, target }, { disconnect, onHover, onBlur }) {
    const { dataset, intl } = this.props;
    const relations = dataset.modelRelations.find(relation => (
      relation.modelLeft === source.name && relation.modelRight === target.name
    ));
    if (relations) {
      const isErrorRelation = relations.relation.some(relation => (
        relation.leftError || relation.rightError
      ));

      return (
        <Fragment>
          <div className={classnames('link-button', isErrorRelation && 'is-error')} onMouseEnter={onHover} onMouseLeave={onBlur}>
            <Tooltip
              appendToBody
              popperClass="draggable-element-link-action-tip"
              effect="dark"
              placement="top"
              content={intl.formatMessage(strings.EDIT)}
            >
              <i
                className="icon-superset-link"
                role="button"
                tabIndex="0"
                aria-label="edit"
                onClick={() => this.handleEditConnection({ source, target }, disconnect)}
                onKeyUp={() => this.handleEditConnection({ source, target }, disconnect)}
              />
            </Tooltip>
            <Tooltip
              appendToBody
              popperClass="draggable-element-link-action-tip"
              effect="dark"
              placement="top"
              content={intl.formatMessage(strings.DELETE)}
            >
              <i
                className="icon-superset-table_delete"
                role="button"
                tabIndex="0"
                aria-label="delete"
                onClick={() => this.handleDeleteConnection({ source, target }, disconnect)}
                onKeyUp={() => this.handleDeleteConnection({ source, target }, disconnect)}
              />
            </Tooltip>
          </div>
        </Fragment>
      );
    }
    return null;
  }

  render() {
    const { connectDropTarget, content, intl, isEditMode } = this.props;
    const { instance } = this.state;
    const { $operationArea } = this;
    if (content.length) {
      return connectDropTarget(
        <div className={this.containerClass}>
          {/* ref/id prop won't work in sql, so use document.querySelector */}
          <div ref={$operationArea} id="$operationArea" className="operation-area">
            <div className="drop-model-canvas" style={this.containerStyle}>
              {instance && content.map(item => (
                <DraggableElement
                  isConnectable={!item.error}
                  key={item.name}
                  jsPlumb={instance}
                  data={item}
                  dataKey="name"
                  isEditMode={isEditMode}
                  x={this.getModelCanvas(item).x || 0}
                  y={this.getModelCanvas(item).y || 0}
                  top={this.getModelCanvas(item).top || []}
                  right={this.getModelCanvas(item).right || []}
                  bottom={this.getModelCanvas(item).bottom || []}
                  left={this.getModelCanvas(item).left || []}
                  onDrag={this.handleDrag}
                  onDragMove={this.handleDragMove}
                  onBeforeConnect={this.handleBeforeConnect}
                  onConnectionChanged={this.handleConnectionChanged}
                  onDestory={this.handleDestoryCanvas}
                  renderConnectOverlay={this.renderConnectOverlay}
                  renderConnectStyle={this.renderConnectStyle}
                >
                  <RelationMapModel
                    canDelete
                    {...item}
                    {...this.props}
                    deleteModel={this.handleDeleteModel}
                  />
                </DraggableElement>
              ))}
            </div>
          </div>
        </div>,
      );
    }
    return connectDropTarget(
      <div className={this.containerClass}>
        <span className="tip">
          <i className="icon-superset-model" />
          <br />
          {intl.formatMessage(strings.DRAG_TO_ADD_MODEL)}
        </span>
      </div>,
    );
  }
}

const areaTarget = {
  drop(props, monitor) {
    const { boundDatasetActions, onDrop } = props;
    const { wrapperSelector } = DropModelContainer;

    if (!monitor.didDrop()) {
      const model = monitor.getItem();
      const containerEl = document.querySelector(wrapperSelector);

      const { x: dropX, y: dropY } = monitor.getClientOffset();
      const { left: containerX, top: containerY } = containerEl.getBoundingClientRect();
      const modelX = dropX < containerX + 30 ? 0 : dropX - containerX - 127;
      const modelY = dropY < containerY + 30 ? 0 : dropY - containerY - 53;

      boundDatasetActions.setCanvasPosition(model.name, { x: modelX, y: modelY });

      onDrop(model);
    }
  },
};

export default DropTarget(
  ItemTypes.DragableItem,
  areaTarget,
  (connect, monitor) => ({
    connectDropTarget: connect.dropTarget(),
    isOver: monitor.isOver(),
    canDrop: monitor.canDrop(),
  }),
)(DropModelContainer);
