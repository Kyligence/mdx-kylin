-- CAL_DT-Hierarchy 综合测试
-- 下钻到 QTR_BEG_DT，展开全部 MONTH_BEG_DT
-- 在字段 MONTH_BEG_DT 级别筛选：值 大于 400
SELECT
  NON EMPTY Hierarchize(AddCalculatedMembers(DrilldownMember({{DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])}}, {[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01],[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-04-01],[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-07-01],[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-10-01]}))) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM (
  SELECT
    Filter(Hierarchize([KYLIN_CAL_DT].[cal_dt-Hierarchy].[MONTH_BEG_DT].Members), ([Measures].[_COUNT_]>400)) ON COLUMNS
  FROM [learn_kylin]
  WHERE ([Measures].[_COUNT_])
)
WHERE ([Measures].[_COUNT_])
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH
  SET [XL_Col_Dim_0] AS 'VisualTotals(Distinct(Filter(Hierarchize([KYLIN_CAL_DT].[cal_dt-Hierarchy].[MONTH_BEG_DT].Members), ((([Measures].[_COUNT_] > 400))))))'
SELECT
  NON EMPTY Hierarchize(
    Intersect(
      AddCalculatedMembers(DrilldownMember({{DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}, [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])}}, {[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01], [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-04-01], [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-07-01], [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-10-01]})),
      [XL_Col_Dim_0]
    )
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM [learn_kylin]
WHERE ([Measures].[_COUNT_])
CELL PROPERTIES [VALUE],[FORMAT_STRING],[LANGUAGE],[BACK_COLOR],[FORE_COLOR],[FONT_FLAGS]