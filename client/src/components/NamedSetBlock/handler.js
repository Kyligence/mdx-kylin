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
import { business, strings, configs } from '../../constants';

export const validator = {
  name({ intl }, isNamedSetDuplicate) {
    return (rule, inputValue = '', callback) => {
      const value = inputValue.trim();
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_NAME);
        callback(new Error(message));
        // 别名占用关键字报错
      } else if (configs.blackListKeyWords.includes(value.toLowerCase())) {
        const name = value;
        const nodeType = intl.formatMessage(strings.NAMEDSET).toLowerCase();
        const message = intl.formatMessage(strings.INVALID_NAME_IS_KEY_WORD, { name, nodeType });
        callback(new Error(message));
        // 别名重名报错
      } else if (isNamedSetDuplicate) {
        const params = { namedSetName: value };
        const message = intl.formatMessage(strings.NAMEDSET_DUPLICATE, params);
        callback(new Error(message));
        // 名称不符合规则报错
      } else if (!business.nameRegExpInDataset.namedSet.test(value)) {
        const message = intl.formatMessage(strings.INVALID_NAMED_SET_NAME_IN_DATASET);
        callback(new Error(message));
        // 超过最大长度报错
      } else if (value.length > configs.datasetMaxLength.namedSetName) {
        const maxLength = configs.datasetMaxLength.namedSetName;
        const params = { maxLength };
        const message = intl.formatMessage(strings.NAMEDSET_NAME_TOO_LONG, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  expression(that) {
    const isExpressionValid = async expression => {
      const { boundDatasetActions, dataset } = that.props;
      const data = await boundDatasetActions.validateNamedSetExpressionForMDX(expression, dataset);
      const { location, error } = data[0];
      that.handleInput('location', location && location !== 'Measures' ? location : 'Named Set');
      return { isValid: !error, error };
    };

    return async (rule, value, callback) => {
      const { intl } = that.props;
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_EXPRESSION);
        callback(new Error(message));
      } else if (value.length > configs.datasetMaxLength.namedSetExpression) {
        const maxLength = configs.datasetMaxLength.namedSetExpression;
        const params = { maxLength };
        const message = intl.formatMessage(strings.NAMEDSET_EXPRESSION_TOO_LONG, params);
        callback(new Error(message));
      } else {
        // 表达式校验不正确报错
        try {
          that.setState({ isValidating: true });

          const { isValid, error } = await isExpressionValid(value);

          that.setState({ isValidating: false });

          if (!isValid) {
            callback(new Error(error));
          } else {
            callback();
          }
        } catch (e) {
          that.setState({ isValidating: false });
          callback();
        }
      }
    };
  },
};
