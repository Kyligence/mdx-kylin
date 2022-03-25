## Query syntax

The MDX syntax currently supported by MDX for Kylin is a subset of the standard MDX, which is different from the latest standard in some details.

This paper introduces the syntax structure supported by MDX for Kylin.

### Select statement

Syntax structure described by BNF: 

```BNF
[ WITH <SELECT_WITH_CLAUSE>   
  [ , <SELECT_WITH_CLAUSE>...n ]   
]
SELECT [ *   
    | ( <SELECT_QUERY_AXIS_CLAUSE>   
        [ , <SELECT_QUERY_AXIS_CLAUSE>,...n ]   
      )
    ]  
FROM
   <SELECT_SUBCUBE_CLAUSE>   
      [ <SELECT_SLICER_AXIS_CLAUSE> ]  
      [ <SELECT_CELL_PROPERTY_LIST_CLAUSE> ]  

<SELECT_WITH_CLAUSE> ::=   
   ( MEMBER <CREATE MEMBER body clause>)   
   | ( SET <CREATE SET body clause>)

<SELECT_QUERY_AXIS_CLAUSE> ::=  
   [ NON EMPTY ] Set_Expression  
   [ <SELECT_DIMENSION_PROPERTY_LIST_CLAUSE> ]   
      ON Integer_Expression   
       | AXIS(Integer)   
       | COLUMNS   
       | ROWS   
       | PAGES   
       | SECTIONS   
       | CHAPTERS   

<SELECT_SUBCUBE_CLAUSE> ::=  
      Cube_Name   
   | [NON VISUAL] (SELECT   
                  [ *   
       | ( <SELECT_QUERY_AXIS_CLAUSE> [ ,   
           <SELECT_QUERY_AXIS_CLAUSE>,...n ] )   
         ]   
            FROM   
         <SELECT_SUBCUBE_CLAUSE>   
         <SELECT_SLICER_AXIS_CLAUSE> )  

<SELECT_SLICER_AXIS_CLAUSE> ::=   
      WHERE Tuple_Expression  

<SELECT_CELL_PROPERTY_LIST_CLAUSE> ::=   
   [ CELL ] PROPERTIES Cell_Property_Name   
      [ , Cell_Property_Name,...n ]  

<SELECT_DIMENSION_PROPERTY_LIST_CLAUSE> ::=  
   [DIMENSION] PROPERTIES   
      (Dimension_Property_Name [,Dimension_Property_Name,...n ] )   
    | (Level_Property_Name [, Level_Property_Name,...n ] )   
    | (Member_Property_Name [, Member_Property_Name,...n ] )
```

### Syntax restrictions

SELECT_WITH_CLAUSE

  - At present, MDX only supports member and set, and the calculated measure and named set need to be defined in the dataset.

SELECT_QUERY_AXIS_CLAUSE

  - MDX only enables execution optimization for queries on a specific axis when up to two axes are used.

SELECT_SUBCUBE_CLAUSE

  - Specify the query cube. Currently, it supports Cube_Name and has limited support for subqueries.

SELECT_SLICER_AXIS_CLAUSE

  - The expression must contain members in dimension and hierarchy, not members referenced in SELECT_QUERY_AXIS_CLAUSE expression.

### Syntax parameters

| Parameter | Description |
|-----|-----|
| Set_Expression | Returns a valid multidimensional expression of a set | 
| Integer_Expression | An integer between 0 and 127 |
| Cube_Name | Provides a valid string for the cube name |
| Tuple_Expression | Returns a valid multidimensional expression for tuple |
| Cell_Property_Name | A valid string representing the cell property |
| Dimension_Property_Name | A valid string representing the dimension property |
| Level_Property_Name | A valid string representing the level property |
| Member_Property_Name | A valid string representing the member property |
