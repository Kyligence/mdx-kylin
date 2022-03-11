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
import ReactDOM from 'react-dom';

import './index.less';
import { domHelper } from '../../utils';
import { pointStyle, linkStyle, getMaxPosition } from './handle';

export default class DraggableElement extends PureComponent {
  static propTypes = {
    children: PropTypes.node.isRequired,

    x: PropTypes.number.isRequired,
    y: PropTypes.number.isRequired,

    jsPlumb: PropTypes.object.isRequired,
    isConnectable: PropTypes.bool,
    data: PropTypes.object.isRequired,
    dataKey: PropTypes.string.isRequired,

    onDrag: PropTypes.func.isRequired,
    onDragMove: PropTypes.func.isRequired,
    onDestory: PropTypes.func.isRequired,
    onBeforeConnect: PropTypes.func.isRequired,
    onClickConnection: PropTypes.func,
    onConnectionChanged: PropTypes.func.isRequired,

    renderConnectOverlay: PropTypes.func.isRequired,
    renderConnectStyle: PropTypes.func.isRequired,
  };

  static pointTypes = ['Top', 'Right', 'Bottom', 'Left'];
  // { jsPlumbId1: reactClass1, jsPlumbId2: reactClass2 }
  static store = {};
  // EndPoint instance var
  static currentEndPoint = null;
  static currentDraggableElement = null;
  static isDragging = false;
  static defaultProps = {
    isConnectable: true,
    onClickConnection: () => {},
  };

  state = {
    zIndex: 1,
  };

  $el = React.createRef();

  canvas = {
    id: null,
    x: 0,
    y: 0,
    width: 0,
    height: 0,
    endPoints: [],
    connections: {
      top: [],
      right: [],
      bottom: [],
      left: [],
    },
  };

  pointSettings = {
    endpoint: 'Dot',
    paintStyle: pointStyle.blur,
    isSource: true,
    isTarget: true,
    connector: [
      'Bezier',
      { curviness: 22 },
    ],
    connectorStyle: linkStyle.hover,
    dragOptions: {
      zIndex: 9,
    },
    maxConnections: -1,
  };

  constructor(props) {
    super(props);
    this.data = props.data;
    this.drawOverlay = this.drawOverlay.bind(this);
    this.isAlreadyConnect = this.isAlreadyConnect.bind(this);
    this.handleMouseDown = this.handleMouseDown.bind(this);
    this.handleDrag = this.handleDrag.bind(this);
    this.handleDragMove = this.handleDragMove.bind(this);
    this.handleConnected = this.handleConnected.bind(this);
    this.handleMouseOver = this.handleMouseOver.bind(this);
    this.handleBeforeConnect = this.handleBeforeConnect.bind(this);
    this.handleEndPointMouseover = this.handleEndPointMouseover.bind(this);
  }

  componentDidMount() {
    this.initCanvas();
    this.initCanvasData();
    this.initEvents();
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { x: oldX, y: oldY } = this.props;
    const { x: newX, y: newY } = nextProps;
    if (oldX !== newX || oldY !== newY) {
      this.drawPosition(nextProps);
    }
  }

  componentWillUnmount() {
    delete DraggableElement.store[this.canvas.id];
    window.removeEventListener('mousemove', this.handleMouseOver);
  }

  get draggableStyle() {
    const { zIndex } = this.state;
    return { zIndex };
  }

  setCanvasConnections({ sourceEndPoint, targetEndPoint }) {
    const { store } = DraggableElement;
    const { dataKey } = this.props;

    const sourceId = sourceEndPoint.elementId;
    const sourceDir = sourceEndPoint.anchor.type.toLowerCase();
    const sourceEl = store[sourceId];

    const targetId = targetEndPoint.elementId;
    const targetDir = targetEndPoint.anchor.type;
    const targetKey = store[targetId].data[dataKey];

    sourceEl.canvas.connections[sourceDir].push({ direction: targetDir, [dataKey]: targetKey });
  }

  setZIndex({ zIndex }) {
    this.setState({ zIndex });
    for (const endPoint of this.canvas.endPoints) {
      endPoint.canvas.style.zIndex = zIndex;
    }
  }

  handleHoverOverlay = ({ sourceId, targetId }) => {
    const { jsPlumb } = this.props;
    const options = targetId ? { source: sourceId, target: targetId } : { source: sourceId };

    for (const connection of jsPlumb.getConnections(options)) {
      connection.setPaintStyle(linkStyle.hover);
      for (const endPoint of connection.endpoints) {
        endPoint.setPaintStyle(pointStyle.linkHover);
      }
    }
  }

