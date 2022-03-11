-- 包含 INCLUDE_CALC_MEMBERS 以及 子查询语句 的改写用例
SELECT
    {[Measures].[_COUNT_],[Measures].[SUM_PRICE]}
      DIMENSION PROPERTIES PARENT_UNIQUE_NAME,HIERARCHY_UNIQUE_NAME ON COLUMNS ,
    NON EMPTY CrossJoin(
        Hierarchize({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[All]},,,INCLUDE_CALC_MEMBERS)}, POST),
        Hierarchize({DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[All]},,,INCLUDE_CALC_MEMBERS)})
    ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME, HIERARCHY_UNIQUE_NAME ON ROWS
FROM (
    SELECT
      (
        {[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US], [KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN]},
        {[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[2],
         [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL],
         [KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0]}
      ) ON COLUMNS
    FROM (
      SELECT
        (
          {[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01],
           [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2013-01-01]}
        ) ON COLUMNS
      FROM [learn_kylin]
    )
)
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
-- MDX --
WITH
  SET [XL_Row_Dim_0] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[US]), Ascendants([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].&[CN])})))'
  SET [XL_Row_Dim_1] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[2]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[2]), Ascendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL]), Ascendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0]), Descendants([KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].&[0])})))'
  MEMBER [KYLIN_CAL_DT].[cal_dt-Hierarchy].[Slicer_0] AS 'Aggregate({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01], [KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2013-01-01]})'
SELECT
  {[Measures].[_COUNT_], [Measures].[SUM_PRICE]} DIMENSION PROPERTIES PARENT_UNIQUE_NAME, HIERARCHY_UNIQUE_NAME ON COLUMNS,
  NON EMPTY CrossJoin(
    Hierarchize(Intersect({AddCalculatedMembers(DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_COUNTRY].[All]}))}, [XL_Row_Dim_0]), POST),
    Hierarchize(Intersect({AddCalculatedMembers(DrilldownLevel({[KYLIN_ACCOUNT].[ACCOUNT_BUYER_LEVEL].[All]}))}, [XL_Row_Dim_1]))
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME, HIERARCHY_UNIQUE_NAME ON ROWS
FROM [learn_kylin]
WHERE ([KYLIN_CAL_DT].[cal_dt-Hierarchy].[Slicer_0])
CELL PROPERTIES [VALUE],[FORMAT_STRING],[LANGUAGE],[BACK_COLOR],[FORE_COLOR],[FONT_FLAGS]
