-- 值筛选 - 值大于 - 含筛选器
-- 子查询含有 slicer-axis
SELECT
  NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[All]})})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS ,
  NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS
FROM (
  SELECT
    Filter(Hierarchize([KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT].Members), ([Measures].[_COUNT_]>100)) ON COLUMNS
    FROM [learn_kylin]
    WHERE ([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[All],[Measures].[_COUNT_])
)
WHERE ([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[All],[Measures].[_COUNT_])
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH SET [XL_Row_Dim_0] AS 'VisualTotals(Distinct(Filter(Hierarchize([KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT].Members), ((([Measures].[_COUNT_] > 100))))))'
SELECT
  NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[All]})})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS,
  NON EMPTY Hierarchize(Intersect(AddCalculatedMembers({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}, [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])}), [XL_Row_Dim_0])) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS
FROM [learn_kylin]
WHERE ([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[All], [Measures].[_COUNT_])
CELL PROPERTIES [VALUE],[FORMAT_STRING],[LANGUAGE],[BACK_COLOR],[FORE_COLOR],[FONT_FLAGS]