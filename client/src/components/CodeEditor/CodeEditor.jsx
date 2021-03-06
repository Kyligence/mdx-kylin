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
import classnames from 'classnames';
import AceEditor from 'react-ace';

import 'ace-builds/src-noconflict/ext-language_tools';
import './snippet-mdx';
import 'ace-builds/src-noconflict/snippets/text';
import 'ace-builds/src-noconflict/snippets/sql';
import './mode-mdx';
import 'ace-builds/src-noconflict/mode-text';
import 'ace-builds/src-noconflict/mode-sql';
import 'ace-builds/src-noconflict/theme-chrome';

import './index.less';

const EMPTY_ARRAY = [];

export default class CodeEditor extends PureComponent {
  static propTypes = {
    mode: PropTypes.string,
    theme: PropTypes.string,
    editorProps: PropTypes.object,
    setOptions: PropTypes.object,
    className: PropTypes.string,
    completions: PropTypes.array,
    buildinKeyWords: PropTypes.bool,
  };

  static defaultProps = {
    mode: 'text',
    theme: 'chrome',
    className: '',
    completions: EMPTY_ARRAY,
    buildinKeyWords: true,
    editorProps: {
      $blockScrolling: true,
    },
    setOptions: {
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true,
      enableSnippets: true,
      tabSize: 2,
    },
  };

  $editor = React.createRef();

  oriCompleters = [];

  acePopup = null;
  $heightLightLine = null;
  animationTimer = null;

  componentDidMount() {
    this.oriCompleters = this.$editor.current.editor.completers;
  }

  componentDidUpdate(prevProps) {
    this.updateKeyWords(prevProps);
    this.updateCompleterEvents();
  }

  get completions() {
    const { completions } = this.props;
    return [
      {
        identifierRegexps: [/[[\].a-zA-Z_0-9\u4e00-\u9fa5-]/],
        getCompletions(editor, session, pos, prefix, callback) {
          callback(null, completions);
        },
      },
    ];
  }

  updateKeyWords = prevProps => {
    const { completions: newCompletions, buildinKeyWords } = this.props;
    const { completions: oldCompletions } = prevProps;

    if (newCompletions !== oldCompletions) {
      this.$editor.current.editor.completers = buildinKeyWords ? [
        ...this.oriCompleters,
        ...this.completions,
      ] : this.completions;
    }
  };

  // #region ??????hover???????????????????????????????????????????????????
  getHeightLightLine = isHover => {
    const { acePopup } = this;

    const hoveredLineId = acePopup.getHoveredRow();
    const selectedLineId = acePopup.getRow();
    const $lines = acePopup.renderer.$textLayer.$lines.cells;

    let $heightLightId = null;
    // ???hover????????????hoveredId
    if (isHover === true) {
      $heightLightId = hoveredLineId !== -1 ? hoveredLineId : selectedLineId;
    }
    // ???select????????????selectedId
    if (isHover === false) {
      $heightLightId = selectedLineId !== -1 ? selectedLineId : hoveredLineId;
    }

    const $line = $lines.find(line => line.row === $heightLightId);
    return $line ? $line.element : null;
  };

  updateCompleterEvents = () => setTimeout(() => {
    const { $editor: { current: $editor } } = this;

    // ???popup?????????????????????
    if ($editor && $editor.editor && $editor.editor.completer && $editor.editor.completer.popup) {
      this.acePopup = $editor.editor.completer.popup;
      this.acePopup.off('changeHoverMarker', this.handleHoverLine);
      this.acePopup.on('changeHoverMarker', this.handleHoverLine);
      this.acePopup.off('select', this.handleSelectLine);
      this.acePopup.on('select', this.handleSelectLine);
    }
  });

  execHeightLightLineAnimation = (lastOffset = 0) => {
    const { $heightLightLine, acePopup } = this;
    const { container: $popup } = acePopup;
    const $lastSpans = $heightLightLine.querySelectorAll('span:last-child');
    const $lastSpan = $lastSpans[$lastSpans.length - 1];

    if ($lastSpan) {
      const paddingRight = 10;
      const lastSpanRight = $lastSpan.offsetLeft + $lastSpan.offsetWidth + paddingRight;
      const isTextOverflow = $popup.offsetWidth < lastSpanRight - lastOffset;
      if (isTextOverflow) {
        const newOffset = lastOffset + 10;

        // ???????????????????????????????????? ???????????? ??? ????????????
        if (!lastOffset) {
          $heightLightLine.style.transition = 'transform .5s linear';
          $heightLightLine.style.transform = 'translateX(0px)';
        }

        // ??????????????????????????????????????????
        setTimeout(() => {
          $heightLightLine.style.transform = `translateX(-${newOffset}px)`;
          this.animationTimer = setTimeout(() => this.execHeightLightLineAnimation(newOffset), 500);
        });
      }
    }
  };

  cleanupHeightLineAnimation = () => new Promise(resolve => {
    const { $heightLightLine, animationTimer } = this;

    if ($heightLightLine) {
      $heightLightLine.style.transition = 'transform 0s linear';
      setTimeout(() => {
        $heightLightLine.style.transform = '';
        clearTimeout(animationTimer);
        resolve();
      });
    } else {
      resolve();
    }
  });

  handleHoverLine = () => this.handleHeightLightLine(true);

  handleSelectLine = () => this.handleHeightLightLine(false);

  handleHeightLightLine = async isHover => {
    // ?????????????????? ???????????? ??? ?????? ??? ?????????
    await this.cleanupHeightLineAnimation();

    // ????????????ace.popup??? ??????????????????
    const $heightLightLine = this.getHeightLightLine(isHover);
    // ??? ?????????????????? ??????
    if ($heightLightLine) {
      // ??????????????? ??????????????????
      this.$heightLightLine = $heightLightLine;
      // ????????????
      this.execHeightLightLineAnimation();
    }
  };
  // #endregion

  render() {
    const { className, ...props } = this.props;
    const { $editor } = this;

    return (
      <AceEditor
        ref={$editor}
        className={classnames('code-editor', className)}
        {...props}
      />
    );
  }
}
