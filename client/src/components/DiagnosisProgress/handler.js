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
import { createSelector } from 'reselect';
import { dataHelper } from '../../utils';

export const getPendingIdx = createSelector(
  state => state.results,
  results => results.findIndex(result => result.status === 'pending'),
);

export const getRunningIdx = createSelector(
  state => state.results,
  results => results.findIndex(result => result.status === 'running'),
);

export const getSuccessIdx = createSelector(
  state => state.results,
  results => dataHelper.findLastIndex(results, result => result.status === 'success'),
);

export const getErrorIdx = createSelector(
  state => state.results,
  results => results.findIndex(result => result.status === 'error'),
);

export const getDownloadUrl = createSelector(
  state => state.results,
  results => {
    const isAllSuccess = results.length && results.every(result => result.status === 'success');
    if (isAllSuccess) {
      return results[results.length - 1].detail;
    }
    return null;
  },
);
