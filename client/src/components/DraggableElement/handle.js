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
