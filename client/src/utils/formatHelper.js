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
