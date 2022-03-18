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
