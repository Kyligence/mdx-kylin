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
import { DragSource } from 'react-dnd';
import { getEmptyImage } from 'react-dnd-html5-backend';
import RelationMapModel from './RelationMapModel';

const ItemTypes = {
  DragableItem: 'DragableModelItem',
};

const ItemSource = {
  beginDrag(props) {
    return props;
  },
};

class DragModelSource extends PureComponent {
  static propTypes = {
    name: PropTypes.string.isRequired,
    modify: PropTypes.number.isRequired,
    deleteModel: PropTypes.func,
    isEditMode: PropTypes.bool.isRequired,
    isDraggable: PropTypes.bool,
    isDragging: PropTypes.bool.isRequired,
    connectDragPreview: PropTypes.func.isRequired,
    connectDragSource: PropTypes.func.isRequired,
    message: PropTypes.string,
  };

  static defaultProps = {
    isDraggable: true,
    deleteModel: () => {},
    message: '',
  };

  componentDidMount() {
    const { connectDragPreview } = this.props;
    if (connectDragPreview) {
      // Use empty image as a drag preview so browsers don't draw it
      // and we can draw whatever we want on the custom drag layer instead.
      connectDragPreview(getEmptyImage(), {
        // IE fallback: specify that we'd rather screenshot the node
        // when it already knows it's being dragged so we can hide it with CSS.
        captureDraggingState: true,
      });
    }
  }

  render() {
    const { connectDragSource, isDragging, isDraggable, message } = this.props;
    return isDraggable ? connectDragSource(
      <div className="dragable-model-item">
        <RelationMapModel
          {...this.props}
          canDelete={false}
          isDragging={isDragging}
          message={message}
        />
      </div>,
    ) : (
      <div className="dragable-model-item disabled">
        <RelationMapModel
          {...this.props}
          canDelete={false}
          isDragging={isDragging}
          message={message}
        />
      </div>
    );
  }
}

export default DragSource(
  ItemTypes.DragableItem,
  ItemSource,
  (connect, monitor) => ({
    connectDragSource: connect.dragSource(),
    connectDragPreview: connect.dragPreview(),
    isDragging: monitor.isDragging(),
  }),
)(DragModelSource);
