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
import { bindActionCreators } from 'redux';
import * as userActions from './userActions';
import * as modalActions from './modalActions';
import * as globalActions from './globalActions';
import * as systemActions from './systemActions';
import * as datasetActions from './datasetActions';
import * as projectActions from './projectActions';
import * as toolkitActions from './toolkitActions';
import * as upgradeActions from './upgradeActions';
import * as diagnosisActions from './diagnosisActions';
import * as indicatorActions from './indicatorActions';
import * as userGroupActions from './userGroupActions';
import * as datasetRoleActions from './datasetRoleActions';
import * as queryHistoryActions from './queryHistoryActions';

export default function mapDispatchToProps(dispatch) {
  return {
    boundUserActions: bindActionCreators(userActions, dispatch),
    boundModalActions: bindActionCreators(modalActions, dispatch),
    boundGlobalActions: bindActionCreators(globalActions, dispatch),
    boundSystemActions: bindActionCreators(systemActions, dispatch),
    boundDatasetActions: bindActionCreators(datasetActions, dispatch),
    boundProjectActions: bindActionCreators(projectActions, dispatch),
    boundToolkitActions: bindActionCreators(toolkitActions, dispatch),
    boundUpgradeActions: bindActionCreators(upgradeActions, dispatch),
    boundDiagnosisActions: bindActionCreators(diagnosisActions, dispatch),
    boundIndicatorActions: bindActionCreators(indicatorActions, dispatch),
    boundUserGroupActions: bindActionCreators(userGroupActions, dispatch),
    boundDatasetRoleActions: bindActionCreators(datasetRoleActions, dispatch),
    boundQueryHistoryActions: bindActionCreators(queryHistoryActions, dispatch),
  };
}