  handleBlurOverlay = ({ sourceId, targetId }) => {
    const { jsPlumb } = this.props;
    const options = targetId ? { source: sourceId, target: targetId } : { source: sourceId };

    for (const connection of jsPlumb.getConnections(options)) {
      connection.setPaintStyle(linkStyle.blur);
      for (const endPoint of connection.endpoints) {
        endPoint.setPaintStyle(pointStyle.blur);
      }
    }
  }

  deleteCanvasConnections({ sourceId, targetId }) {
    const { store } = DraggableElement;
    const { dataKey, onConnectionChanged } = this.props;

    const sourceEl = store[sourceId];
    const source = store[sourceId].data;

    const targetValue = targetId && store[targetId].data[dataKey];

    const { connections } = sourceEl.canvas;
    for (const dirKey of Object.keys(connections)) {
      connections[dirKey] = connections[dirKey]
        .filter(connection => (
          targetId ? connection[dataKey] !== targetValue : false
        ));
    }
    if (onConnectionChanged) {
      onConnectionChanged({ source, connections });
    }
  }

  drawPosition({ x, y }) {
    const { $el } = this;
    $el.current.style.left = `${x}px`;
    $el.current.style.top = `${y}px`;
  }

  drawConnection({ sourceId, sourceDir, targetId, targetDir }) {
    if (targetId) {
      const { jsPlumb, renderConnectStyle } = this.props;
      const { drawOverlay } = this;
      const { store, pointTypes } = DraggableElement;

      const source = store[sourceId].data;
      const sourcePointIdx = pointTypes.findIndex(pointType => pointType === sourceDir);
      const sourceEndPoint = store[sourceId].canvas.endPoints[sourcePointIdx];

      const target = store[targetId].data;
      const targetPointIdx = pointTypes.findIndex(pointType => pointType === targetDir);
      const targetEndPoint = store[targetId].canvas.endPoints[targetPointIdx];

      const sourceConnectable = store[sourceId].props.isConnectable;
      const targetConnectable = store[targetId].props.isConnectable;
      const isConnectable = sourceConnectable && targetConnectable;

      const overlays = isConnectable ? drawOverlay({ source, sourceId, target, targetId }) : [];
      const connectOptions = { source: sourceEndPoint, target: targetEndPoint, overlays };
      const connect = jsPlumb.connect(connectOptions);

      connect.setPaintStyle(linkStyle.blur);
      if (renderConnectStyle) {
        renderConnectStyle({ source, target }, connect);
      }
    }
  }

  drawConnections(props) {
    const { pointTypes } = DraggableElement;
    const pointKeys = pointTypes.map(item => item.toLowerCase());
    // pointKeys有top, right, bottom, left，和pointTypes不一样
    for (const pointKey of pointKeys) {
      const connections = props[pointKey];

      for (const connection of connections) {
        const { targetId, targetDir } = this.findTargetInfo(connection);
        const { sourceId, sourceDir } = this.findSourceInfo(pointKey);
        this.drawConnection({ sourceId, sourceDir, targetId, targetDir });
      }
    }
  }

  undrawConnections({ sourceId, targetId }) {
    const { jsPlumb } = this.props;
    const options = targetId ? { source: sourceId, target: targetId } : { source: sourceId };
    const connections = jsPlumb.getConnections(options);
    for (const connection of connections) {
      jsPlumb.deleteConnection(connection);
      this.deleteCanvasConnections({ sourceId, targetId });
    }
  }

  drawOverlay({ source, sourceId, target, targetId }) {
    const { onClickConnection, renderConnectOverlay } = this.props;

    const overlay = document.createElement('div');
    const disconnect = () => this.undrawConnections({ sourceId, targetId });
    const onHover = () => this.handleHoverOverlay({ sourceId, targetId });
    const onBlur = () => this.handleBlurOverlay({ sourceId, targetId });

    overlay.onclick = () => onClickConnection && onClickConnection({ source, target }, disconnect);

    const location = 0.5;
    const width = 10;
    const height = 10;

    const create = () => {
      const linkButton = (
        <div className="link-button">
          <i className="icon-superset-link" />
        </div>
      );

      const linkEl = renderConnectOverlay
        ? renderConnectOverlay({ source, target }, { disconnect, onHover, onBlur })
        : linkButton;

      ReactDOM.render(linkEl, overlay);

      return overlay;
    };

    const customOverLay = { create, location, width, height };

    return [['Custom', customOverLay]];
  }

