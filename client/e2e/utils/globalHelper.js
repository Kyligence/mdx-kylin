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
const {
  BROWSER,
  VERSION,
} = process.env;

exports.getParams = async function getParams(driver) {
  const capabilities = await driver.getCapabilities();

  const browser = capabilities.get('browserName') || BROWSER;

  const versionString = capabilities.get('version') || VERSION || '99';
  const version = +(versionString).split('.')[0];

  return { browser, version };
};
