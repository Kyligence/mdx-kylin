## 数据集 API

本节介绍数据集相关的接口调用。

- [导出数据集](#导出数据集)
- [导入数据集](#导入数据集)
- [更新数据集](#更新数据集)
- [获取数据集列表](#获取数据集列表)



### 导出数据集

`GET  http://<host>:<port>/api/dataset`

请求参数：

- project : 项目名

- datasetType : 数据集类型

- datasetName : 数据集名称

请求 Header:

+ Basic Auth 认证

+ `Content-Type`: `application/json`

Curl 示例如下：

```sh
curl -X GET \
'http://localhost:7080/api/dataset?project=learn_kylin&datasetType=mdx&datasetName=test' \
-H 'Authorization: Basic QURNSU46S1lMSU4=' \
-H 'Content-Type: application/json;charset=utf8' \
-o export_dataset.json
```

> 命令解释：调用接口，将相应的 json 数据存入到当前目录的export_dataset.json 中



### 导入数据集

`POST http://<host>:<port>/api/dataset?createType=import`

- Basic Auth认证

- `Content-Type`: `application/json`

- `createType`:`import` 固定值

- HTTP Body 是 `dataset` 的 `json` 文件

- Curl示例如下：

  ```sh
  curl -X POST \
  'http://localhost:7080/api/dataset?createType=import' \
  -H 'Authorization: Basic QURNSU46S1lMSU4=' \
  -H 'Content-Type: application/json;charset=utf8' \
  -d "@export_dataset.json"
  ```

  > 命令解释：调用接口导入数据集，json 为当前目录下的export_dataset.json 文件



### 更新数据集

`PUT http://<host>:<port>/api/dataset`

- Basic Auth认证

- `Content-Type`: `application/json`

-  HTTP Body 是 `dataset` 的 `json` 文件

- Curl示例如下：

  ```sh
  curl -X PUT \
  'http://localhost:7080/api/dataset' \
  -H 'Authorization: Basic QURNSU46S1lMSU4=' \
  -H 'Content-Type: application/json;charset=utf8' \
  -d "@export_dataset.json"
  ```

  > 命令解释：调用接口导入数据集，json 为当前目录下的export_dataset.json 文件



### 获取数据集列表

`GET  http://<host>:<port>/api/datasets/MDX/<project>`

- Basic Auth认证

- `Content-Type`: `application/json`

- Curl示例如下：

  ```sh
  curl -X GET \
  'http://localhost:7080/api/datasets/MDX/learn_kylin' \
  -H 'Authorization: Basic QURNSU46S1lMSU4=' \
  -H 'Content-Type: application/json;charset=utf8' \
  -o dataset.json
  ```