  initCanvas() {
    const { jsPlumb } = this.props;
    const { $el } = this;
    const { pointTypes } = DraggableElement;
    const containment = jsPlumb.getContainer();
    const start = this.handleMouseDown;
    const drag = this.handleDragMove;
    const stop = this.handleDrag;
    this.drawPosition(this.props);
    jsPlumb.draggable($el.current, { start, drag, stop, containment });

    for (const pointType of pointTypes) {
      jsPlumb.addEndpoint($el.current, { anchor: pointType }, this.pointSettings);
    }

    setTimeout(() => {
      this.drawConnections(this.props);
      this.toggleHover(false);
    });
  }

  initCanvasData() {
    const { jsPlumb, x, y } = this.props;
    const { $el } = this;
    const { store } = DraggableElement;
    this.canvas.id = jsPlumb.getId($el.current);
    this.canvas.endPoints = jsPlumb.getEndpoints($el.current);
    this.canvas.x = x;
    this.canvas.y = y;
    store[this.canvas.id] = this;

    const elSize = $el.current.getBoundingClientRect();
    this.canvas.width = elSize.width;
    this.canvas.height = elSize.height;

    this.setZIndex(Object.keys(DraggableElement.store).length + 1);
  }

  initEvents() {
    const { jsPlumb } = this.props;
    const { endPoints } = this.canvas;
    jsPlumb.bind('beforeDrop', this.handleBeforeConnect);
    jsPlumb.bind('connection', this.handleConnected);
    for (const endPoint of endPoints) {
      endPoint.bind('mouseover', this.handleEndPointMouseover);
    }
    window.addEventListener('mousemove', this.handleMouseOver);
  }

  destroyCanvas() {
    const { jsPlumb, onDestory } = this.props;
    const { store } = DraggableElement;
    const { id, endPoints } = this.canvas;
    const source = this.data;

    for (const endPoint of endPoints) {
      jsPlumb.deleteEndpoint(endPoint);
    }
    if (onDestory) {
      onDestory({ source });
    }

    delete store[id];
    for (const nodeId of Object.keys(store)) {
      const { connections } = store[nodeId].canvas;
      for (const dirKey of Object.keys(connections)) {
        connections[dirKey] = connections[dirKey]
          .filter(connection => connection.name !== source.name);
      }
    }
  }

  isAlreadyConnect({ sourceId, targetId }) {
    const { jsPlumb } = this.props;
    const connections = jsPlumb.getConnections();

    return !!connections.find(connection => (
      (connection.sourceId === sourceId && connection.targetId === targetId) ||
      (connection.sourceId === targetId && connection.targetId === sourceId)
    ));
  }

  findTargetInfo(connection) {
    const { dataKey } = this.props;
    const { store } = DraggableElement;
    const { direction: targetDir, ...targetDataSearch } = connection;
    const searchValue = targetDataSearch[dataKey];
    const targetId = Object.keys(store).find(
      nodeId => store[nodeId].data[dataKey] === searchValue,
    );
    return { targetId, targetDir };
  }

  findSourceInfo(pointKey) {
    const { id: sourceId } = this.canvas;
    const { pointTypes } = DraggableElement;
    const sourceDir = pointTypes.find(
      item => item.toLowerCase() === pointKey,
    );
    return { sourceId, sourceDir };
  }

  handleMouseDown() {
    const { store } = DraggableElement;
    const draggableEls = Object.values(store);
    draggableEls.forEach((el, zIndex) => {
      el.setZIndex({ zIndex });
    });
    this.setZIndex({ zIndex: draggableEls.length });
  }

  handleDrag({ pos: [x, y] }) {
    const { onDrag, data } = this.props;

    this.canvas.x = x;
    this.canvas.y = y;

    if (onDrag) {
      onDrag(data, { x, y });
    }
  }

  handleDragMove({ drag: { size: [width, height] }, pos: [x, y], el: dragEl }) {
    const { onDragMove } = this.props;
    const { store } = DraggableElement;

    if (onDragMove) {
      const initPosition = { x: 0, y: 0, width: 0, height: 0 };
      const elIds = Object.keys(store).filter(elId => elId !== dragEl.id);

      let maxPosition = elIds.reduce((max, elId) => (
        getMaxPosition(max, store[elId].canvas)
      ), initPosition);

      maxPosition = getMaxPosition(maxPosition, { x, y, width, height });
      onDragMove({ id: dragEl.id, x, y, width, height }, maxPosition);
    }
  }

