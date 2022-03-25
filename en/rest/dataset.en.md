## Dataset API


This section introduces the Rest APIs related to the dataset.

- [Export](#export)
- [Import](#import)
- [Update](#Update)
- [Fetch List](#Fetch List)



### Export

`GET http://<host>:<port>/api/dataset`

Request Parameters

- project

- datasetType

- datasetName

Request Headers

- Basic Auth authentication

- `Content-Type`: `application/json`

Curl examples are as follows:

```sh
curl -X GET \
'http://localhost:7080/api/dataset?project=learn_kylin&datasetType=mdx&datasetName=test' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type: application/json;charset=utf8' \
-o export_dataset.json
```

> Command interpretation: call the interface, save the json data into the current directory's export_dataset.json



### Import

`POST http://<host>:<port>/api/dataset?createType=import`

- Basic Auth authentication

- `Content-Type`: `application/json`

- `createType`:`import` fixed value

- HTTP Body is a `json` file of `dataset`

- Curl examples are as follows:

```sh
curl -X POST \
'http://localhost:7080/api/dataset?createType=import' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type: application/json;charset=utf8' \
-d "@export_dataset.json"
```

> Command explanation: Call the interface to import the data set, json is the export_dataset.json file in the current directory.



### Update

`PUT http://<host>:<port>/api/dataset`

- Basic Auth authentication

- `Content-Type`: `application/json`

- HTTP Body is a `json` file of `dataset`

- Curl examples are as follows:

```sh
curl -X PUT \
'http://localhost:7080/api/dataset' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type: application/json;charset=utf8' \
-d "@export_dataset.json"
```

> Command explanation: Call the interface to import the data set, json is the export_dataset.json file in the current directory.



### Fetch List

`GET  http://<host>:<port>/api/datasets/MDX/<project>`

- Basic Auth authentication

- `Content-Type`: `application/json`

- Curl examples are as follows:

  ```sh
  curl -X GET \
  'http://localhost:7080/api/datasets/MDX/learn_kylin' \
  -H 'Authorization: Basic QURNSU46S1lMSU4=' \
  -H 'Content-Type: application/json;charset=utf8' \
  -o dataset.json
  ```

