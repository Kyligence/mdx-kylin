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
export const nameRegExp = /^\w+$/;
export const nameRegExpWithSpace = /^[A-Za-z0-9_\s]+$/;
export const nameRegExpWithChinese = /^[\u4e00-\u9fa5A-Za-z0-9_]+$/;
export const nameRegExpWithChineseAndSpace = /^[\u4e00-\u9fa5A-Za-z0-9_\s]+$/;

export const nameRegExpInDataset = {
  defaultRule: /^[\u4e00-\u9fa5A-Za-z0-9_\s\-%()（）?？]+$/,
  namedSet: /^[\u4e00-\u9fa5A-Za-z0-9_\s\-()%]+$/,
  translate: /^[\u4e00-\u9fa5A-Za-z0-9_\s\-%()（）?？]*$/,
};
export const nameRegExpInDatasetName = /^[\u4e00-\u9fa5A-Za-z0-9_\s]+$/;
export const subfolderRegExpInDataset = /^[\\\u4e00-\u9fa5A-Za-z0-9\s]*$/;

export const responseRegx = /^\[[A-Z0-9_-]+\]/;
