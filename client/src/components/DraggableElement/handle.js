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
export const pointStyle = {
  hover: {
    stroke: '#0988de',
    fill: 'white',
    radius: 7,
    strokeWidth: 1,
  },
  linkHover: {
    stroke: '#0988de',
    fill: 'white',
    radius: 2,
    strokeWidth: 1,
  },
  connectedUnhover: {
    stroke: '#CCCCCC',
    fill: 'white',
    radius: 2,
    strokeWidth: 1,
  },
  blur: {
    stroke: 'transparent',
    fill: 'transparent',
    radius: 2,
    strokeWidth: 1,
  },
};

export const linkStyle = {
  hover: {
    strokeWidth: 1,
    stroke: '#0988de',
    joinstyle: 'round',
  },
  blur: {
    strokeWidth: 1,
    stroke: '#CCCCCC',
    joinstyle: 'round',
  },
};

export function getMaxPosition(max, el) {
  const newMax = { ...max };
  if (el.x + el.width > max.x + max.width) {
    newMax.x = el.x;
    newMax.width = el.width;
  }
  if (el.y + el.height > max.y + max.height) {
    newMax.y = el.y;
    newMax.height = el.height;
  }
  return newMax;
}
