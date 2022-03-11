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
import { Breadcrumb } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { dataHelper } from '../../utils';

export default
@Connect({
  mapState: {
    datasetName: state => state.workspace.dataset.datasetName,
    datasetRoleName: state => state.workspace.datasetRole.name,
    isNoBreadCrumb: state => state.global.isNoBreadCrumb,
  },
  mapReselect: {
    currentSitePath: reselect => reselect.system.currentSitePath,
  },
})
@InjectIntl()
class BreadCrumb extends PureComponent {
  static propTypes = {
    currentSitePath: PropTypes.array.isRequired,
    history: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isNoBreadCrumb: PropTypes.bool.isRequired,
  };

  get currentPaths() {
    const { currentSitePath, intl, ...params } = this.props;
    let currentPaths = [];

    for (const { category } of currentSitePath) {
      if (category) {
        const categoryArray = category instanceof Array ? category : [category];
        currentPaths = [
          ...currentPaths,
          ...categoryArray
            .map(categoryItem => dataHelper.translate(intl, categoryItem, params))
            .filter(item => item),
        ];
      }
    }

    return currentPaths;
  }

  handleClickBreadcrumb(url) {
    const { history } = this.props;
    if (url) {
      history.push(url);
    }
  }

  render() {
    const { isNoBreadCrumb } = this.props;
    const { currentPaths } = this;
    return currentPaths.length && !isNoBreadCrumb ? (
      <Breadcrumb className="bread-crumb" separator="/">
        {currentPaths.map(path => (
          <Breadcrumb.Item key={path}>
            {path}
          </Breadcrumb.Item>
        ))}
      </Breadcrumb>
    ) : null;
  }
}
