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
import strings from './strings';
import pageUrls from './pageUrls';

import * as SVGIcon from './icon';

export const sitemap = [
  {
    name: 'login',
    url: pageUrls.login,
    title: strings.KYLIN_MDX,
    isLoginPage: true,
  },
  {
    name: 'toolkit',
    url: pageUrls.toolkit,
    title: strings.KYLIN_MDX,
    icon: 'icon-superset-setting',
    category: strings.TOOLKIT,
    menuText: strings.TOOLKIT,
    isLoginPage: true,
  },
  {
    name: 'overview',
    url: pageUrls.overview,
    title: strings.KYLIN_MDX,
    icon: SVGIcon.NavStatistics,
    menuText: strings.OVERVIEW,
  },
  {
    name: null,
    url: pageUrls.dataset,
    children: [
      {
        name: 'listDataset',
        url: pageUrls.listDataset,
        title: strings.KYLIN_MDX,
        category: strings.DATASET,
        icon: SVGIcon.NavDatasource,
        menuText: strings.DATASET,
      },
      {
        name: 'datasetInfo',
        url: pageUrls.datasetInfo,
        paramUrl: pageUrls.datasetInfoWithId,
        title: strings.KYLIN_MDX,
        category: [strings.DATASET, '<%= datasetName %>'],
        belongTo: strings.DATASET,
        fallbackUrl: pageUrls.listDataset,
      },
      {
        name: 'datasetRelation',
        url: pageUrls.datasetRelation,
        paramUrl: pageUrls.datasetRelationWithId,
        title: strings.KYLIN_MDX,
        category: [strings.DATASET, '<%= datasetName %>'],
        belongTo: strings.DATASET,
        fallbackUrl: pageUrls.listDataset,
      },
      {
        name: 'datasetSemantic',
        url: pageUrls.datasetSemantic,
        paramUrl: pageUrls.datasetSemanticWithId,
        title: strings.KYLIN_MDX,
        category: [strings.DATASET, '<%= datasetName %>'],
        belongTo: strings.DATASET,
        fallbackUrl: pageUrls.listDataset,
      },
      {
        name: 'datasetTranslation',
        url: pageUrls.datasetTranslation,
        paramUrl: pageUrls.datasetTranslationWithId,
        title: strings.KYLIN_MDX,
        category: [strings.DATASET, '<%= datasetName %>'],
        belongTo: strings.DATASET,
        fallbackUrl: pageUrls.listDataset,
      },
      {
        name: 'datasetUsages',
        url: pageUrls.datasetUsages,
        paramUrl: pageUrls.datasetUsagesWithId,
        title: strings.KYLIN_MDX,
        category: [strings.DATASET, '<%= datasetName %>'],
        belongTo: strings.DATASET,
        fallbackUrl: pageUrls.listDataset,
      },
      {
        name: 'datasetAccess',
        url: pageUrls.datasetAccess,
        paramUrl: pageUrls.datasetAccessWithId,
        title: strings.KYLIN_MDX,
        category: [strings.ACCESS_CONTROL, '<%= datasetName %>'],
        belongTo: strings.DATASET,
        fallbackUrl: pageUrls.listDataset,
      },
    ],
  },
  {
    name: 'queryHistory',
    url: pageUrls.queryHistory,
    title: strings.KYLIN_MDX,
    icon: SVGIcon.NavQueryHistory,
    category: strings.QUERY_HISTORY,
    menuText: strings.QUERY_HISTORY,
  },
  {
    name: 'datasetRole',
    url: pageUrls.listDatasetRole,
    title: strings.KYLIN_MDX,
    icon: SVGIcon.NavUserGroup,
    category: [strings.DATASET_ROLE, '<%=datasetRoleName%>'],
    menuText: strings.DATASET_ROLE,
  },
  {
    name: 'diagnosis',
    url: pageUrls.diagnosis,
    title: strings.KYLIN_MDX,
    icon: SVGIcon.NavDiagnose,
    category: strings.DIAGNOSIS,
    menuText: strings.DIAGNOSIS,
  },
  {
    name: 'configuration',
    url: pageUrls.configuration,
    title: strings.KYLIN_MDX,
    icon: SVGIcon.NavConfiguration,
    category: strings.CONFIGURATION,
    menuText: strings.CONFIGURATION,
  },
  {
    name: 'notFound',
    url: pageUrls.notFound,
    title: strings.KYLIN_MDX,
  },
];

export const ONE_SECOND = 1000;

export const ONE_MINUTE = ONE_SECOND * 60;

export const ONE_HOUR = ONE_MINUTE * 60;

export const ONE_DAY = ONE_HOUR * 24;

