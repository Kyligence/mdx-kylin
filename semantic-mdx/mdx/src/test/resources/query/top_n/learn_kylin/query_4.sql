-- skip
-- Top N，层级结构，下钻一层，筛选下一层
SELECT
  NON EMPTY Hierarchize(
    AddCalculatedMembers({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])})
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM (
  SELECT
    Generate(
      Hierarchize([KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT].Members) AS [XL_Filter_Set_1],
      TopCount(Filter(
        Except(DrilldownLevel([XL_Filter_Set_1].Current AS [XL_Filter_HelperSet_1], , 0), [XL_Filter_HelperSet_1]),
        Not IsEmpty([Measures].[_COUNT_])
      ), 3, [Measures].[_COUNT_])
    ) ON COLUMNS
  FROM (
    SELECT
      Generate(
        Hierarchize({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]}) AS [XL_Filter_Set_0],
        TopCount(Filter(
          Except(DrilldownLevel(DrilldownLevel([XL_Filter_Set_0].Current, , 0) AS [XL_Filter_HelperSet_0], , 0), [XL_Filter_HelperSet_0]),
          Not IsEmpty([Measures].[_COUNT_])
        ), 2, [Measures].[_COUNT_])
      ) ON COLUMNS
    FROM [learn_kylin]
    WHERE ([Measures].[_COUNT_])
  )
  WHERE ([Measures].[_COUNT_])
)
WHERE ([Measures].[_COUNT_])
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH
  SET [XL_Col_Dim_0] AS 'VisualTotals(Distinct(Hierarchize({DrillDownLevelTop({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT]}, 3, , [Measures].[_COUNT_])})))'
  SET [XL_Col_Dim_1] AS 'VisualTotals(Distinct(Hierarchize({DrillDownLevelTop({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]}, 2, , [Measures].[_COUNT_])})))'
SELECT
  NON EMPTY Hierarchize(
    Intersect(
      Intersect(
        AddCalculatedMembers(
          {DrilldownLevel(
            {DrilldownLevel(
              {[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]}
            )},
          [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])}),
        [XL_Col_Dim_0]),
      [XL_Col_Dim_1]
    )
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM [learn_kylin]
WHERE ([Measures].[_COUNT_])