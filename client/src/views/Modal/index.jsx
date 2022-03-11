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
import React from 'react';
import './index.less';
import DimensionUsageModal from '../../components/DimensionUsageModal/DimensionUsageModal';
import AboutModal from '../../components/AboutModal/AboutModal';
import CloneDatasetModal from '../../components/CloneDatasetModal/CloneDatasetModal';
import RenameDatasetModal from '../../components/RenameDatasetModal/RenameDatasetModal';
import GlobalMessageModal from '../../components/GlobalMessageModal/GlobalMessageModal';
import GlobalAdminMask from '../../components/GlobalAdminMask/GlobalAdminMask';
import GlobalMessageMask from '../../components/GlobalMessageMask/GlobalMessageMask';
import AddDatasetRelationshipModal from '../../components/AddDatasetRelationshipModal/AddDatasetRelationshipModal';
import DatasetAccessModal from '../../components/DatasetAccessModal/DatasetAccessModal';
import ConnectionUserModal from '../../components/ConnectionUserModal/ConnectionUserModal';
import CheckDatasetDialog from '../../components/CheckDatasetDialog/CheckDatasetDialog';
import ExpressionWizardModal from '../../components/ExpressionWizardModal/ExpressionWizardModal';
import SaveNotExistedModal from '../../components/SaveNotExistedModal/SaveNotExistedModal';
import HierarchyWeightModal from '../../components/HierarchyWeightModal/HierarchyWeightModal';
import FixDatasetModal from '../../components/FixDatasetModal/FixDatasetModal';
import ExportDatasetModal from '../../components/ExportDatasetModal/ExportDatasetModal';
import UploadDatasetModal from '../../components/UploadDatasetModal/UploadDatasetModal';
import DifferDatasetsModal from '../../components/DifferDatasetsModal/DifferDatasetsModal';
import FormatSettingModal from '../../components/FormatSettingModal/FormatSettingModal';

// 异步加载弹窗
export default function Modal() {
  return (
    <div className="modals">
      <CloneDatasetModal />
      <RenameDatasetModal />
      <AboutModal />
      <AddDatasetRelationshipModal />
      <HierarchyWeightModal />
      <FixDatasetModal />
      <ExportDatasetModal />
      <UploadDatasetModal />
      <DifferDatasetsModal />
      <FormatSettingModal />
      <DimensionUsageModal />
      <DatasetAccessModal />
      <ConnectionUserModal />
      <CheckDatasetDialog />
      <GlobalMessageModal />
      <ExpressionWizardModal />
      <SaveNotExistedModal />
      <GlobalAdminMask />
      <GlobalMessageMask />
    </div>
  );
}
