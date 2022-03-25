## 数据集角色 API

对于有对数据集角色进行相关操作的需求，可以使用本节介绍的相关 API 进行操作。

相关操作包含：

- [新增角色](#新增角色)
- [更新角色](#更新角色)
- [查询角色](#查询角色)
- [删除角色](#删除角色)
- [向已有角色增加单个用户](#向已有角色增加单个用户)
- [从已有角色删除单个用户](#从已有角色删除单个用户)
- [查询角色列表](#查询角色列表)

### 新增角色

该 API 可以用于新增角色，并且直接向其中添加用户

- `POST  http://host:port/api/role`
- Basic Auth 认证
- `Content-Type`: `application/json`
- HTTP Body 是 `json`，内容如下：

```json
{
    "name": "[角色名]",
    "contains": [
        {
            "type": "user",
            "name": "[用户名1]"
        },
        {
            "type": "user",
            "name": "[用户名2]"
        }
    ],
    "description": "[角色描述]"
}
```

- Curl 示例如下：

```sh
curl -X POST \
'http://host:port/api/role' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type:application/json;charset=utf8' \
-d '{"name":"role_test","contains":[{"type":"user","name":"ADMIN"}],"description":"test"}'
```

> 命令解释：调用接口，新增包含ADMIN用户的数据集角色：role_test。

- 返回信息

```sh
{
	"status": 0,
	"data": {
		"role_id": 2
	}
}
```

> 返回信息解释：status 为 0 表示成功, role_id 为角色 id。

### 更新角色

该 API 可以用于角色中的用户

- `PUT http://host:port/api/role/{roleId}`
- URL Path Variable
	- `roleId`: 取值为角色 id
- Basic Auth 认证
- `Content-Type`: `application/json`
- HTTP Body 是 `json`，内容如下：

```json
{
    "name": "[角色名]",
    "contains": [
        {
            "type": "user",
            "name": "[用户名1]"
        },
        {
            "type": "user",
            "name": "[用户名1]"
        }
    ],
    "description": "[角色描述]"
}
```

- Curl 示例如下：

```sh
curl -X PUT \
'http://host:port/api/role/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type:application/json;charset=utf8' \
-d '{"name":"role_test","contains":[{"type":"user","name":"ADMIN"}],"description":"test_update"}'
```

> 命令解释：调用接口，全量更新名称为 role_test 的数据集角色及描述。

- 返回信息

```sh
{
	"status": 0,
	"data": {
		"role_id": 2
	}
}
```

> 返回信息解释：status 为 0 表示成功，role_id 为角色 id。

### 查询角色

- `GET http://host:port/api/role/{roleId}`
- URL Path Variable
  - `roleId`: 取值为角色 id
- Basic Auth认证
- `Content-Type`: `application/json`
- Curl 示例如下：

```sh
curl -X GET \
'http://host:port/api/role/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

> 命令解释：调用接口，查询角色 id 为 2 的数据集角色信息。

- 返回信息

```sh
{
	"status": 0,
	"data": {
        "name": "role_test",
        "description": "test_update",
        "contains": [
            {
                "type": "user",
                "name": "ADMIN"
            }
        ]
    }
}
```

> 返回信息解释：status为0表示成功，data包含数据集角色信息。

### 删除角色

- `DELETE http://host:port/api/role/{roleId}`
- URL Path Variable
  - `roleId`: 取值为角色id
- Basic Auth认证
- `Content-Type`: `application/json`
- Curl 示例如下：

```sh
curl -X DELETE \
'http://host:port/api/role/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

> 命令解释：调用接口，删除角色id为2的数据集角色信息。

- 返回信息

```sh
{
  "status" : 0,
  "data" : "success"
}
```

> 返回信息解释：status为0表示成功，data包含删除成功信息。

### 向已有角色增加单个用户

- `POST http://host:port/api/role/user/visibility/{roleId}`
- URL Path Variable 
	- `roleId`: 取值为角色 id
- Basic Auth认证
- `Content-Type`: `application/json`
- HTTP Body 是 `json`，内容如下：

```json
{
    "name": "[角色名]",
    "contains": [
        {
            "type": "user",
            "name": "[用户名]"
        }
    ],
    "description": ""
}
```

- Curl 示例如下：

```sh
curl -X POST \
'http://host:port/api/role/user/visibility/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type:application/json;charset=utf8' \
-d '{"name":"role_test","contains":[{"type":"user","name":"ANALYST"}],"description":""}'
```

> 命令解释：调用接口，向已有数据集角色 role_test 中新增一个用户 ANALYST 。

- 返回信息

```sh
{
  "status" : 0,
  "data" : "success"
}
```

> 返回信息解释：status 为 0 表示成功，data 包含删除成功信息。

### 从已有角色删除单个用户

- `DELETE http://host:port/api/role/user/visibility/{roleId}`
- URL Path Variable
  - `roleId`: 取值为角色 id
- Basic Auth认证
- `Content-Type`: `application/json`
- Curl 示例如下：

```sh
curl -X DELETE \
'http://host:port/api/role/user/visibility/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type:application/json;charset=utf8' \
-d '{"name":"role_test","contains":[{"type":"user","name":"ANALYST"}],"description":""}'
```

> 命令解释：调用接口，从已有数据集角色 role_test 中移除一个用户 ANALYST 。

- 返回信息

```sh
{
  "status" : 0,
  "data" : "success"
}
```
> 返回信息解释：status 为 0 表示成功，data 包含删除成功信息。

### 查询角色列表

- `GET http://host:port/api/roles`
- Basic Auth认证
- `Content-Type`: `application/json`

URL Parameters
- `pageNum`- 必选 `Integer`, 页数，最小为 0
- `pageSize`- 必选  `Integer`, 每页大小
- `containsDesc`- 必选  `Boolean` , 是否包含角色描述
- `RoleName`- 可选 `String` , 模糊查询字符串，不选为正常全查询，选定则会根据对应字符串进行模糊查询

- Curl 示例如下：

```sh
curl -X GET \
'http://host:port/api/roles?pageNum=0&pageSize=500&containsDesc=true' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

> 命令解释：调用接口，返回指定分页的数据集角色列表。

- 返回信息

```sh
{
    "status": 0,
    "data": {
        "pageNum": 0,
        "pageSize": 500,
        "list": [
            {
                "id": 3,
                "name": "role_test",
                "description": "test_update"
            }, {
                "id": 1,
                "name": "Admin",
                "description": "This role is an admin role with all semantic information access to all datasets"
            }
        ],
        "total": 2
    }
}
```
> 返回信息解释：status 为 0 表示成功，data 包含数据集角色列表信息。
