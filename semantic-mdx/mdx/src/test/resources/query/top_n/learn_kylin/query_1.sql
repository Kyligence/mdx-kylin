-- Top N, 层级维度
-- 活动字段 YEAR_BEG_DT，选择 YEAR_BEG_DT 筛选 Top 2
SELECT
  NON EMPTY Hierarchize(
    AddCalculatedMembers({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})})
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM (
  SELECT
    Generate(
      Hierarchize({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]}) AS [XL_Filter_Set_0],
      TopCount(
        Filter(
          Except(
            DrilldownLevel([XL_Filter_Set_0].Current AS [XL_Filter_HelperSet_0], , 0),
            [XL_Filter_HelperSet_0]),
          Not IsEmpty([Measures].[_COUNT_])
        ),
        2,
        [Measures].[_COUNT_])
    ) ON COLUMNS
  FROM [learn_kylin]
  WHERE ([Measures].[_COUNT_])
)
WHERE ([Measures].[_COUNT_])
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH
  SET [XL_Col_Dim_0] AS
    'VisualTotals(Distinct(Hierarchize({DrilldownLevelTop({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]},2,,[Measures].[_COUNT_])})))'
SELECT
  NON EMPTY Hierarchize(
    Intersect(
      AddCalculatedMembers({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}),
      [XL_Col_Dim_0]
    )
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM [learn_kylin]
WHERE ([Measures].[_COUNT_])
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS