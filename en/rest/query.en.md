## Query related API

Related operations include:

- [Clear MDX Query Cache](#clear-mdx-query-cache)


### Clear MDX Query Cache

MDX for Kylin retains the cache of MDX queries. To set the cache resident time, see [Configuration file description](../configuration/properties.en.md)

- `GET http://host:port/mdx/xmla/<project>/clearCache`

Request Parameters

- project

Request Headers

- Basic Auth

- `Content-Type`: `application/json`

Curl examples are as follows:

```sh
curl -X GET \
'http://host:port/mdx/xmla/clearCache' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

```sh
curl -X GET \
'http://host:port/mdx/xmla/<project>/clearCache' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

> Command explanation: Call the interface and clear all MDX query caches.

- returned messages

```json
"<project> Cache has been cleared."
```

```json
"Cache has been cleared."
```

> Explanation of the returned message: The cache has been cleared.
