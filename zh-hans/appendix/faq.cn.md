##  常见问题

以下收集了用户在学习使用 MDX for Kylin 过程中经常遇到的一些问题。

**Q：关于 MDX for Kylin 查询缓存的相关说明**

A：MDX for Kylin 在如下情况下将清除缓存：

- MDX for Kylin 会主动的探测 Kylin 的 Segment 变化。在发现变化后，MDX for Kylin 会主动清除缓存。

- dataset 元数据变更，任何修改 dataset 的操作，都会导致当前 dataset 的查询缓存失效；

- 主动调用清除 MDX 查询缓存接口，[详见手册](../rest/query.cn.md)

  具体命令如下：

  ```
  curl -X GET \
  'http://host:port/mdx/xmla/clearCache' \
  -H 'Authorization: Basic QURNSU46S1lMSU4='
  ```
  > 注意：该接口调用后会刷新 MDX 所有项目的查询缓存；

- 查询缓存 12 小时后自动失效。失效时间可通过 `$MDX_HOME/conf/insight.properties` 中的参数 `insight.mdx.mondrian.cache.expire-minute` 调整，当前默认值 12 小时。