export const pageCount = {
  datasetRoleList: 10,
  auditList: 10,
  datasetList: 10,
  queryHistory: 10,
  indicator: 99999999,
  sqlHistory: 5,
  userList: 500,
  userGroupList: 500,
  projectList: 99999999,
};

export const batchDataCount = {
  user: 500,
  userGroup: 500,
  datasetRole: 500,
};

export const userMaxLength = {
  username: 256,
};

export const datasetRoleMaxLength = {
  description: 500,
};

export const datasetMaxLength = {
  datasetName: 100,
  tableAlias: 300,
  columnAlias: 300,
  columnDesc: 1000,
  columnDefaultMember: 1000,
  hierarchyName: 300,
  hierarchyDesc: 1000,
  measureAlias: 300,
  measureDesc: 1000,
  measureFormat: 150,
  cMeasureName: 300,
  folderName: 100,
  cMeasureDesc: 1000,
  namedSetName: 300,
  namedSetExpression: 5000,
  measureGroupName: 300,
  cMeasureExpression: 5000,
  translation: 100,
};

export const dimTableTypes = {
  REGULAR: 'regular',
  TIME: 'time',
};

export const adminSites = [
  'datasetRole',
  'diagnosis',
  'configuration',
];

export const dimColumnTypes = {
  REGULAR: 0,
  LEVEL_YEAR: 1,
  LEVEL_QUARTERS: 2,
  LEVEL_MONTHS: 3,
  LEVEL_WEEKS: 4,
  LEVEL_DAYS: 5,
};

export const columnTypeIconMaps = {
  boolean: 'icon-superset-type_boolean',
  varbinary: 'icon-superset-type_varbinary',
  date: 'icon-superset-type_date',
  float: 'icon-superset-type_float',
  decimal: 'icon-superset-type_decimal',
  double: 'icon-superset-type_double',
  int: 'icon-superset-type_int',
  integer: 'icon-superset-type_int',
  bigint: 'icon-superset-type_bigint',
  smallint: 'icon-superset-type_int',
  tinyint: 'icon-superset-type_int',
  time: 'icon-superset-type_time',
  timestamp: 'icon-superset-type_timestamp',
  varchar: 'icon-superset-type_varchar',
  map: 'icon-superset-type_map',
  list: 'icon-superset-type_list',
  interval_day_to_seconds: 'icon-superset-type_interval_day_to_seconds',
  interval_years_to_months: 'icon-superset-type_interval_years_to_months',
};

export const nodeTypes = {
  // dataset nodeTypes
  MODEL: 'model',
  TABLE: 'table',
  COMMON_TABLE: 'commonTable',
  RELATIONSHIP: 'relationship',
  MODEL_RELATION: 'modelRelation',
  COLUMN: 'column',
  DIM_COLUMN: 'dimColumn',
  HIERARCHY: 'hierarchy',
  HIERARCHY_LEVEL: 'hierarchyLevel',
  NAMEDSET: 'namedSet',
  NAMEDSET_ROOT: 'namedSetRoot',
  MEASURE: 'measure',
  MEASURE_ROOT: 'measureRoot',
  MEASURE_GROUP: 'measureGroup',
  CALCULATE_MEASURE: 'calculateMeasure',
  CALCULATE_MEASURE_ROOT: 'calculateMeasureRoot',
  TRANSLATION: 'translate',
  DIM_USAGE: 'usage',
  // indicator nodeTypes
  DEFAULT_FOLDER: 'defaultFolder',
  CUSTOM_FOLDER: 'customFolder',
  SUB_FOLDER: 'subfolder',
  INDICATOR: 'indicator',
};

export const nodeIconMaps = {
  [nodeTypes.MODEL]: 'icon-superset-model',
  [nodeTypes.TABLE]: 'icon-superset-sample',
  [nodeTypes.COMMON_TABLE]: 'icon-superset-common_dim',
  [nodeTypes.RELATIONSHIP]: 'icon-superset-link',
  [nodeTypes.COLUMN]: 'icon-superset-mdx_dimension',
  [nodeTypes.DIM_COLUMN]: 'icon-superset-mdx_dimension',
  [nodeTypes.HIERARCHY]: 'icon-superset-hierachy',
  [nodeTypes.NAMEDSET]: 'icon-superset-named_set',
  [nodeTypes.NAMEDSET_ROOT]: 'icon-superset-named_set_folder',
  [nodeTypes.MEASURE]: 'icon-superset-mdx_measure',
  [nodeTypes.MEASURE_ROOT]: 'icon-superset-measure_2',
  [nodeTypes.MEASURE_GROUP]: 'icon-superset-model',
  [nodeTypes.CALCULATE_MEASURE]: 'icon-superset-calculated_measure',
  [nodeTypes.CALCULATE_MEASURE_ROOT]: 'icon-superset-c_measure',
  [nodeTypes.DEFAULT_FOLDER]: 'icon-superset-desc',
  [nodeTypes.CUSTOM_FOLDER]: 'icon-superset-audit',
  [nodeTypes.SUB_FOLDER]: 'icon-superset-folder',
  [nodeTypes.INDICATOR]: 'icon-superset-insight',
};

