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
import { configs } from '../constants';

const { formatTypes, negativeTypes, currencyTypes } = configs;

function getNegativeType(negative) {
  if (typeof negative === 'string' && /^\(.*\)$/.test(negative)) {
    return negativeTypes.PARENTHESES;
  }
  return negativeTypes.NORMAL;
}

export function getDecimalFormat(decimalCount) {
  let format = '';

  if (decimalCount > 0) {
    format = '.';
    for (let i = 0; i < decimalCount; i += 1) {
      format += '0';
    }
  }

  return format;
}

export function getSampleDecimalFormat(decimalCount) {
  let format = '';

  if (decimalCount > 0) {
    format = '.';
    for (let i = 0; i < decimalCount; i += 1) {
      if (i === 0) {
        format += '1';
      } else {
        format += '0';
      }
    }
  }

  return format;
}

function getNegativeFormat(negativeType, positive) {
  switch (negativeType) {
    case negativeTypes.PARENTHESES:
      return `(${positive})`;
    case negativeTypes.NORMAL:
    default:
      return `-${positive}`;
  }
}

export function parseFormatToSettings(format, formatType) {
  const [positive, negative] = format.split(';');

  switch (formatType) {
    case formatTypes.NUMBER: {
      const [integer, decimal] = positive.split('.');
      return {
        decimalCount: typeof decimal === 'string' ? decimal.length : 0,
        isThousandSeparate: typeof integer === 'string' ? integer.includes(',') : false,
        negativeType: getNegativeType(negative),
      };
    }
    case formatTypes.CURRENCY: {
      const [integer, decimal] = positive.split('.');
      const currency = Object.values(currencyTypes).includes(integer[0])
        ? integer[0]
        : currencyTypes.DOLLAR;
      return {
        decimalCount: typeof decimal === 'string' ? decimal.length : 0,
        isThousandSeparate: true,
        negativeType: getNegativeType(negative),
        currency,
      };
    }
    case formatTypes.PERCENT: {
      const [, decimal] = positive.replace('%', '').split('.');
      return {
        decimalCount: typeof decimal === 'string' ? decimal.length : 0,
        isThousandSeparate: false,
        negativeType: getNegativeType(negative),
      };
    }
    case formatTypes.CUSTOM: {
      const [integer, decimal] = positive.replace('%', '').split('.');
      const currency = Object.values(currencyTypes).includes(integer[0])
        ? integer[0]
        : currencyTypes.DOLLAR;

      return {
        decimalCount: typeof decimal === 'string' ? decimal.length : 0,
        isThousandSeparate: typeof integer === 'string' ? integer.includes(',') : false,
        negativeType: getNegativeType(negative),
        currency,
        customFormat: format,
      };
    }
    case formatTypes.NO_FORMAT:
    default:
      return {};
  }
}

export function parseSettingsToFormat(settings) {
  const {
    formatType, decimalCount, isThousandSeparate, negativeType, currency, customFormat,
  } = settings;

  switch (formatType) {
    case formatTypes.NUMBER: {
      const integer = isThousandSeparate ? '#,###' : '####';
      const decimal = getDecimalFormat(decimalCount);
      const positive = `${integer}${decimal}`;
      const negative = getNegativeFormat(negativeType, positive);
      return `${positive};${negative}`;
    }
    case formatTypes.CURRENCY: {
      // const integer = isThousandSeparate ? '#,###' : '####';
      const integer = '#,###';
      const decimal = getDecimalFormat(decimalCount);
      const positive = `${currency}${integer}${decimal}`;
      const negative = getNegativeFormat(negativeType, positive);
      return `${positive};${negative}`;
    }
    case formatTypes.PERCENT: {
      // const integer = isThousandSeparate ? '#,###' : '0';
      // const negative = getNegativeFormat(negativeType, positive);
      const integer = '0';
      const decimal = getDecimalFormat(decimalCount);
      const positive = `${integer}${decimal}%`;
      const negative = getNegativeFormat(negativeTypes.NORMAL, positive);
      return `${positive};${negative}`;
    }
    case formatTypes.CUSTOM: {
      return customFormat;
    }
    case formatTypes.NO_FORMAT:
    default:
      return 'regular';
  }
}
