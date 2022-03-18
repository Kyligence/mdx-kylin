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

import './index.less';
import { configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import ConfigConnectionBlock from '../../components/ConfigConnectionBlock/ConfigConnectionBlock';

const { pollingDelayMs } = configs;

export default
@Connect({
  mapState: {
    pollings: state => state.global.pollings,
  },
})
@InjectIntl()
class Configurations extends PureComponent {
  static propTypes = {
    pollings: PropTypes.object.isRequired,
  };

  componentDidMount() {
    const { pollings } = this.props;
    pollings.configuration.setParams({ duringMs: pollingDelayMs.configurationInPage });
  }

  componentWillUnmount() {
    const { pollings } = this.props;
    pollings.configuration.setParams({ duringMs: pollingDelayMs.configuration });
  }

  render() {
    return (
      <div className="layout-wrapper configurations">
        <ConfigConnectionBlock />
      </div>
    );
  }
}
