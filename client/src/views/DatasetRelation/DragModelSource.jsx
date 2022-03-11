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
