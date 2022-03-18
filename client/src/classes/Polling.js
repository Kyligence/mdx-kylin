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
export default class Polling {
  duringMs = 5000;
  timer = null;

  polling = [];

  beforeStartPolling = [];
  afterStartPolling = [];

  beforeStopPolling = [];
  afterStopPolling = [];

  beforeEachPolling = [];
  afterEachPolling = [];

  static checkEventValid(eventName) {
    return ['polling', 'beforeStartPolling', 'afterStartPolling', 'beforeStopPolling', 'afterStopPolling', 'beforeEachPolling', 'afterEachPolling'].includes(eventName);
  }

  constructor(params) {
    // public methods
    this.setParams = this.setParams.bind(this);
    this.stop = this.stop.bind(this);
    this.start = this.start.bind(this);
    this.addEventListener = this.addEventListener.bind(this);
    this.removeEventListener = this.removeEventListener.bind(this);
    // private methods
    this.handleEvent = this.handleEvent.bind(this);
    this.handlePolling = this.handlePolling.bind(this);

    this.setParams(params);
  }

  async setParams(params = {}) {
    const isPollingStarted = !!this.timer;
    // 如果轮询已经在启动状态，则停止轮询并重新启动
    if (isPollingStarted) {
      await this.stop();
    }

    for (const [key, value] of Object.entries(params)) {
      this[key] = value;
    }

    // 如果轮询已经在启动状态，则停止轮询并重新启动
    if (isPollingStarted) {
      await this.start();
    }
  }

  addEventListener(eventName, eventMethod) {
    if (Polling.checkEventValid(eventName)) {
      const isEventNotExisted = !this[eventName].includes(eventMethod);

      if (isEventNotExisted) {
        this[eventName].push(eventMethod);
      }
    }
  }

  removeEventListener(eventName, eventMethod) {
    if (Polling.checkEventValid(eventName)) {
      const isEventExisted = this[eventName].includes(eventMethod);

      if (isEventExisted) {
        this[eventName] = this[eventName].filter(method => method !== eventMethod);
      }
    }
  }

  async start() {
    const { handleEvent, handlePolling } = this;

    clearTimeout(this.timer);
    await handleEvent('beforeStartPolling');
    await handlePolling();
    await handleEvent('afterStartPolling');
  }

  async stop() {
    const { handleEvent } = this;

    await handleEvent('beforeStopPolling');
    clearTimeout(this.timer);
    this.timer = null;
    await handleEvent('afterStopPolling');
  }

  async handlePolling() {
    const { handleEvent, duringMs } = this;

    await handleEvent('beforeEachPolling');
    await handleEvent('polling');
    await handleEvent('afterEachPolling');

    this.timer = setTimeout(() => {
      this.handlePolling();
    }, duringMs);
  }

  async handleEvent(eventName) {
    const eventMethods = this[eventName];

    for (const eventMethod of eventMethods) {
      try {
        await eventMethod();
      } catch (e) {}
    }
  }
}
