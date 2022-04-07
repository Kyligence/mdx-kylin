-- CAL_DT-Hierarchy 综合测试
-- 按层级展开，隐藏子级别的部分结果
SELECT
  NON EMPTY Hierarchize(AddCalculatedMembers(DrilldownMember(
    {{DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}},
    {[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01],
     [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2013-01-01],
     [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2014-01-01]}
  ))) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM (
  SELECT
    ({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-10-01],
      [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-07-01],
      [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01],
      [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2014-01-01],
      [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2013-01-01]}
    ) ON COLUMNS
  FROM [learn_kylin]
)
WHERE ([Measures].[_COUNT_])
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH SET [XL_Col_Dim_0] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-10-01]), Descendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-10-01]), Ascendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-07-01]), Descendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-07-01]), Ascendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01]), Descendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01]), Ascendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2014-01-01]), Descendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2014-01-01]), Ascendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2013-01-01]), Descendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2013-01-01])})))'
SELECT
  NON EMPTY Hierarchize(Intersect(
    AddCalculatedMembers(DrilldownMember(
      {{DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}},
      {[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01],
       [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2013-01-01],
       [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2014-01-01]}
    )),
    [XL_Col_Dim_0]
  )) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM [learn_kylin]
WHERE ([Measures].[_COUNT_])
CELL PROPERTIES [VALUE],[FORMAT_STRING],[LANGUAGE],[BACK_COLOR],[FORE_COLOR],[FONT_FLAGS]