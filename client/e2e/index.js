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
const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');

const e2ePath = path.resolve('./e2e.env');
const e2ePathLocal = path.resolve('./e2e.env.local');

if (fs.existsSync(e2ePath)) {
  dotenv.config({ path: e2ePath });
}

if (fs.existsSync(e2ePathLocal)) {
  dotenv.config({ path: e2ePathLocal });
}

require('./startup/startup.spec');
require('./login/login.spec');
require('./menu/menu.spec');
require('./datasetRole/datasetRole.spec');
require('./diagnosis/diagnosis.spec');
require('./configurations/configurations.spec');
require('./dataset/dataset.spec');
require('./queryHistory/queryHistory.spec');
