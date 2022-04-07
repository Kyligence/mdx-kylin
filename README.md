# MDX for Kylin Overview
## What is MDX for Kylin?
MDX for Kylin is an MDX query engine based on Mondrian's secondary development, and contributed by Kyligence, using Apache Kylin 4 as a data source. The use experience of MDX for Kylin is similar to Microsoft SSAS, in the way that it can integrate a number of data analysis tools, including Microsoft Excel and Tableau, to provide a better experience for big data analysis scenarios.
Use MDX for Kylin to create business metrics


#### Atomic metrics and business metrics

In Kylin Cube, the metrics we create are aggregate calculations on a single column (except TopN). It is relatively simple and only includes a limited number of aggregate functions, i.e., Sum/Max/Min/Count/Count Distinct. We call them atomic metrics. In actual business scenarios and based on atomic metrics, we can perform complex calculations on atomic metrics to create composite metrics with business implications. We call them business metrics.

#### Hierarchy, calculated measure, and named set

Hierarchy: Hierarchies are collections of dimension-based levels that can be used to improve the analytical capabilities of data analysts. For example, you can create a time hierarchy with levels such as year, quarter, month, week, and day. In this way, analysts can analyze sales year by year in the client application first, and then expand "Quarter > Month > Week > Day" for more detailed analysis when needed.

Calculated measure: Calculated measures are new metrics/metrics as a result of composite computing on atomic metrics with MDX expressions. We mainly use calculated measures to create business metrics.

Named set: In the use of MDX for Kylin, there is often a need to reuse a set of members. Such need can be met by defining a named set. A NamedSet is a set of members calculated by using a specified expression. The named set can be placed directly on the axis for display, or used in an expression for calculated measures or other named sets.

#### Create a semantic model

In Kylin 4, we create a data model based on the relationship between tables, and define dimensions and measures on Cubes. We can regard these measures as atomic metrics. In MDX for Kylin, we join related Kylin Cubes to create datasets; and create business metrics with business implications based on atomic metrics.

#### Data analysis
In daily use, the client sends an MDX query to MDX for Kylin, which will then parse the MDX query into SQL and send it to Kylin. After that, Kylin will answer the SQL query based on the pre-computed Cuboid and return the result to MDX for Kylin. Then, MDX for Kylin will do some calculation of derived metrics, and return the multidimensional data results to the client as the last step.

#### Process overview
In general, supporting MDX interface can enhance the semantic role of Kylin and provide users a unified data analysis and management experience to better leverage the value of data. The figure below shows the process of processing business metrics from raw data from the bottom up.

## Technical advantages of MDX for Kylin
MDX for Kylin has the following advantages over other open-source MDX query engines:
- It can better support BI (Excel/Tableau/Power BI, etc.) products and adapt to the XMLA protocol;
- It is rewritten with specific optimizations for BI's MDX Query;
- It adapts to Kylin queries and can use Kylin's pre-computing function to speed up MDX queries;
- It provides unified metrics definition and management function through an easy-to-use operation interface.

## Quick start with Docker
Test environment
- Macbook Pro, Docker Desktop (latest version)
- A virtual machine in Windows 10, Microsoft Excel (for Windows)


#### Start the container

```sh
docker run -d \
    -m 8g \
    -p 7070:7070 \
    -p 7080:7080 \
    -p 8088:8088 \
    -p 50070:50070 \
    -p 8032:8032 \
    -p 8042:8042 \
    -p 2181:2181 \
    --name kylin-4.0.1 \
    apachekylin/apache-kylin-standalone:kylin-4.0.1-mondrian
```
