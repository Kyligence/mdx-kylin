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
import React, { PureComponent, Suspense, Fragment } from 'react';
import PropTypes from 'prop-types';
import { IntlProvider } from 'react-intl';
import { Route, Switch } from 'react-router-dom';

import { Connect } from './store';
import { pageUrls } from './constants';
import { serviceHelper, routerHelper, messageHelper } from './utils';
import Modal from './views/Modal';
import SuspenseComponent from './components/SuspenseComponent/SuspenseComponent';
import Layout from './views/Layout/Layout';

const Overview = React.lazy(() => import(/* webpackChunkName: "Overview" */'./views/Overview/Overview'));
const Login = React.lazy(() => import(/* webpackChunkName: "Login" */'./views/Login/Login'));
const ListDataset = React.lazy(() => import(/* webpackChunkName: "ListDataset" */'./views/ListDataset/ListDataset'));
const ListDatasetRole = React.lazy(() => import(/* webpackChunkName: "ListDatasetRole" */'./views/ListDatasetRole/ListDatasetRole'));
const QueryHistory = React.lazy(() => import(/* webpackChunkName: "QueryHistory" */'./views/QueryHistory/QueryHistory'));
const Diagnosis = React.lazy(() => import(/* webpackChunkName: "Diagnosis" */'./views/Diagnosis/Diagnosis'));
const Configurations = React.lazy(() => import(/* webpackChunkName: "Configurations" */'./views/Configurations/Configurations'));
const Toolkit = React.lazy(() => import(/* webpackChunkName: "Toolkit" */'./views/Toolkit/Toolkit'));
const NotFound = React.lazy(() => import(/* webpackChunkName: "NotFound" */'./views/NotFound/NotFound'));

const DatasetLayout = React.lazy(() => import(/* webpackChunkName: "DatasetLayout" */'./views/DatasetLayout/DatasetLayout'));
const DatasetInfo = React.lazy(() => import(/* webpackChunkName: "DatasetInfo" */'./views/DatasetInfo/DatasetInfo'));
const DatasetRelation = React.lazy(() => import(/* webpackChunkName: "DatasetRelation" */'./views/DatasetRelation/DatasetRelation'));
const DatasetSemantic = React.lazy(() => import(/* webpackChunkName: "DatasetSemantic" */'./views/DatasetSemantic/DatasetSemantic'));
const DatasetTranslation = React.lazy(() => import(/* webpackChunkName: "DatasetTranslation" */'./views/DatasetTranslation/DatasetTranslation'));
const DatasetUsages = React.lazy(() => import(/* webpackChunkName: "DatasetUsages" */'./views/DatasetUsages/DatasetUsages'));
const DatasetAccess = React.lazy(() => import(/* webpackChunkName: "DatasetAccess" */'./views/DatasetAccess/DatasetAccess'));

export default
@Connect({
  mapReselect: {
    menus: reselect => reselect.system.menus,
    currentSite: reselect => reselect.system.currentSite,
    availableSites: reselect => reselect.system.availableSites,
  },
  mapState: {
    language: state => state.system.language,
    currentUser: state => state.system.currentUser,
    currentProject: state => state.system.currentProject,
    configurations: state => state.system.configurations,
    aadSettings: state => state.system.aadSettings,
    projectList: state => state.data.projectList,
    pollings: state => state.global.pollings,
  },
})
class App extends PureComponent {
  static hasConfirmPopper() {
    return !!document.querySelector('.el-message-box__wrapper');
  }

  static propTypes = {
    boundSystemActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
    boundProjectActions: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
    language: PropTypes.shape({
      locale: PropTypes.string,
      messages: PropTypes.object,
    }).isRequired,
    currentUser: PropTypes.object.isRequired,
    currentSite: PropTypes.object,
    currentProject: PropTypes.object.isRequired,
    configurations: PropTypes.object.isRequired,
    menus: PropTypes.array.isRequired,
    availableSites: PropTypes.array.isRequired,
    pollings: PropTypes.object.isRequired,
    projectList: PropTypes.object.isRequired,
    aadSettings: PropTypes.object.isRequired,
  };

  static defaultProps = {
    currentSite: null,
  };

  state = {
    isSystemInited: false,
    isDataInited: false,
  };

  constructor(props) {
    super(props);
    this.handleCheckConnectionUser = this.handleCheckConnectionUser.bind(this);
    this.handlePollingConfiguration = this.handlePollingConfiguration.bind(this);
    this.handlePageVisibility = this.handlePageVisibility.bind(this);
  }

  async componentDidMount() {
    await this.perpareSystem();
    await this.perpareData();
    document.addEventListener('visibilitychange', this.handlePageVisibility);
  }

  async componentDidUpdate(prevProps, prevStates) {
    const { props: nextProps, state: nextStates, redirectToLogin } = this;

    const isLogin = !prevProps.currentUser.username && nextProps.currentUser.username;
    const isLogout = prevProps.currentUser.username && !nextProps.currentUser.username;

    if (nextStates.isSystemInited) {
      if (isLogin || isLogout) {
        await this.clearupData();
      }
      if (isLogin) {
        await this.perpareData();
      }
    }

    routerHelper.routerHook(
      { prevProps, prevStates },
      { nextProps, nextStates, redirectToLogin },
    );
  }

  async componentWillUnmount() {
    document.removeEventListener('visibilitychange', this.handlePageVisibility);
  }

  redirectToLogin = () => {
    const { aadSettings, history } = this.props;
    const { isAADMode } = aadSettings;

    if (isAADMode) {
      window.location.href = aadSettings.loginUrl;
    } else {
      history.push(pageUrls.login);
    }
  };

