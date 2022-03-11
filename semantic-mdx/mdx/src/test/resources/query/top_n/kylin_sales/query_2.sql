-- skip
SELECT
  {[Measures].[Count]} ON COLUMNS,
  NON EMPTY Hierarchize(
    AddCalculatedMembers({DrilldownLevel({[Kylin Cal Dt].[Cal Dt].[All]})})
  ) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS
FROM (
  SELECT
    Generate(
      Hierarchize([Kylin Cal Dt].[Time].[Year Beg Dt].Members) AS [XL_Filter_Set_0],
      TopCount(
        Filter(
          Except(
            DrilldownLevel([XL_Filter_Set_0].Current AS [XL_Filter_HelperSet_0], , 0),
            [XL_Filter_HelperSet_0]),
          Not IsEmpty([Measures].[Count])
        ),
        2,
        [Measures].[Count]
      )
    ) ON COLUMNS
  FROM [Kylin Sales]
)
CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS