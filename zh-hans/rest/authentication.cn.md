## 安全认证 API

MDX for Kylin 所有的 API 都是基于 [Basic Authentication](https://en.wikipedia.org/wiki/Basic_access_authentication) 认证机制。Basic Authentication 是一种简单的访问控制机制，将帐号密码基于 Base64 编码后作为请求头添加到 HTTP 请求头中，后端会读取请求头中的帐号密码信息进行认证。以 MDX for Kylin 默认的账户密码 `ADMIN:KYLIN` 为例，对应帐号密码编码后结果为 `Basic QURNSU46S1lMSU4=`，那么 HTTP 对应的头信息为 `Authorization: Basic QURNSU46S1lMSU4=`。