  /* eslint-disable class-methods-use-this */
  handleEndPointMouseover(endpoint) {
    if (!DraggableElement.isDragging) {
      DraggableElement.currentEndPoint = endpoint;
    }
  }

  handleConnected({ sourceId, targetId, sourceEndpoint, targetEndpoint }) {
    const { onConnectionChanged } = this.props;
    const { id, connections } = this.canvas;
    const { store } = DraggableElement;
    // connected事件会触发多次，因此判断只有source才能触发connected
    if (sourceId === id && onConnectionChanged) {
      const source = store[sourceId].data;
      const sourceEl = store[sourceId];
      const sourcePoints = sourceEl.canvas.endPoints;
      const sourceEndPoint = sourcePoints.find(point => point === sourceEndpoint);

      const targetEl = store[targetId];
      const targetPoints = targetEl.canvas.endPoints;
      const targetEndPoint = targetPoints.find(point => point === targetEndpoint);

      this.setCanvasConnections({ sourceEndPoint, targetEndPoint });

      onConnectionChanged({ source, connections });
    }
  }

  handleBeforeConnect({ sourceId, targetId, dropEndpoint }) {
    const { onBeforeConnect, jsPlumb } = this.props;
    const { drawOverlay, isAlreadyConnect } = this;
    const { currentEndPoint, store } = DraggableElement;
    const isSelfConnect = sourceId === targetId;

    if (onBeforeConnect) {
      if (!isAlreadyConnect({ sourceId, targetId }) && !isSelfConnect) {
        // 获取拖拽的 源id/目标id/连线参数
        const source = store[sourceId].data;
        const target = store[targetId].data;
        const sourceConnectable = store[sourceId].props.isConnectable;
        const targetConnectable = store[targetId].props.isConnectable;
        const isConnectable = sourceConnectable && targetConnectable;
        const overlays = isConnectable ? drawOverlay({ source, sourceId, target, targetId }) : [];
        const connectOptions = { source: currentEndPoint, target: dropEndpoint, overlays };
        const connector = () => {
          const connection = jsPlumb.connect(connectOptions);
          connection.setPaintStyle(linkStyle.blur);
        };

        onBeforeConnect({ source, target }, connector);
      }
      return false;
    }
    return true;
  }

  handleMouseOver(event) {
    const { jsPlumb } = this.props;
    const { $el } = this;
    const { endPoints } = this.canvas;
    const containerEl = jsPlumb.getContainer();
    const isConnecting = !!containerEl.querySelector('.jtk-dragging');

    let isHoverElement = false;
    let isHoverPoint = false;
    let isHoverOverlay = false;

    const path = domHelper.findParents(event.target);
    path.forEach(element => {
      if (!isHoverElement) {
        isHoverElement = element === $el.current;
      }
      if (!isHoverPoint) {
        isHoverPoint = endPoints.some(e => e.canvas === element);
      }
      if (!isHoverOverlay && typeof element.className === 'string' && element.className.includes('jtk-overlay')) {
        isHoverOverlay = true;
      }
    });

    if (isHoverElement || isHoverPoint) {
      this.toggleHover(true);
    } else if (isConnecting && !isHoverPoint) {
      this.toggleHover(true);
    } else if (!isHoverOverlay) {
      this.toggleHover(false);
    }

    DraggableElement.isDragging = isConnecting;
  }

  toggleHover(isHover) {
    const { isConnectable } = this.props;
    const { endPoints } = this.canvas;

    endPoints.forEach((endPoint, index) => {
      const { canvas } = endPoint;
      if (isHover && isConnectable) {
        endPoint.setStyle(pointStyle.hover);
        if (index === 0) {
          canvas.style.transform = 'translate(0, -5px)';
        } else if (index === 1) {
          canvas.style.transform = 'translate(5px, 0)';
        } else if (index === 2) {
          canvas.style.transform = 'translate(0, 5px)';
        } else if (index === 3) {
          canvas.style.transform = 'translate(-5px, 0)';
        }
      } else if (endPoint.connections.length) {
        endPoint.setStyle(pointStyle.connectedUnhover);
        canvas.style.transform = '';
      } else {
        endPoint.setStyle(pointStyle.blur);
        canvas.style.transform = '';
      }
    });
  }

  render() {
    const { children } = this.props;
    const { draggableStyle } = this;

    return (
      <div
        className="draggable-element"
        ref={this.$el}
        style={draggableStyle}
        onClick={this.handleMouseDown}
      >
        {children}
      </div>
    );
  }
}
