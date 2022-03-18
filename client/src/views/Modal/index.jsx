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
