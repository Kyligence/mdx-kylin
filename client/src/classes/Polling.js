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
