-- 标签过滤 - 以结尾 - 含筛选器
-- 行：cal_dt-Hierarchy（活动：QTR_BEG_DT）
-- 列：ACCOUNT_BUYER_LEVEL（筛选：部分值）, [Values]
-- 值：_COUNT_, MAX_PRICE
-- 滤：ACCOUNT_COUNTRY
SELECT
  NON EMPTY CrossJoin(Hierarchize(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[All]})})), {[Measures].[_COUNT_],[Measures].[MAX_PRICE]}) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS ,
  NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS
FROM (
  SELECT
    Filter([KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT].Members, (Right([KYLIN_CAL_DT].[cal_dt-Hierarchy].CurrentMember.member_caption,2)="01")) ON COLUMNS
  FROM (
    SELECT
      ({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[2], [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[1], [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0]}) ON COLUMNS
    FROM (
      SELECT
        ({[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN],[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US]}) ON COLUMNS
      FROM [learn_kylin]
    )
  )
)
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH
  SET [XL_Col_Dim_0] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[2]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[2]), Ascendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[1]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[1]), Ascendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0])})))'
  SET [XL_Row_Dim_0] AS 'VisualTotals(Distinct(Filter([KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT].Members, (((Right([KYLIN_CAL_DT].[cal_dt-Hierarchy].CurrentMember.member_caption, 2) = "01"))))))'
  MEMBER [KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[Slicer_0] AS 'Aggregate({[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN], [KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US]})'
SELECT
  NON EMPTY CrossJoin(Hierarchize(Intersect(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[All]})}), [XL_Col_Dim_0])), {[Measures].[_COUNT_], [Measures].[MAX_PRICE]}) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS,
  NON EMPTY Hierarchize(Intersect(AddCalculatedMembers({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}, [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])}), [XL_Row_Dim_0])) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS
FROM [learn_kylin]
WHERE ([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[Slicer_0])
CELL PROPERTIES [VALUE],[FORMAT_STRING],[LANGUAGE],[BACK_COLOR],[FORE_COLOR],[FONT_FLAGS]