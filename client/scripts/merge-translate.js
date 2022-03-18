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
const globSync = require('glob').sync;
const DIST_FOLDER = getDistFolder();
const I18N_FOLDER = path.resolve(DIST_FOLDER);
const MESSAGES_PATTERN = './public/messages/**/*.json';

ensureLocaleFolder();

const defaultMessages = getDefaultMessages();
const messageKeys = Object.keys(defaultMessages).sort();
const sortedMessages = sortDefaultMessages(defaultMessages, messageKeys);
const fileLocales = getInitLocalesFromFiles();
const fileLocaleKeys = Object.keys(fileLocales);

for (const localeKey of fileLocaleKeys) {
  const localeMessages = {};

  for (const messageKey of messageKeys) {
    const fileMessages = fileLocales[localeKey];

    const currentMessage = fileMessages[messageKey];
    const { defaultMessage } = defaultMessages[messageKey];

    const isMessageEmpty = !currentMessage;
    const isMessageSet = currentMessage && currentMessage !== defaultMessage;

    if (isMessageEmpty) {
      localeMessages[messageKey] = defaultMessage;
    } else {
      localeMessages[messageKey] = currentMessage;
    }
  }

  writeFileSync(localeKey, localeMessages);
}

function ensureLocaleFolder() {
  try {
    fs.accessSync(I18N_FOLDER, fs.constants.F_OK);
  } catch (e) {
    fs.mkdirSync(I18N_FOLDER);
  }
}

function getDefaultMessages() {
  const pathArray = globSync(MESSAGES_PATTERN);
  const fileArray = pathArray.map(path => fs.readFileSync(path, 'utf8'));
  const messageArray = fileArray.map(file => JSON.parse(file));
  return messageArray.reduce((collection, messageGroup) => {
    for (const message of messageGroup) {
      if (message.id in collection) {
        const id = message.id;
        const path = message.file;
        const line = message.start.line;
        throw new Error(`Duplicate message id: ${id} in ${path} at line ${line}`);
      } else {
        collection[message.id] = message;
      }
    }
    return collection;
  }, {});
}

function sortDefaultMessages(messages, messageKeys) {
  return messageKeys.reduce((acc, key) => {
    acc[key] = messages[key];
    return acc;
  }, {});
}

function getInitLocalesFromFiles() {
  const localeFiles = fs.readdirSync(I18N_FOLDER);
  const localeFileMap = localeFiles.reduce((fileMap, fileName) => {
    const locale = fileName.replace(/\.json$/, '');
    const filePath = path.resolve(I18N_FOLDER, fileName);
    const fileMessages = JSON.parse(fs.readFileSync(filePath, 'utf8') || '{}');

    fileMap[locale] = fileMessages;

    return fileMap;
  }, {});
  return localeFileMap;
}

function writeFileSync(locale, messages) {
  const localePath = path.resolve(I18N_FOLDER, `${locale}.json`);
  const localeContent = JSON.stringify(messages, null, 2);
  fs.writeFileSync(localePath, localeContent);
}

function getDistFolder() {
  const argIndex = process.argv.indexOf('--dist');

  return process.argv[argIndex + 1] || './public/locale';
}
