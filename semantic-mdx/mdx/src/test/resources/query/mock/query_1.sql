-- 虚拟数据，用于测试 slicer 重复情况
WITH
  MEMBER [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[Slicer_0] AS 'Aggregate({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0], [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[1]})'
SELECT
  NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM (
  SELECT
     ({[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN],[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US]}) ON COLUMNS
  FROM [learn_kylin]
  WHERE ([Measures].[SUM_PRICE])
)
WHERE ([Measures].[SUM_PRICE],[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[JP])
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH
  MEMBER [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[Slicer_0] AS 'Aggregate({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0], [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[1]})'
  MEMBER [KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[Slicer_0] AS 'Aggregate({[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN], [KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US], [KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[JP]})'
SELECT
  NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS
FROM [learn_kylin]
WHERE ([Measures].[SUM_PRICE],[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[Slicer_0])
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS