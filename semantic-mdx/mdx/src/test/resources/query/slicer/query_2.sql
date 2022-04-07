-- 测试 Slicer，查询轴有三个 Hierarchy，即 CrossJoin 两次
SELECT
  {[Measures].[_COUNT_],[Measures].[SUM_PRICE],[Measures].[MAX_PRICE],[Measures].[MIN_PRICE]} DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS ,
  NON EMPTY CrossJoin(
    CrossJoin(
      Hierarchize(AddCalculatedMembers(DrilldownMember({{DrilldownLevel({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT])}}, {[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01].&[2012-01-01],[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01].&[2012-02-01],[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01].&[2012-03-01]}))),
      Hierarchize(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[All]})}))),
    Hierarchize(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].[All]})}))
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS
FROM (
  SELECT
    ({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01]},{[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US], [KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN]},{[KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[0], [KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[1], [KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[2], [KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[3]}) ON COLUMNS
  FROM (
    SELECT
      ({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0],[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[1],[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[4],[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[5]}) ON COLUMNS
    FROM [learn_kylin]
  )
) CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH
  SET [XL_Row_Dim_0] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01]), Descendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01])})))'
  SET [XL_Row_Dim_1] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US]), Ascendants([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN])})))'
  SET [XL_Row_Dim_2] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[0]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[0]), Ascendants([KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[1]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[1]), Ascendants([KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[2]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[2]), Ascendants([KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[3]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].&[3])})))'
  MEMBER [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[Slicer_0] AS 'Aggregate({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0], [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[1], [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[4], [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[5]})'
SELECT
  {[Measures].[_COUNT_], [Measures].[SUM_PRICE], [Measures].[MAX_PRICE], [Measures].[MIN_PRICE]} DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS,
  NON EMPTY CrossJoin(
    CrossJoin(
      Hierarchize(Intersect(AddCalculatedMembers(DrilldownMember({{DrilldownLevel({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}, [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])}, [KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT])}}, {[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01].&[2012-01-01], [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01].&[2012-02-01], [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01].&[2012-03-01]})), [XL_Row_Dim_0])),
      Hierarchize(Intersect(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[All]})}), [XL_Row_Dim_1]))),
    Hierarchize(Intersect(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_SELLER_LEVEL].[All]})}), [XL_Row_Dim_2]))
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS
FROM [learn_kylin]
WHERE ([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[Slicer_0])
CELL PROPERTIES [VALUE],[FORMAT_STRING],[LANGUAGE],[BACK_COLOR],[FORE_COLOR],[FONT_FLAGS]