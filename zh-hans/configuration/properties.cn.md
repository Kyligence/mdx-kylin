## 基本配置参数

MDX for Kylin 的配置文件 `insight.properties` 保存在 `conf` 目录中。用户可以通过修改相关配置，实现环境适配、性能优化等目的。

用户修改任何配置项之后，需要重启后方可生效。

- [1. 通用配置](#1-通用配置)
- [2. Semantic Service 模块相关配置](#2-semantic-service-模块相关配置)
- [3. MDX 模块相关配置](#3-mdx-模块相关配置)


### 1. 通用配置

| 配置项                               | 描述                                                                                                        |
|-----------------------------------|-----------------------------------------------------------------------------------------------------------|
| insight.kylin.host | 该参数指定需要连接的 Kylin 服务器的 IP 地址或 Hostname，如 `127.0.0.1`，默认值`localhost`                         |
| insight.kylin.port | 该参数指定需要连接的 Kylin 服务器的端口号，默认值`7070`                                                         |
| insight.kylin.ssl  | MDX for Kylin 访问 Kylin 是否启用 https，默认值`false`                                               |
| insight.database.type             | MDX for Kylin 使用的元数据库的类型，默认值`mysql`                                                                       |
| insight.database.useSSL           | MDX for Kylin 元数据库是否使用SSL连接，默认值`false`                                                                    |
| insight.database.ip               | MDX for Kylin 需要连接的元数据库的 IP 地址或 Hostname，默认值`localhost`                                                   |
| insight.database.port             | MDX for Kylin 需要连接的元数据库的端口号，默认值`3306`                                                                     |
| insight.database.name             | MDX for Kylin 需要连接的元数据库名称，默认值`insight`                                                                    |
| insight.database.username         | MDX for Kylin 需要连接的元数据库的用户名，默认值`root`                                                                     |
| insight.database.password         | MDX for Kylin 需要连接的元数据库的密码，默认值`698d2c7907fc9b6dbe7f8a8c4cb0297a`，即密码 `root` 的加密串                          |
| insight.mdx.cluster.nodes         | 该配置用于生成诊断包使用，该配置项需要您填写 MDX for Kylin 集群中所有节点的 IP 和 Port 信息。如果配置多个节点，请用英文逗号隔开，例如 `IP_1:Port_1,IP_2:Port_2` |
| insight.semantic.secret-key       | 该配置用于加密 MDX for Kylin 所需要用到密钥串，默认值`3500d18495a54c54b9a3d56641a8a521`。例如用于加密连接的元数据密码或用户登录密码，用户可按照需要变更自己的密钥串，如果变更对应的密钥串，则用户需要重新加密对应的密码等，建议在初次安装部署 MDX for Kylin 时候变更 |


### 2. Semantic Service 模块相关配置

| 配置项                                                 | 描述                                                                                                                                            |
|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| insight.semantic.datasource-version                 | 该参数指定 MDX for Kylin 中 semantic service 连接的引擎的版本号，仅支持 `2`，表示连接的 Kylin                                                                  |
| insight.semantic.port                               | 该参数指定 MDX for Kylin 中 semantic service 的端口号，默认值`7080`                                                                                         |
| insight.semantic.meta.sync.project-page-size        | 该参数指定 MDX for Kylin 中 semantic service 同步项目时，单页获取最大项目数，默认值`1000`                                                                              |
| insight.dataset.verify.interval.count               | 该参数指定 MDX for Kylin 中 semantic service 进行数据集校验的时间间隔数，单位：20秒，例如：2 表示 2*20=40 秒。当且仅 `insight.dataset.verify.enable` 配置为 true 时生效，默认值`15`,即 5 分钟 |
| insight.dataset.verify.enable                       | 该参数指定 MDX for Kylin 中 semantic service 是否开启数据集同步校验功能，默认值`true`                                                                                |
| insight.semantic.model.version.verify               | 该参数指定 MDX for Kylin 中 semantic service 是否开启元数据模型版本检查，默认值`false`                                                                               |
| insight.semantic.startup.sync.enable                | 该参数指定 MDX for Kylin 中 semantic service 是否开启启动服务即开始进行同步任务，默认值`true`                                                                            |
| insight.semantic.segment.sync.enable                | 该参数指定 MDX for Kylin 中 semantic service 是否开启检查 Kylin 的 Segment 变化，并根据 Segment 变化清除 MDX for Kylin 的缓存，默认值`true`                  |
| insight.semantic.meta.sync.user-page-size           | 该参数指定 MDX for Kylin 中 semantic service 从 Kylin 中同步用户信息时每页的大小，默认值`100000`                                                       |
| insight.semantic.cookie-age                         | 该参数指定 MDX for Kylin 中 semantic service Web UI 访问时用户 cookie 的生命时长，默认值`86400`，即 86400 秒                                                         |
| insight.semantic.checkout.dataset.connect           | 该参数指定 MDX for Kylin 的数据集保存时，是否要进行连接校验，默认值`true`                                                                                               |
| insight.semantic.connect.timeout                    | MDX for Kylin 连接 Kylin 的连接超时时间，单位是毫秒，默认值`5000`                                                                                 |
| insight.semantic.socket.timeout                     | MDX for Kylin 连接 Kylin 的连接 socket 超时时间，单位是毫秒，默认值`10000`                                                                        |
| insight.kylin.only.normal.dim.enable | MDX for Kylin 在多对多场景下是否只从 Kylin 拉取普通维度，默认值`false`，当前 Kylin 暂不支持 多对多场景                                                                           |
| insight.semantic.connection.request.timeout         | MDX for Kylin 连接 Kylin 的连接请求超时时间，单位是毫秒，默认值`8000`                                                                               |
| insight.semantic.max-http-header-size               | MDX for Kylin 的 http 请求 header 的最大长度，单位是 kb，默认值`8192`                                                                                         |
| insight.semantic.meta.keep.mdx-query.max.rows       | 系统中保存 MDX 查询历史记录的最大条数，默认值 `1000000`                                                                                                           |
| insight.semantic.segment.sync.enable                | 是否同步 Kylin 的 Segment 信息，默认值`true`                                                                                              |
| insight.semantic.zookeeper.enable                   | 是否开启将服务信息注册到 ZooKeeper, 默认值`false`                                                                                                            |
| insight.semantic.zookeeper.address                  | ZooKeeper 的地址，默认值`localhost:2181`                                                                                                             |
| insight.semantic.zookeeper.node.path                | 注册的节点地址，默认值`/mdx`                                                                                                                             |
| insight.semantic.zookeeper.session.timeout          | 与 ZooKeeper 连接的超时时间，默认值`30000`                                                                                                                |
| insight.semantic.reject.enable                      | 该参数用于指定是否启用拦截策略，启用后，对符合拦截策略的查询请求将会被拒绝，默认`false`。                                                                                              |

### 3. MDX 模块相关配置

| 配置项                                                       | 描述                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| insight.mdx.servlet-path                                     | 该参数指定 MDX for Kylin 中 mdx 的 servlet 路径，默认值`/mdx/xmla/` |
| insight.mdx.gateway-path                                     | 该参数指定 MDX for Kylin 中 gateway 的 servlet 路径，默认值`/mdx/xmla_server/` |
| insight.mdx.optimize-enabled                                 | 该参数指定 MDX for Kylin 中 mdx 是否开启语句优化，默认值`true` |
| insight.mdx.calculate-total-need                             | 该参数指定 MDX for Kylin 中 mdx 是否计算全局汇总及分类汇总，默认值`true` |
| insight.mdx.schema.create-from-dataset                       | 该参数指定 MDX for Kylin 中 mdx 是否从数据集中创建 schema，默认值`true` |
| insight.mdx.schema.refresh.disable                           | 该参数指定 MDX for Kylin 中 mdx 是否禁用刷新，默认值`false`  |
| insight.mdx.sql.orderby.enable                               | 该参数指定 MDX for Kylin 中是否开启 SQL orderby，默认值`true` |
| insight.mdx.sql.calcite-engine-hint.enable                   | 该参数指定 MDX for Kylin 对接 Excel 时，进行包含 similar to 的查询时，是否强制 Kylin 使用 Calcite 引擎回答，默认值`false` |
| insight.mdx.skip-axis-nonempty-check                         | 该参数指定 MDX for Kylin 中计算轴时是否跳过空值校验，默认值`true` |
| insight.mdx.mondrian.olap.case.sensitive                     | 该参数指定 MDX for Kylin 中 mdx 语句是否大小写敏感，默认值`true` |
| insight.mdx.mondrian.rolap.maxQueryThreads                   | 该参数指定 MDX for Kylin 中 mdx 最大查询线程数，默认值`50`   |
| insight.mdx.mondrian.query.sql.page.size                     | 该参数指定 MDX for Kylin 中 mdx 分页操作时，单页获取最大记录数，默认值`10000` |
| insight.mdx.mondrian.rolap.generate.formatted.sql            | 该参数指定 MDX for Kylin 中 mdx 是否格式化输出 sql 语句，默认值`false` |
| insight.mdx.mondrian.rolap.star.disableCaching               | 该参数指定 MDX for Kylin 中 mdx 是否禁用缓存，默认值`false`  |
| insight.mdx.mondrian.rolap.ignoreInvalidMembers              | 该参数指定 MDX for Kylin 中 mdx 是否忽略无效的 members，默认值`true` |
| insight.mdx.mondrian.rolap.ignoreInvalidMembersDuringQuery   | 该参数指定 MDX for Kylin 中 mdx 在查询中是否忽略无效的 members，默认值`true` |
| insight.mdx.mondrian.cache.expire-minute                     | 该参数指定 MDX for Kylin 中 mdx 缓存过期时间为几分钟，默认值`720` |
| insight.mdx.mondrian.olap.pure-axes-calculate.source.type    | 该参数指定 MDX for Kylin 中 mdx 轴计算时的维度数据源类型，可选的参数有：`SNAPSHOT`, `PARTIAL_CUBE`, `ALL_CUBE`，默认值`PARTIAL_CUBE` |
| insight.mdx.mondrian.olap.pure-axes-calculate.cardinality-gap | 该参数指定 MDX for Kylin 中对只查询单个维度表的 SQL 语句强制使用 `insight.mdx.mondrian.olap.pure-axes-calculate.source.type=SNAPSHOT` 行为的维度表基数上限（不包含），特殊取值 `-1` 表示总是强制使用，`0` 表示不强制使用，默认值为 `-1` |
| insight.mdx.mondrian.olap.pure-axes-calculate.total-cardinality-gap | 该参数指定 MDX for Kylin 中对查询完整的轴的 SQL 语句强制使用 `insight.mdx.mondrian.olap.pure-axes-calculate.source.type=SNAPSHOT` 行为的查询结果基数上限（不包含），特殊取值 `-1` 表示总是强制使用，`0` 表示不强制使用，默认值为 `0` |
| insight.mdx.mondrian.olap.support-non-default-all-member     | 该参数指定 MDX for Kylin 中 mdx 是否支持非默认为所有成员，默认值`false` |
| insight.mdx.mondrian.olap.cell.calc.join-dims                | 该参数指定 MDX for Kylin 中 mdx 在计算 cell 时是否 join 其他维表，默认值`true` |
| insight.mdx.mondrian.rolap.return-null-when-divide-zero      | 该参数指定 MDX for Kylin 中 mdx 除数为 0 时是否返回 0，默认值`true` |
| insight.mdx.mondrian.rolap.optimize-tuple-size-in-aggregate.enable | 该参数指定 MDX for Kylin 中 mdx 聚合时是否开启元组大小优化，默认值`false` |
| insight.mdx.mondrian.rolap.calculate-cell-when-non-measure   | 该参数指定 MDX for Kylin 中 mdx 无度量时是否聚合单元数据，默认值`false` |
| insight.mdx.mondrian.sql.fetch-dim-from-snapshot             | 该参数指定 MDX for Kylin 中 mdx 是否优先从快照中拉取维度数据，默认值`true` |
| insight.mdx.mondrian.sql.enableOrderBy                       | 该参数指定 MDX for Kylin 中 mdx sql查询时是否添加排序规则，默认值`true` |
| insight.mdx.mondrian.jdbc.timezone                           | 该参数指定 MDX for Kylin 中 mdx jdbc查询服务所在时区，默认值`GMT+8:00` |
| insight.mdx.mondrian.schema.init-level-group-order-by-key    | 该参数指定 MDX for Kylin 中 mdx 是否通过键值去初始化层级级别，默认值`true` |
| insight.mdx.mondrian.query.filter-pushdown.enable            | 该参数指定 MDX for Kylin 中 mdx 是否开启过滤下推，默认值`true` |
| insight.mdx.mondrian.query.filter-pushdown.in-clause-max-size | 该参数指定 MDX for Kylin 中 mdx 子句最大过滤下推数量，默认值`20` |
| insight.mdx.mondrian.query.sql.customize.enable              | 该参数指定 MDX for Kylin 中 mdx 是否开启定制化 sql，默认值`true` |
| insight.mdx.mondrian.query.sql.customize.class               | 该参数指定 MDX for Kylin 中 mdx CustomizeSqlQuery的实现类，默认值`io.kylin.mdx.web.mondrian.rolap.sql.KylinSqlQuery` |
| insight.mdx.mondrian.query.sql.max.size                      | 该参数指定 MDX for Kylin 中 mdx sql 查询时最大查询行数，默认值`50000` |
| insight.mdx.mondrian.rolap.star.disableLocalSegmentCache     | 该参数指定 MDX for Kylin 中 mdx 查询的 segments 是否开启本地缓存，生成特定缓存及查找块缓存 SPI，默认值`false` |
| insight.mdx.mondrian.olap.triggers.enable                    | 该参数指定 MDX for Kylin 中 mdx 当配置属性变更后是否通知 Mondrian，默认值`true` |
| insight.mdx.mondrian.rolap.nonempty                          | 该参数指定 MDX for Kylin 中 mdx 是否每一个查询轴需要非空设置，默认值`false` |
| insight.mdx.mondrian.rolap.compareSiblingsByOrderKey         | 该参数指定 MDX for Kylin 中 mdx 是否对兄弟成员进行和从原表达式拉取成员进行校验，默认值`false` |
| insight.mdx.mondrian.rolap.maxSqlThreads                     | 该参数指定 MDX for Kylin 中 mdx 支持 SQL 的 Mondrian 实例的最大数量，默认值`100` |
| insight.mdx.mondrian.rolap.maxCacheThreads                   | 该参数指定 MDX for Kylin 中 mdx 支持缓存时每一个 Mondrian 实例的最大线程数，默认值`100` |
| insight.mdx.mondrian.rolap.queryTimeout                      | 设置 MDX 查询的超时时间，单位秒，默认值`300`                 |
| insight.mdx.mondrian.olap.NullMemberRepresentation           | 该参数指定 MDX for Kylin 中 mdx 当结果为空时如何输出展示，默认值`#null` |
| insight.mdx.mondrian.visualtotal.solve.order                 | 该参数指定 MDX for Kylin 中 mdx 设置总计顺序，默认值`-1`     |
| insight.mdx.mondrian.cross-join.native.enable                | 该参数指定了 MDX for Kylin 下发查询时是否原生使用 Cross Join，默认值`true` |
| insight.mdx.mondrian.filter.row.limit                        | 该参数指定 MDX for Kylin 对接 Excel 时，进行 Filter 操作时，是否加入 Limit 条件，默认 `0`，表示不加入 Limit 条件。若为 `10`，将在查询中加入 limit 10 的 SQL 子句。注意：此处的参数必须要写入**正整数**，否则会引起 MDX 查询执行报错。**注意**：设置该参数后可能导致在 Excel 中对下拉列表的反选操作选择**数据不全**，默认值`0` |
| insight.mdx.mondrian.result.limit                            | 该参数用于指定检查 SQL 返回的行数，Cell 的数量。如果超限，直接返回错误。默认值`100000` |
| insight.mdx.mondrian.mdprop.mdx.subqueries                   | 该参数用于指示MDX对于子查询的支持级别，MDX默认为2，不支持子查询。如果需要MDX支持子查询，可以改成31，该功能为Beta版。 |
| insight.mdx.mondrian.query.sql.model-priority-hint.enable    | 该参数用于指定是否在 SQL 语句中指定优先用于回答查询的模型，启用后，当前查询的数据集引用的模型会被指定为查询的高优先级模型。 默认值为`false`。注意指定优先模型只会在查询的度量属于同一模型时生效。 |
| insight.mdx.xmla.support-rowset-visibilities                 | 该参数用于指定是否启用对于 XML/A Schema 中的 `HIERARCHY_VISIBILITY` 和 `MEASURE_VISIBILITY` 限制条件的支持，默认值为`false`。启用该参数后，原有的包含透视表的 Excel 文件需要经过修复才能正常使用。 |
| insight.mdx.mondrian.query.limit                             | 该参数是系统支持的最大查询数量，默认1000。
