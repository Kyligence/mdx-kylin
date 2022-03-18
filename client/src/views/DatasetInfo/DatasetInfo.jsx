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
