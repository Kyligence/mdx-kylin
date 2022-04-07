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
