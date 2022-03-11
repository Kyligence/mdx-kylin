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
import { Form, Input, Select } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { validator } from './handler';

export default
@Connect({
  mapState: {
    currentProject: state => state.system.currentProject,
    projectList: state => state.data.projectList,
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class DatasetInfo extends PureComponent {
  formRef = React.createRef();

  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    projectList: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    match: PropTypes.object.isRequired,
  };

  state = {
    form: {
      type: '',
      project: '',
      datasetName: '',
    },
  };

  componentDidMount() {
    const { dataset, currentProject } = this.props;
    this.setState({
      form: {
        type: dataset.type,
        project: dataset.project || currentProject.name,
        datasetName: dataset.datasetName,
      },
    });
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { dataset: nextDataset } = nextProps;
    const { dataset: prevDataset } = this.props;

    if (prevDataset.datasetName !== nextDataset.datasetName) {
      this.handleInput('datasetName', nextDataset.datasetName);
    }
  }

  get datasetId() {
    const { match } = this.props;
    return match.params.datasetId;
  }

  get rules() {
    return {
      type: [{ required: true, validator: validator.type(this.props), trigger: 'blur' }],
      project: [{ required: true, validator: validator.project(this.props), trigger: 'click' }],
      datasetName: [{ required: true, validator: validator.datasetName(this.props), trigger: 'blur' }],
    };
  }

  get projectOptions() {
    const { projectList } = this.props;
    return projectList.data.map(item => item.name);
  }

  handleInput(key, value) {
    const { form: oldForm } = this.state;
    const form = { ...oldForm, [key]: value };
    this.setState({ form });
  }

  handleSubmit() {
    const { boundDatasetActions } = this.props;
    const { form } = this.state;
    boundDatasetActions.setDatasetBasicInfo(form);
  }

  render() {
    const { intl } = this.props;
    const { form } = this.state;
    const { projectOptions, formRef, datasetId } = this;
    return (
      <div className="steps-form-wrapper">
        <Form labelPosition="top" ref={formRef} model={form} rules={this.rules} labelWidth="100">
          <div className="dataset-type">
            <span className="font-medium">{intl.formatMessage(strings.DATASET_TYPE)}</span>
            <span>{form.type}</span>
          </div>
          <Form.Item label={intl.formatMessage(strings.PROJECT_NAME)} prop="project">
            <Select
              placeholder={intl.formatMessage(strings.SELECT_PROJECT)}
              value={form.project}
              onChange={val => this.handleInput('project', val)}
              disabled
            >
              {projectOptions.map(projectName => (
                <Select.Option
                  key={projectName}
                  label={projectName}
                  value={projectName}
                />
              ))}
            </Select>
          </Form.Item>
          <Form.Item label={intl.formatMessage(strings.DATASET_NAME)} prop="datasetName">
            <Input
              disabled={!!datasetId}
              value={form.datasetName}
              placeholder={intl.formatMessage(strings.INPUT_DATASET_NAME)}
              onChange={val => this.handleInput('datasetName', val)}
              onBlur={() => this.handleSubmit()}
            />
          </Form.Item>
        </Form>
      </div>
    );
  }
}
