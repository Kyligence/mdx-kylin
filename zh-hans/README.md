## MDX for Kylin 概述

MDX for Kylin 是基于 Mondrian 二次开发的、由 Kyligence 贡献的、使用 Apache Kylin 作为数据源的 MDX 查询引擎 。MDX for Kylin 的使用体验比较接近 Microsoft SSAS，可以集成多种数据分析工具，包括 Microsoft Excel、Tableau 等，可以为大数据分析场景下提供更极致的体验。

MDX for Kylin 是在决策支持和业务分析中使用的分析数据引擎。MDX for Kylin 助力消除数据孤岛，统一数据口径，提高数据业务转化效率，提升运营决策能力。欢迎阅读 [技术文章](https://mp.weixin.qq.com/s/w4nTjwh0sq6ze4gyXwL1HA) 了解更多。

MDX for Kylin 相对其它开源 MDX 查询引擎，具有以下优势：

- 更好支持 BI (Excel/Tableau/Power BI 等) 产品，适配 XMLA 协议；
- 针对 BI 的 MDX Query 进行了特定优化重写；
- 适配 Kylin 查询，通过 Kylin 的预计算能力加速 MDX 查询；
- 通过简洁易懂的操作界面，提供了统一的指标定义和管理能力。

### 语义层 - 强大的业务语义层和MDX查询接口

MDX for Kylin 能自动同步 Kylin 中的模型，并基于这些模型进行语义定义，将数据模型转换为业务友好的语言，赋予数据业务价值。MDX for Kylin 语义层为业务提供统一的分析指标库，支持年累计，月累计，同比环比等复杂业务计算，统一业务数据口径。MDX for Kylin 语义层还提供了 MDX 查询接口，可以对接 Excel、Tableau、Smartbi 等 BI 工具进行多维分析。

### 细粒度权限管控

MDX for Kylin 提供了对于数据集所有语义信息的权限管控，满足分析平台对于数据安全管理的需求。
