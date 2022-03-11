-- skip
-- 值过滤 - 值大于 - 含筛选器
-- 行：cal_dt-Hierarchy（活动：MONTH_BEG_DT）, ACCOUNT_BUYER_LEVEL
-- 列：[Values]
-- 值：_COUNT_, MAX_PRICE, MIN_PRICE, SUM_PRICE
-- 滤：ACCOUNT_COUNTRY
SELECT
  {[Measures].[_COUNT_],[Measures].[MAX_PRICE],[Measures].[MIN_PRICE],[Measures].[SUM_PRICE]}
    DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS ,
  NON EMPTY CrossJoin(
    Hierarchize(AddCalculatedMembers(
      {DrilldownLevel(
        {DrilldownLevel(
          {DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},
          [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT]
        )},
      [KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT])}
    )),
    Hierarchize(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[All]})}))
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS
FROM (
  SELECT
    Filter(
      CrossJoin(
        Hierarchize([KYLIN_CAL_DT].[cal_dt-Hierarchy].[MONTH_BEG_DT].Members),
        Hierarchize([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[ACCOUNT_BUYER_LEVEL].Members)
      ),
      ([Measures].[_COUNT_]>20)
    ) ON COLUMNS
  FROM (
    SELECT
      ({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01]}) ON COLUMNS
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
  SET [XL_Row_Dim_0] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01]), Descendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01].&[2012-01-01])})))'
  SET [XL_Row_Dim_1] AS 'Distinct(Filter(CrossJoin(Hierarchize([KYLIN_CAL_DT].[cal_dt-Hierarchy].[MONTH_BEG_DT].Members), Hierarchize([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[ACCOUNT_BUYER_LEVEL].Members)), ((([Measures].[_COUNT_] > 20)))))'
  MEMBER [KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[Slicer_0] AS 'Aggregate({[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN], [KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US]})'
SELECT {[Measures].[_COUNT_], [Measures].[MAX_PRICE], [Measures].[MIN_PRICE], [Measures].[SUM_PRICE]} DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS,
  NON EMPTY Intersect(CrossJoin(
    Hierarchize(Intersect(
        AddCalculatedMembers({DrilldownLevel({DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}, [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])}, [KYLIN_CAL_DT].[cal_dt-Hierarchy].[QTR_BEG_DT])}),
        [XL_Row_Dim_0])),
    Hierarchize(AddCalculatedMembers({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[All]})}))
  ), [XL_Row_Dim_1]) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS
FROM [learn_kylin]
WHERE ([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[Slicer_0])
CELL PROPERTIES [VALUE],[FORMAT_STRING],[LANGUAGE],[BACK_COLOR],[FORE_COLOR],[FONT_FLAGS]
