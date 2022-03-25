## 查询语法

MDX for Kylin 目前支持的 MDX 语法属于标准 MDX 的子集，在某些细节上与最新标准存在一些差异。

本文介绍 MDX for Kylin 支持的语法结构。

### SELECT 语句

采用 BNF 描述的语法结构：

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

### 语法限制

SELECT_WITH_CLAUSE

  - MDX 目前仅支持 MEMBER 和 SET， 计算度量和命名集需要在数据集中定义。

SELECT_QUERY_AXIS_CLAUSE

  - 特定 Axis 上的查询语句， MDX 仅对最多使用两个 Axis 的情况下启用执行优化。

SELECT_SUBCUBE_CLAUSE

  - 指定查询 Cube，目前支持 Cube_Name，对子查询有限支持。

SELECT_SLICER_AXIS_CLAUSE

  - 表达式必须包含 Dimension 和 Hierarchy 中的 Member，不应该包含 SELECT_QUERY_AXIS_CLAUSE 表达式中所引用的 Member。

### 语法参数

| 参数 | 描述 |
|-----|-----|
| Set_Expression | 返回 Set 的有效多维表达式 |
| Integer_Expression | 一个介于 0 和 127 之间的整数 |
| Cube_Name | 提供 Cube 名称的有效字符串 |
| Tuple_Expression | 返回 Tuple 的有效多维表达式 |
| Cell_Property_Name | 表示 Cell 属性的有效字符串 |
| Dimension_Property_Name | 表示 Dimension 属性的有效字符串 |
| Level_Property_Name | 表示 Level 属性的有效字符串 |
| Member_Property_Name | 表示 Member 属性的有效字符串 |

