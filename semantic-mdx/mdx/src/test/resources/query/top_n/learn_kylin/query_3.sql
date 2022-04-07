-- skip
-- Top N，层级结构，下钻一层
SELECT
  NON EMPTY Hierarchize(
    AddCalculatedMembers(
      {DrilldownLevel(
        {DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},
        [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT]
      )}
    )
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM (
  SELECT
    Generate(
      Hierarchize({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]}) AS [XL_Filter_Set_0],
      TopCount(Filter(
        Except(DrilldownLevel(DrilldownLevel([XL_Filter_Set_0].Current, , 0) AS [XL_Filter_HelperSet_0], , 0), [XL_Filter_HelperSet_0]),
        Not IsEmpty([Measures].[_COUNT_])
      ), 6, [Measures].[_COUNT_])
    ) ON COLUMNS
  FROM [learn_kylin]
  WHERE ([Measures].[_COUNT_])
)
WHERE ([Measures].[_COUNT_])
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH
  SET [XL_Col_Dim_0] AS
    'VisualTotals(Distinct(
      Hierarchize({DrillDownLevelTop({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]}, 6, , [Measures].[_COUNT_])})
    ))'
SELECT
  NON EMPTY Hierarchize(
    Intersect(
      AddCalculatedMembers(
        {DrilldownLevel(
          {DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},
          [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT]
        )}),
      [XL_Col_Dim_0]
    )
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM [learn_kylin]
WHERE ([Measures].[_COUNT_])
CELL PROPERTIES [VALUE],[FORMAT_STRING],[LANGUAGE],[BACK_COLOR],[FORE_COLOR],[FONT_FLAGS]