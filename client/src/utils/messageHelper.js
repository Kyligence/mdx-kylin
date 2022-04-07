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
