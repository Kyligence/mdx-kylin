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
import * as ace from 'ace-builds/src-noconflict/ace';

ace.define(
  'ace/mode/mdx_highlight_rules',
  [
    'require',
    'exports',
    'module',
    'ace/lib/oop',
    'ace/mode/text_highlight_rules',
  ],
  (require, exports) => {
    const oop = ace.require('ace/lib/oop');
    const { TextHighlightRules } = ace.require('ace/mode/text_highlight_rules');

    function MdxHighlightRules() {
      // MDX normal keywords
      const keywords = 'ABSOLUTE|DESC|SELF_BEFORE_AFTER|ACTIONPARAMETERSET|SESSION|DESCRIPTION|SET|AFTER|ALL|SORT|' +
        'AND|AS|STORAGE|ASC|MEASURE|AVERAGE|MEMBER|BASC|DROP|STRTOVAL|' +
        'BDESC|EMPTY|BEFORE|END|BEFORE_AND_AFTER|EXCLUDEEMPTY|NEST|BY|NO_ALLOCATION|' +
        'CACHE|NO_PROPERTIES|CALCULATE|NON|CALCULATION|TOTALS|FOR|NOT_RELATED_TO_FACTS|TREE|FREEZE|' +
        'CALCULATIONS|FROM|ON|CALL|TYPE|CELL|GLOBAL|OR|' +
        'CELLFORMULASETLIST|GROUP|PAGES|UNIQUE|CHAPTERS|GROUPING|UPDATE|CLEAR|HIDDEN|PASS|' +
        'USE|USE_EQUAL_ALLOCATION|POST|USE_WEIGHTED_ALLOCATION|COLUMN|IGNORE|USE_WEIGHTED_INCREMENT|COLUMNS|INCLUDEEMPTY|INDEX|' +
        'PROPERTY|IS|RECURSIVE|CREATE|RELATIVE|CREATEPROPERTYSET|VISUAL|CREATEVIRTUALDIMENSION|ROWS|WHERE|' +
        'CUBE|SCOPE|WITH|SECTIONS|CURRENTCUBE|SELECT|XOR|SELF|DEFAULT_MEMBER|SELF_AND_AFTER|' +
        'SELF_AND_BEFORE';

      const builtinConstants = 'true|false|null';

      const builtinFunctions =
        // MDX normal function
        'AddCalculatedMembers|Aggregate|Ancestor|Ancestors|Ascendants|Avg|Axis|BottomCount|BottomPercent|' +
        'BottomSum|CalculationCurrentPass|CalculationPassValue|CASE|Children|ClosingPeriod|CoalesceEmpty|Correlation|Count|Count|' +
        'Count|Count|Cousin|Covariance|CovarianceN|Crossjoin|Current|CurrentOrdinal|CustomData|' +
        'DataMember|DefaultMember|Descendants|Distinct|DistinctCount|DrilldownLevel|DrilldownLevelBottom|DrilldownLevelTop|' +
        'DrilldownMember|DrilldownMemberBottom|DrilldownMemberTop|DrillupLevel|DrillupMember|Error|Except|Exists|Extract|Filter|' +
        'FirstChild|FirstSibling|Generate|Head|Hierarchize|IIf|Intersect|IsAncestor|IsEmpty|' +
        'IsGeneration|IsLeaf|IsSibling|Item|Item|KPIGoal|KPIStatus|KPITrend|KPIWeight|KPICurrentTimeMember|' +
        'KPIValue|Lag|LastChild|LastPeriods|LastSibling|Lead|LinkMember|' +
        'LinRegIntercept|LinRegPoint|LinRegR2|LinRegSlope|LinRegVariance|LookupCube|Max|MeasureGroupMeasures|Median|Members|' +
        'Members|MemberToStr|MemberValue|Min|Mtd|NameToSet|NextMember|NonEmpty|NonEmptyCrossjoin|' +
        'OpeningPeriod|Order|Ordinal|ParallelPeriod|Parent|PeriodsToDate|Predict|PrevMember|Properties|Qtd|' +
        'Rank|RollupChildren|Root|SetToArray|SetToStr|Siblings|Stddev|StddevP|Stdev|StdevP|' +
        'StripCalculatedMembers|StrToMember|StrToSet|StrToTuple|StrToValue|Subset|Sum|Tail|This|ToggleDrillState|' +
        'TopCount|TopPercent|TopSum|TupleToStr|Union|UniqueName|UnknownMember|Unorder|UserName|ValidMeasure|' +
        'Value|Var|Variance|VarianceP|VarP|VisualTotals|Wtd|Ytd|' +
        // MDX special function
        '$AdjustedProbability|$Distance|$Probability|$ProbabilityStDev|$ProbabilityStdDeV|$ProbabilityVariance|$StDev|$StdDeV|$Support|$Variance|' +
        'Action|Alter|Append|Automatic|Back_Color|Before_And_Self|Before_Self_After|Break|Boolean|Calculated|' +
        'Catalog_Name|Cell_Ordinal|Cells|Children_Cardinality|Cluster|ClusterDistance|ClusterProbability|Clusters|Column_Values|Content|' +
        'Contingent|Continuous|Cube_Name|Custom|Cyclical|Dimension_Unique_Name|Discrete|Discretized|DrillTrough|Else|' +
        'Equal_Areas|Exclude_Null|Exclusive|Expression|FirstRowset|Fixed|Flattened|Font_Flags|Font_Name|Font_size|' +
        'Fore_Color|Format_String|Formatted_Value|Formula|Hierary_Unique_name|Include_Null|Include_Statistics|Inclusive|Input_Only|IsDescendant|' +
        'Level_Unique_Name|Long|MaxRows|Member_Caption|Member_Guid|Member_Name|Member_Ordinal|Member_Type|Member_Unique_Name|Microsoft_Clustering|' +
        'Microsoft_Decision_Trees|Mining|Model|Model_Existence_Only|Models|Move|Normal|Not|Ntext|Nvarchar|' +
        'OLAP|OpenQuery|Ordered|Parent_Level|Parent_Unique_Name|PMML|Predict_Only|PredictAdjustedProbability|PredictHistogram|Prediction|' +
        'PredictionScore|PredictProbability|PredictProbabilityStDev|PredictProbabilityVariance|PredictStDev|PredictSupport|PredictVariance|Probability|Probability_StDev|Probability_StdDev|' +
        'Probability_Variance|RangeMax|RangeMid|RangeMin|Refresh|Related|Rename|Rollup|Schema_Name|Sequence_Time|' +
        'Server|Shape|Skip|Solve_Order|Support|Text|Thresholds|Under|Uniform|When|' +
        'Abs|Acos|Acosh|AscB|AscW|Asin|Asinh|Atan2|Atanh|Atn|' +
        'CBool|CByte|CDate|CDbl|CInt|Caption|Cast|Chr|ChrB|ChrW|' +
        'Cos|Cosh|CurrentDateMember|CurrentDateString|DDB|Date|DateAdd|DateDiff|DatePart|DateSerial|' +
        'DateValue|Day|Degrees|Exp|FV|FirstQ|Fix|Format|FormatCurrency|FormatDateTime|' +
        'FormatNumber|FormatPercent|Hex|Hour|IPmt|IS EMPTY|IS NULL|InStr|InStrRev|Int|' +
        'IsDate|LCase|LTrim|Left|Len|Log|Log10|Mid|Minute|Month|' +
        'Now|Oct|PPmt|PV|Parameter|Percentile|Pi|Pmt|Power|RTrim|' +
        'Radians|Rate|Replace|Right|Round|SLN|SYD|Second|Sgn|Sin|' +
        'Sinh|Space|Sqr|SqrtPi|Str|StrComp|StrReverse|String|Tan|Tanh|' +
        'ThirdQ|Time|TimeSerial|TimeValue|Timer|Trim|TypeName|UCase|Val|Weekday|' +
        'WeekdayName|Year|Exclude';

      const builtinVariables =
        'Leaves|Level|Levels|Dimension|Dimensions|AllMembers|CurrentMember|Hierarchy|Name';

      const dataTypes =
        'int|numeric|decimal|date|varchar|char|bigint|float|double|bit|binary|text|set|timestamp|' +
        'money|real|number|integer';

      const keywordMapper = this.createKeywordMapper(
        {
          'support.function': builtinFunctions,
          keyword: keywords,
          'constant.language': builtinConstants,
          'storage.type': dataTypes,
          variable: builtinVariables,
        },
        'identifier',
        true,
      );

      this.$rules = {
        start: [
          {
            token: 'comment',
            regex: '--.*$',
          },
          {
            token: 'comment',
            start: '/\\*',
            end: '\\*/',
          },
          {
            token: 'string', // " string
            regex: '".*?"',
          },
          {
            token: 'string', // ' string
            regex: "'.*?'",
          },
          {
            token: 'string', // ` string (apache drill)
            regex: '`.*?`',
          },
          {
            token: 'constant.numeric', // float
            regex: '[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b',
          },
          {
            token: keywordMapper,
            regex: '[a-zA-Z_$][a-zA-Z0-9_$]*\\b',
          },
          {
            token: 'keyword.operator',
            regex:
              '\\+|\\-|\\/|\\/\\/|%|<@>|@>|<@|&|\\^|~|<|>|<=|=>|==|!=|<>|=',
          },
          {
            token: 'paren.lparen',
            regex: '[\\(]',
          },
          {
            token: 'paren.rparen',
            regex: '[\\)]',
          },
          {
            token: 'text',
            regex: '\\s+',
          },
        ],
      };
      this.normalizeRules();
    }

    oop.inherits(MdxHighlightRules, TextHighlightRules);
    /* eslint-disable no-param-reassign */
    exports.MdxHighlightRules = MdxHighlightRules;
  },
);

ace.define(
  'ace/mode/mdx',
  [
    'require',
    'exports',
    'module',
    'ace/lib/oop',
    'ace/mode/text',
    'ace/mode/mdx_highlight_rules',
  ],
  (require, exports) => {
    const oop = ace.require('ace/lib/oop');
    const { Mode: TextMode } = ace.require('ace/mode/text');
    const { MdxHighlightRules } = ace.require('ace/mode/mdx_highlight_rules');

    const Mode = function () {
      this.HighlightRules = MdxHighlightRules;
      // 自动补齐方括号，会影响带方括号的自动提示
      // this.$behaviour = this.$defaultBehaviour;
    };
    oop.inherits(Mode, TextMode);

    (function () {
      this.lineCommentStart = '--';

      this.$id = 'ace/mode/mdx';
      this.snippetFileId = 'ace/snippets/mdx';
    }.call(Mode.prototype));
    /* eslint-disable no-param-reassign */
    exports.Mode = Mode;
  },
);

(() => {
  ace.require(['ace/mode/mdx'], m => {
    if (typeof module === 'object' && typeof exports === 'object' && module) {
      module.exports = m;
    }
  });
})();