export const tableRelationTypes = {
  JOINT: 0,
  NOT_JOINT: 1,
  MANY_TO_MANY: 2,
};

export const pollingDelayMs = {
  configuration: ONE_MINUTE * 10,
  configurationInPage: ONE_SECOND * 5,
};

export const projectAccessInKE = {
  GLOBAL_ADMIN: 'GLOBAL_ADMIN',
  ADMINISTRATION: 'ADMINISTRATION',
  MANAGEMENT: 'MANAGEMENT',
  OPERATION: 'OPERATION',
  READ: 'READ',
};

export const enabledProjectAccess = [
  projectAccessInKE.GLOBAL_ADMIN,
  projectAccessInKE.ADMINISTRATION,
];

export const inbuildDatasetRoles = [
  'Admin',
];

export const preventMenuFlagList = [
  'isDatasetEdit',
  'isSemanticEdit',
  'isDashboardEdit',
  'isExporterEdit',
];

export const systemManageCapabilities = [
  'addDatasetRole',
  'editDatasetRole',
  'viewDatasetRole',
  'deleteDatasetRole',
];

export const datasetVersionMaps = {
  'v0.1': 'v0.2',
  'v0.2': 'v0.3',
  'v0.3': 'v0.4',
  'v0.4': 'newest',
};

export const blackListKeyWords = ['all', 'level', 'members', 'measures'];

export const getDatasetVersion = () => Object
  .keys(datasetVersionMaps)
  .find(currVer => datasetVersionMaps[currVer] === 'newest');

export const disablePopperAutoFlip = {
  modifiers: {
    flip: { enabled: false },
    hide: { enabled: false },
    preventOverflow: { enabled: false },
  },
};

export const translatableNodeTypes = [
  nodeTypes.CALCULATE_MEASURE_ROOT,
  nodeTypes.NAMEDSET_ROOT,
];

export const queryTypes = {
  MDX: 'MDX',
  SQL: 'SQL',
};

export const entityTypes = {
  DATASET: 'dataset',
};

export const datasetCreateTypes = {
  IMPORT: 'import',
  CLONE: 'clone',
  RENAME: 'rename',
  NEW: 'new',
};

export const requestTypes = {
  CANCEL: 'CANCEL',
};

export const applicationNames = {
  Excel: 'Excel',
  Tableau: 'Tableau',
  SmartBI: 'Smartbi',
  PowerBI: 'Power BI',
  MicroStrategy: 'MicroStrategy',
  Olap4j: 'Olap4j',
  Unknown: strings.OTHER,
};

export const numericalTypes = [
  /^tinyint/, /^smallint/, /^int/, /^integer/, /^bigint/, /^float/, /^double/, /^decimal/, /^long/,
];

export const excelMimeTypes = [
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-excel.sheet.binary.macroEnabled.12',
  'application/vnd.ms-excel',
  'application/vnd.ms-excel.sheet.macroEnabled.12',
];

export const deleteTypes = {
  DIMENSION_DELETED: 'DIMENSION_DELETED',
  MEASURE_DELETED: 'MEASURE_DELETED',
};

export const formatTypes = {
  NO_FORMAT: 'No Format',
  NUMBER: 'Number',
  CURRENCY: 'Currency',
  PERCENT: 'Percent',
  CUSTOM: 'Custom',
};

export const defaultFormats = {
  [formatTypes.NO_FORMAT]: 'regular',
  [formatTypes.NUMBER]: '####.00;-####.00',
  [formatTypes.CURRENCY]: '$####.00;-$####.00',
  [formatTypes.PERCENT]: '0.00%;-0.00%',
  [formatTypes.CUSTOM]: '0',
};

export const formatTranslations = {
  [formatTypes.NO_FORMAT]: strings.NO_FORMAT,
  [formatTypes.NUMBER]: strings.NUMBER_FORMAT,
  [formatTypes.CURRENCY]: strings.CURRENCY,
  [formatTypes.PERCENT]: strings.PERCENTAGE,
  [formatTypes.CUSTOM]: strings.CUSTOMIZE,
};

export const formatValue = 1234.1;
export const thousandValue = '1,234';

export const negativeTypes = {
  NORMAL: 'normal',
  PARENTHESES: 'parentheses',
};

export const currencyTypes = {
  DOLLAR: '$',
};
