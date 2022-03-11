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
import React, { Fragment, PureComponent } from 'react';
import PropTypes from 'prop-types';
import { Form, Radio, DateRangePicker, Select, Button, Alert } from 'kyligence-ui-react';
import dayjs from 'dayjs';

import './index.less';
import { strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import DiagnosisProgressList from '../../components/DiagnosisProgressList/DiagnosisProgressList';
import { TODAY, LAST_3_DAYS, LAST_7_DAYS, LAST_1_MONTH, CUSTOMIZE, validator, dateTypes } from './handler';

export default
@Connect({
  mapState: {
    clusters: state => state.system.clusters,
  },
})
@InjectIntl()
class Diagnosis extends PureComponent {
  static propTypes = {
    boundDiagnosisActions: PropTypes.object.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    clusters: PropTypes.array.isRequired,
  };

  $form = React.createRef();

  state = {
    isInProgress: false,
    isSubmiting: false,
    dateType: null,
    form: {
      dateRange: [null, null],
      clusters: [],
    },
  };

  constructor(props) {
    super(props);
    this.handleRetry = this.handleRetry.bind(this);
    this.handleFinish = this.handleFinish.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleUnloadPage = this.handleUnloadPage.bind(this);
    this.handleChangeDateRange = this.handleChangeDateRange.bind(this);
    this.handleSelectClusters = this.handleSelectClusters.bind(this);
    this.handleSelectDateType = this.handleSelectDateType.bind(this);
  }

  async componentDidMount() {
    const { boundSystemActions } = this.props;
    await boundSystemActions.getClusters();
    this.handleSelectDateType(TODAY);
    window.addEventListener('beforeunload', this.handleUnloadPage);
  }

  componentWillUnmount() {
    window.removeEventListener('beforeunload', this.handleUnloadPage);
  }

  get clusterOptions() {
    const { clusters } = this.props;
    return clusters.map(({ host, port, status }) => ({
      key: `${host}:${port}`,
      label: `${host}:${port}`,
      value: `${host}:${port}`,
      disabled: status === 'inactive',
    }));
  }

  get rules() {
    return {
      dateRange: [{ required: true, validator: validator.dateRange(this.props), trigger: 'blur' }],
      clusters: [{ required: true, validator: validator.clusters(this.props), trigger: 'change' }],
    };
  }

  get downloadNotice() {
    const { intl } = this.props;
    return intl.formatMessage(strings.DIAGNOSIS_DOWNLOAD_NOTICE, {
      icon: <i key="icon" className="tip-icon icon-superset-download" />,
    });
  }

  toggleGenerateMode(isGenerateMode) {
    this.setState({ isGenerateMode, isInProgress: isGenerateMode });
  }

  toggleSubmiting(isSubmiting) {
    this.setState({ isSubmiting });
  }

  /* eslint-disable prefer-destructuring */
  handleSelectDateType(dateType) {
    const { form } = this.state;

    let startAt = null;
    let endAt = dayjs(Date.now()).endOf('day').valueOf();

    switch (dateType) {
      case TODAY: startAt = dayjs(endAt).add(-1, 'day').valueOf(); break;
      case LAST_3_DAYS: startAt = dayjs(endAt).add(-3, 'day').valueOf(); break;
      case LAST_7_DAYS: startAt = dayjs(endAt).add(-7, 'day').valueOf(); break;
      case LAST_1_MONTH: startAt = dayjs(endAt).add(-1, 'month').valueOf(); break;
      case CUSTOMIZE:
      default: { startAt = form.dateRange[0]; endAt = form.dateRange[1]; break; }
    }
    const dateRange = [startAt, endAt];
    this.setState({ form: { ...form, dateRange }, dateType });
  }

  handleUnloadPage(e) {
    const { isSubmiting, isInProgress } = this.state;
    if (isSubmiting || isInProgress) {
      const event = e || window.event;
      event.returnValue = 123;
    }
  }

  handleSelectClusters(values = []) {
    const clusters = values.map(value => ({
      host: value.split(':')[0],
      port: value.split(':')[1],
    }));
    this.handleInput('clusters', clusters);
  }

  handleChangeDateRange(value) {
    const startAt = dayjs(value[0]).valueOf();
    const endAt = dayjs(value[1]).valueOf();
    this.handleInput('dateRange', [startAt, endAt]);
  }

  handleInput(key, value) {
    const { form: oldForm } = this.state;
    const form = { ...oldForm, [key]: value };
    this.setState({ form });
  }

  handleRetry() {
    this.setState({ isInProgress: true });
  }

  handleFinish() {
    this.setState({ isInProgress: false });
  }

  handleSubmit() {
    const { boundDiagnosisActions } = this.props;
    const { form } = this.state;

    this.toggleSubmiting(true);
    this.toggleGenerateMode(false);
    this.$form.current.validate(async valid => {
      try {
        if (valid) {
          await boundDiagnosisActions.generatePackages(form);
          this.toggleGenerateMode(true);
        }
      } catch {} finally {
        this.toggleSubmiting(false);
      }
    });
  }

  render() {
    const { intl } = this.props;
    const { form, dateType, isGenerateMode, isSubmiting, isInProgress } = this.state;
    const { $form, clusterOptions, rules } = this;
    const selectedClusters = form.clusters.map(({ host, port }) => `${host}:${port}`);
    const isFormLocked = isSubmiting || isInProgress;

    return (
      <div className="diagnosis">
        <div className="title">
          {intl.formatMessage(strings.EXPORT_DIAGNOSIS_PACKAGE)}
        </div>
        <Form className="form" labelPosition="top" ref={$form} model={form} rules={rules}>
          <Form.Item className="date-type-field" label={intl.formatMessage(strings.SELECT_DATE_RANGE)}>
            <Radio.Group value={dateType} onChange={this.handleSelectDateType}>
              {dateTypes.map(item => (
                <Radio key={item} value={item} disabled={isFormLocked}>
                  {intl.formatMessage(strings[item])}
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>
          <div className="date-range">
            <Form.Item
              className="date-range-field"
              key={dateType}
              prop={dateType === 'CUSTOMIZE' ? 'dateRange' : null}
              label={intl.formatMessage(strings.DATE_RANGE)}
            >
              {dateType === 'CUSTOMIZE' ? (
                <DateRangePicker
                  isShowTime
                  value={[
                    dayjs(form.dateRange[0]).toDate(),
                    dayjs(form.dateRange[1]).toDate(),
                  ]}
                  disabled={isFormLocked}
                  placeholder={intl.formatMessage(strings.SELECT_DATE_RANGE)}
                  rangeSeparator={` ${intl.formatMessage(strings.TO)} `}
                  onChange={this.handleChangeDateRange}
                />
              ) : (
                <Fragment>
                  <span>{dayjs(form.dateRange[0]).add(1, 'millisecond').format('YYYY.MM.DD')}</span>
                  <span> - </span>
                  <span>{dayjs(form.dateRange[1]).format('YYYY.MM.DD')}</span>
                </Fragment>
              )}
            </Form.Item>
          </div>
          <Form.Item prop="clusters" label={intl.formatMessage(strings.SERVER)}>
            <Select
              multiple
              value={selectedClusters}
              onChange={this.handleSelectClusters}
              disabled={isFormLocked}
              noDataText={intl.formatMessage(strings.EMPTY_CLUSTERS)}
            >
              {clusterOptions.map(option => (
                <Select.Option
                  key={option.key}
                  label={option.label}
                  value={option.value}
                  disabled={option.disabled}
                />
              ))}
            </Select>
          </Form.Item>
          <div className="package-actions clearfix">
            <Button plain type="primary" onClick={this.handleSubmit} loading={isSubmiting} disabled={isInProgress}>
              {intl.formatMessage(strings.GENERATE_A_PACKAGE)}
            </Button>
          </div>
          {isGenerateMode && (
            <Fragment>
              <Alert
                showIcon
                type="notice"
                icon="icon-superset-infor"
                closable={false}
                title={this.downloadNotice}
              />
              <DiagnosisProgressList
                startAt={form.dateRange[0]}
                endAt={form.dateRange[0]}
                clusters={form.clusters}
                onRetry={this.handleRetry}
                onFinish={this.handleFinish}
              />
            </Fragment>
          )}
        </Form>
      </div>
    );
  }
}
