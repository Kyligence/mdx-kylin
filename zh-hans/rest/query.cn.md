## 查询相关 API

对于有对查询进行相关操作的需求，可以使用本节介绍的相关 API 进行操作。

相关操作包含：

- [清除 MDX 查询缓存](#清除-mdx-查询缓存)

### 清除 MDX 查询缓存

MDX for Kylin 会保留 MDX 查询的缓存，若要设置缓存驻留时间，详见[配置文件说明](../configuration/properties.cn.md)

- `GET  http://host:port/mdx/xmla/<项目>/clearCache`

请求参数:

- 项目名称: 用于指定清除缓存的项目

请求 Header:

- Basic Auth 认证

- `Content-Type`: `application/json`

Curl 示例如下：

```sh
curl -X GET \
'http://host:port/mdx/xmla/clearCache' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

```sh
curl -X GET \
'http://host:port/mdx/xmla/<项目>/clearCache' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

> 命令解释：调用接口，清除所有 MDX 缓存。

- 返回信息

```json
"Cache has been cleared."
```

```json
"<项目> Cache has been cleared."
```
> 返回信息解释：缓存已被清除。
