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
import React, { Fragment } from 'react';
import { Message, MessageBox } from 'kyligence-ui-react';

export function notifyInfo(message, options = {}) {
  const { showClose = false, duration = 3000 } = options;
  Message({ showClose, message, duration, type: 'info' });
}

export function notifySuccess(message, options = {}) {
  const { showClose = false, duration = 3000 } = options;
  Message({ showClose, message, duration, type: 'success' });
}

export function notifyFailed(message, options = {}) {
  const { showClose = false, duration = 3000 } = options;
  Message({ showClose, message, duration, type: 'error' });
}

export function notifyWarning(message, options = {}) {
  const { showClose = false, duration = 3000 } = options;
  Message({ showClose, message, duration, type: 'warning' });
}

/* eslint-disable react/jsx-filename-extension */
export function showConnectionUserAlert({ language, hasConfigurationAccess }) {
  const { messages } = language;

  const messageText = hasConfigurationAccess
    ? messages.RESART_SYNC_TASK_TIP_FOR_SYSTEM_ADMIN
    : messages.RESART_SYNC_TASK_TIP_FOR_PROJECT_ADMIN;

  const messageContent = (
    <Fragment>
      {messageText.split('\r\n').map(text => (
        <div key={text} className="message-text">{text}</div>
      ))}
    </Fragment>
  );

  const messageTitle = messages.NOTICE;
  const type = hasConfigurationAccess ? 'warning' : 'info';
  const confirmButtonText = messages.OK;
  const messageOptions = { type, confirmButtonText, showCancelButton: hasConfigurationAccess };

  return MessageBox.confirm(messageContent, messageTitle, messageOptions);
}

export function showEmptyProjectAlert({ language }) {
  const { messages } = language;

  const messageContent = messages.NO_PROJECT;
  const messageTitle = messages.NOTICE;
  const type = 'info';
  const confirmButtonText = messages.OK;
  const messageOptions = { type, confirmButtonText };

  return MessageBox.alert(messageContent, messageTitle, messageOptions).catch(() => {});
}