  async perpareSystem() {
    const { boundModalActions, boundSystemActions, language } = this.props;

    serviceHelper.initService({
      boundModalActions,
      boundSystemActions,
    });

    this.initGlobalPollings();

    await Promise.all([
      boundSystemActions.getLanguage(language.locale).catch(() => {}),
      boundSystemActions.getCurrentUser().catch(() => this.redirectToLogin()),
      boundSystemActions.getLicense().catch(() => this.redirectToLogin()),
      boundSystemActions.getAADSettings(),
    ]);

    this.setState({ isSystemInited: true });
  }

  async perpareData() {
    const { boundProjectActions, currentUser, pollings, currentSite } = this.props;
    // 如果有用户登录，则获取project
    if (currentUser.username) {
      await Promise.all([
        pollings.configuration.start(),
        boundProjectActions.getProjects(),
      ]);

      this.startupAfterLogin();
      this.setState({ isDataInited: true });
    } else if (['toolkit'].includes(currentSite?.name)) {
      this.setState({ isDataInited: true });
    }
  }

  initGlobalPollings() {
    const { pollings } = this.props;
    pollings.configuration.addEventListener('polling', this.handlePollingConfiguration);
    pollings.configuration.addEventListener('afterEachPolling', this.handleCheckConnectionUser);
  }

  handlePageVisibility() {
    const { boundSystemActions } = this.props;
    if (document.visibilityState === 'visible') {
      boundSystemActions.getCurrentUser();
    }
  }

  startupAfterLogin() {
    const { boundSystemActions } = this.props;
    boundSystemActions.setDefaultProject();

    this.checkEmptyProject();
    this.checkConnectionUser();
  }

  clearupData() {
    const { boundProjectActions, boundSystemActions, pollings } = this.props;
    // 清除系统原先的project
    boundSystemActions.clearCurrentProject();
    boundProjectActions.clearProjects();
    pollings.configuration.stop();
    this.setState({ isDataInited: false });
  }

  gotoEditConnectionUser() {
    const { history, availableSites, boundGlobalActions } = this.props;

    boundGlobalActions.toggleFlag('isAutoPopConnectionUser', true);
    history.push(availableSites.find(site => site.name === 'configuration').url);
  }

  checkEmptyProject() {
    const { currentProject } = this.props;

    if (!currentProject.name) {
      messageHelper.showEmptyProjectAlert(this.props);
    }
  }

  handleCheckConnectionUser() {
    this.checkConnectionUser();
  }

  async checkConnectionUser() {
    const { configurations, availableSites, currentSite, currentProject } = this.props;
    const isAccessLoaded = currentProject.name;
    const isNoConnectionUser = !configurations['insight.kylin.username'];
    const isSyncTaskFailed = configurations['insight.kylin.status'] === 'inactive';
    const isNotInConfigurationSite = currentSite && currentSite.name !== 'configuration';
    const hasNoConfirmPopper = !App.hasConfirmPopper();
    const hasConfigurationAccess = availableSites.some(site => site.name === 'configuration');

    if (isAccessLoaded && isSyncTaskFailed) {
      if (isNoConnectionUser && hasConfigurationAccess) {
        this.gotoEditConnectionUser();
      } else if (!isNoConnectionUser && hasNoConfirmPopper && isNotInConfigurationSite) {
        const props = { ...this.props, hasConfigurationAccess };
        await messageHelper.showConnectionUserAlert(props);
        if (hasConfigurationAccess) {
          this.gotoEditConnectionUser();
        }
      }
    }
  }

  handlePollingConfiguration() {
    const { boundSystemActions, currentUser } = this.props;
    return currentUser.username ? boundSystemActions.getConfigurations() : null;
  }

  /* eslint-disable class-methods-use-this */
  renderDataset(props) {
    const { match } = props;

    return (
      <DatasetLayout {...props}>
        <Suspense fallback={<SuspenseComponent />}>
          <Route path={`${match.path}/basic-info`} component={DatasetInfo} />
          <Route path={`${match.path}/relationship`} component={DatasetRelation} />
          <Route path={`${match.path}/semantic`} component={DatasetSemantic} />
          <Route path={`${match.path}/translation`} component={DatasetTranslation} />
          <Route path={`${match.path}/dimension-usages`} component={DatasetUsages} />
        </Suspense>
      </DatasetLayout>
    );
  }

  render() {
    const { language, currentSite, currentProject } = this.props;
    const { isSystemInited, isDataInited } = this.state;

    return isSystemInited && currentSite && (
      <IntlProvider locale={language.locale} messages={language.messages}>
        <Layout {...this.props}>
          <Suspense fallback={<SuspenseComponent />}>
            <Route path={pageUrls.login} component={Login} />
            <Route path={pageUrls.toolkit} component={Toolkit} />
            {isDataInited ? (
              <Fragment key={currentProject.name}>
                <Switch>
                  <Route path={pageUrls.overview} component={Overview} />
                  <Route path={pageUrls.listDataset} component={ListDataset} />
                  <Route path={pageUrls.listDatasetRole} component={ListDatasetRole} />
                  <Route path={pageUrls.diagnosis} component={Diagnosis} />
                  <Route path={pageUrls.configuration} component={Configurations} />
                  <Route path={pageUrls.datasetNew} component={this.renderDataset} />
                  <Route path={pageUrls.datasetAccessWithId} component={DatasetAccess} />
                  <Route path={pageUrls.datasetWithId} component={this.renderDataset} />
                  <Route path={pageUrls.queryHistory} component={QueryHistory} />
                  <Route path={pageUrls.notFound} component={NotFound} />
                </Switch>
              </Fragment>
            ) : (
              <SuspenseComponent />
            )}
          </Suspense>
        </Layout>
        <Modal />
      </IntlProvider>
    );
  }
}
