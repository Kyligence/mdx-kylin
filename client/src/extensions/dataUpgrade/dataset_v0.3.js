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
export function upgradeDatasetV03(json) {
  return {
    ...json,
    models: json.models.map(model => ({
      ...model,
      dimension_tables: model.dimension_tables.map(table => ({
        ...table,
        dim_cols: table.dim_cols.map(column => ({
          ...column,
          visible: column.visible.map(visible => ({
            ...visible,
            name: visible.type === 'user' ? visible.name.toUpperCase() : visible.name,
          })),
        })),
      })),
      measures: model.measures.map(measure => ({
        ...measure,
        visible: measure.visible.map(visible => ({
          ...visible,
          name: visible.type === 'user' ? visible.name.toUpperCase() : visible.name,
        })),
      })),
    })),
    named_sets: json.named_sets.map(namedSet => ({
      ...namedSet,
      visible: namedSet.visible.map(visible => ({
        ...visible,
        name: visible.type === 'user' ? visible.name.toUpperCase() : visible.name,
      })),
    })),
    calculate_measures: json.calculate_measures.map(calculateMeasure => ({
      ...calculateMeasure,
      visible: calculateMeasure.visible.map(visible => ({
        ...visible,
        name: visible.type === 'user' ? visible.name.toUpperCase() : visible.name,
      })),
    })),
  };
}
