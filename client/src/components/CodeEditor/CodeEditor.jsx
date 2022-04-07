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

  // #region 鼠标hover在提示上，若提示超长出现走马灯动画
  getHeightLightLine = isHover => {
    const { acePopup } = this;

    const hoveredLineId = acePopup.getHoveredRow();
    const selectedLineId = acePopup.getRow();
    const $lines = acePopup.renderer.$textLayer.$lines.cells;

    let $heightLightId = null;
    // 当hover时，选取hoveredId
    if (isHover === true) {
      $heightLightId = hoveredLineId !== -1 ? hoveredLineId : selectedLineId;
    }
    // 当select时，选取selectedId
    if (isHover === false) {
      $heightLightId = selectedLineId !== -1 ? selectedLineId : hoveredLineId;
    }

    const $line = $lines.find(line => line.row === $heightLightId);
    return $line ? $line.element : null;
  };

  updateCompleterEvents = () => setTimeout(() => {
    const { $editor: { current: $editor } } = this;

    // 当popup存在，设置事件
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

        // 当第一次跑走马灯时，加入 初始位置 和 动画过渡
        if (!lastOffset) {
          $heightLightLine.style.transition = 'transform .5s linear';
          $heightLightLine.style.transform = 'translateX(0px)';
        }

        // 加完动画过渡后，设置动画偏移
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
    // 先清空上一个 自动提示 的 动画 和 定时器
    await this.cleanupHeightLineAnimation();

    // 获取最新ace.popup的 高亮自动提示
    const $heightLightLine = this.getHeightLightLine(isHover);
    // 当 高亮自动提示 存在
    if ($heightLightLine) {
      // 设置当前的 高亮自动提示
      this.$heightLightLine = $heightLightLine;
      // 执行动画
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
