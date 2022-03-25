## Dataset Role API

For the operations related to the role of the dataset, you can use the related APIs described in this section.

Related operations include:

- [Add roles](#add-roles)
- [Update roles](#update-roles)
- [Query roles](#query-roles)
- [Delete roles](#delete-roles)
- [Add a single user to an existing role](#add-a-single-user-to-an-existing-role)
- [Deleting a single user from an existing role](#deleting-a-single-user-from-an-existing-role)
- [Querying the Role List](#querying-the-role-list)


### Add roles

This API is used to add dataset roles and add users to it.

- `POST http://host:port/api/role`
- Basic Auth
- `Content-Type`: `application/json`
- The HTTP Body is `json` and the content is as follows:

```json
{
	"name": "[role name]",
	"contains": [
			{
				"type": "user",
				"name": "[user name 1]"
			},
        	{
				"type": "user",
				"name": "[user name 2]"
			}
	],
	"description": "[role description]"
}
```

- Curl example is as follows:

```sh
curl -X POST \
'http://host:port/api/role' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type:application/json;charset=utf8' \
-d '{"name":"role_test","contains":[{"type":"user","name":"ADMIN"}],"description":"test"}'
```

> Command explanation: Invoke the interface and add the role of the dataset containing the ADMIN user: role_test

- returned messages

```sh
{
    "status": 0,
    "data": {
        "role_id": 2
    }
}
```

> Explanation of the returned information: status is 0 for success, role_id is the role id.

### Update roles

This API is used to batch update user information in the dataset

- `PUT http://host:port/api/role/{roleId}`
- URL Path Variable
	- `roleId`: The value is role id
- Basic Auth
- `Content-Type`: `application/json`
- The HTTP Body is `json` and the content is as follows:

```json
{
    "name": "[role name]",
    "contains": [
        {
            "type": "user",
            "name": "[user name 1]"
        },
        {
            "type": "user",
            "name": "[user name 2]"
        }
    ],
    "description": "[role description]"
}
```

- Curl example is as follows:

```sh
curl -X PUT \
'http://host:port/api/role/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type:application/json;charset=utf8' \
-d '{"name":"role_test","contains":[{"type":"user","name":"ADMIN"}],"description":"test_update"}'
```

> Command explanation: Call the interface to fully update the role and description of the dataset named role_test.

- returned messages

```sh
{
    "status": 0,
    "data": {
        "role_id": 2
    }
}
```

> Explanation of returned information: status is 0 for success, role_id is role id.

### Query roles

- `GET http://host:port/api/role/ {roleId}`
- URL Path Variable
	- `roleId`: The value is role id
- Basic Auth
- `Content-Type`: `application/json`
- Curl example is as follows:

```sh
curl -X GET \
'http://host:port/api/role/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

> Command explanation: Call the interface to query the dataset role information with role id 2.

- returned messages

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

> Explanation of returned information: status is 0 for success, and data contains role information for the dataset.

### Delete roles

- `DELETE http://host:port/api/role/{roleId}`
- URL Path Variable
	- `roleId`: The value is role id
- Basic Auth
- `Content-Type`: `application/json`
- Curl example is as follows:

```sh
curl -X DELETE \
'http://host:port/api/role/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

> Command explanation: Call the interface to delete the role information of the dataset with role id 2.

- returned messages

```sh
{
    "status": 0,
    "data": "success"
}
```

> Explanation of the returned information: status is 0 for success, and data contains deletion success information.

### Add a single user to an existing role

- `POST http://host:port/api/role/user/visibility/{roleId}`
- URL Path Variable
	- `roleId`: The value is role id
- Basic Auth
- `Content-Type`: `application/json`
- The HTTP Body is `json` and the content is as follows:

```json
{
    "name": "[role name]",
    "contains": [
        {
            "type": "user",
            "name": "[user name]"
        }
    ],
    "description": ""
}
```

- Curl example is as follows:

```sh
curl -X POST \
'http://host:port/api/role/user/visibility/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type:application/json;charset=utf8' \
-d '{"name":"role_test","contains":[{"type":"user","name":"ANALYST"}],"description":""}'
```

> Command explanation: Call the interface and add a user ANALYST to the existing dataset role role_test.

- returned messages

```sh
{
    "status": 0,
    "data": "success"
}
```

> Explanation of the returned information: status is 0 for success, and data contains deletion success information.

### Deleting a single user from an existing role

- `DELETE http://host:port/api/role/user/visibility/{roleId}`
- URL Path Variable
	- `roleId`: The value is role id
- Basic Auth
- `Content-Type`: `application/json`
- Curl example is as follows:

```sh
curl -X DELETE \
'http://host:port/api/role/user/visibility/2' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type:application/json;charset=utf8' \
-d '{"name":"role_test","contains":[{"type":"user","name":"ANALYST"}],"description":""}'
```

> Command explanation: Call the interface to remove a user ANALYST from the existing dataset role role_test.

- returned messages

```sh
{
	"status": 0,
	"data": "success"
}
```
> Explanation of the returned information: status is 0 for success, and data contains deletion success information.

### Querying the Role List

- `GET http://host:port/api/roles`
- Basic Auth
- `Content-Type`:`application/json`

URL Parameters
- `pageNum`- required` Integer`, the number of pages, the minimum is 0
- `pageSize`- required` Integer`, size per page
- `containsDesc`- Mandatory` Boolean`, whether to include role description
- `RoleName`- Optional` String`, fuzzy query string, not selected as normal full query, selected will perform fuzzy query based on the corresponding string

- Curl example is as follows:

```sh
curl -X GET \
'http://host:port/api/roles?pageNum=0&pageSize=500&containsDesc=true' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' 
```

> Command explanation: Call the interface and return the data set role list for the specified page.

- returned messages

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

> Explanation of returned information: status is 0 for success, and data contains data set role list information.
