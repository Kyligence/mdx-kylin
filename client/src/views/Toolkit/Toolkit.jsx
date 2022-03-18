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
import React from 'react';
import PropTypes from 'prop-types';
import { Loading } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import RepairSchema from './RepairSchema';

export default
@Connect()
@InjectIntl()
class Toolkit extends React.PureComponent {
  static propTypes = {
    boundToolkitActions: PropTypes.object.isRequired,
  };

  state = {
    loading: false,
    configurations: {},
  };

  async componentDidMount() {
    const { boundToolkitActions } = this.props;

    try {
      this.setState({ loading: true });

      const configurations = await boundToolkitActions.getTookitConfigurations();

      this.setState({ configurations });
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
    } finally {
      this.setState({ loading: false });
    }
  }

  render() {
    const { loading, configurations } = this.state;

    return (
      <div className="toolkit">
        <Loading loading={loading}>
          <RepairSchema configurations={configurations} />
        </Loading>
      </div>
    );
  }
}
