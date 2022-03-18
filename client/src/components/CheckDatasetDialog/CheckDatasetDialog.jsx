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
/* eslint-disable */
import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import { Dialog, Tabs, Button, Alert } from 'kyligence-ui-react';
import { strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { withRouter as WithRouter } from 'react-router'
import { getDefaultState, validator } from './handler';
import './index.less';

export default
@Connect({
  namespace: 'modal/CheckDatasetDialog',
  defaultState: getDefaultState(),
    mapState: {
      isShow: state => state.isShow,
      nameSets: state => state.name_set,
      CMArr: state => state.CM,
      dimension :state => state.dimension,
      semanticUrl: state => state.semanticUrl,
    }
})
@InjectIntl()
@WithRouter
class CheckDatasetDialog extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);
    this.handleHideModal = this.handleHideModal.bind(this);
    this.handleBack = this.handleBack.bind(this);
  }
  UNSAFE_componentWillMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('CheckDatasetDialog', getDefaultState());
  }

  get tabsData() {
    const { intl, nameSets, CMArr, dimension } = this.props;
    return [
      {
        name: 'CM',
        label: <span>{intl.formatMessage(strings.CALCULATED_MEASURE)} ({CMArr.length})</span>,
        errors: CMArr || [],
      },
      {
        name: 'nameSet',
        label: <span>{intl.formatMessage(strings.NAMEDSET)} ({nameSets.length})</span>,
        errors: nameSets || [],
      },
      {
        name: 'dimention',
        label: <span>{intl.formatMessage(strings.DIMENSION)} ({dimension.length})</span>,
        errors: dimension || [],
      }
    ]
  }

  handleHideModal() {
    const { boundModalActions } = this.props;
    boundModalActions.hideModal('CheckDatasetDialog');
    let obj = {
      CM: [],
      name_set: [],
      dimention: [],
      semanticUrl: [],
    }
    boundModalActions.setModalData('CheckDatasetDialog', obj);
  }
  handleBack() {
    this.handleHideModal()
    const { history, semanticUrl } = this.props;
    history.push(semanticUrl)
  }

  render() {
    const { isShow, intl, nameSets, CMArr, dimension } = this.props;
    const { tabsData } = this;
    return (
      <Dialog
        visible={isShow}
        title={intl.formatMessage(strings.NOTICE)}
        size="tiny"
        onCancel={this.handleHideModal}
        className="check-dataset-dailog"
      >
        <Dialog.Body>
        <Alert title={intl.formatMessage(strings.ERROR_DATASET_CHECK_MSG)} type="error" showIcon={true} closable={false}></Alert>
          <Tabs activeName='CM'>
          {tabsData.map(tab => (
            <Tabs.Pane key={tab.label} label={tab.label} name={tab.name}>
                <ul className="error-ul">
                  {tab.errors.length > 0 ? tab.errors.map(error => (
                    <li key={error}>{error}</li>
                  )) : <p className="no-data">{intl.formatMessage(strings.NO_DATA)}</p>}
                </ul>
            </Tabs.Pane>
          ))}
          </Tabs>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
        <Button  onClick={this.handleBack} type="primary">
            {intl.formatMessage(strings.MODIFY_EXPRESSION)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
