## 元数据导入和导出

MDX 实例所有元数据和查询历史等信息都存储在数据库中，因此备份与恢复数据库是日常运维和系统升级过程中一个非常重要的环节。

MDX 目前支持 MySQL 和 PostgreSQL 作为元数据存储。其中 MySQL 推荐 5.7 及以后版本，PostgreSQL 推荐 10.1 及以后版本。

---

### 元数据导出

#### MySQL

+ MySQL 通过 mysqldump 命令导出的。出于安全原因，建议将 -P 选项留空，并在随后的交互式输入中输入密码。

  ```shell
  mysqldump -h[insight.database.ip] -P[insight.database.port] -u[insight.database.username] -p[password] [insight.database.name] > backup.sql
  ```

  - 示例：
  
  ```shell
  mysqldump -hlocalhost -P3306 -uroot -proot insight > backup.sql
  ```

#### PostgreSQL

+ PostgreSQL 通过 pg_dump 命令导出，注意在随后的交互式输入中输入密码。

  ```shell
  pg_dump -h [insight.database.ip] -p [insight.database.port] -U [insight.database.username] -d [insight.database.name] -W -F c -b -v -f backup.bak
  ```

---

### 元数据导入

#### MySQL

+ MySQL 通过 mysql 命令导入已备份的元数据。

  ```shell
  mysql -h[insight.database.ip] -P[insight.database.port] -u[insight.database.username] -p[password] [insight.database.name] < backup.sql
  ```

+ 如果指定的数据库不存在，请先通过 MySQL 客户端工具或者命令行创建对应的数据库。

  ```sql
  create database [insight.database.name];
  ```

#### PostgreSQL

+ PostgreSQL 通过 pg_restore 命令导入备份的文件。

  ```shell
  pg_restore -h [insight.database.ip] -p [insight.database.port] -U [insight.database.username] -W -d [insight.database.name] -v backup.bak
  ```

+ 如果指定的数据库不存在，请先通过 createdb 命令创建数据库。

  ```shell
  createdb [insight.database.name]
  ```

---

### 附录：数据表说明

| 数据表                   | 功能                     |
|--------------------------|--------------------------|
| calculate_measure        | 计算度量定义             |
| common_dim_relation      | 维度关系定义             |
| custom_hierarchy         | 用户定义的层次           |
| dataset                  | 数据集                   |
| dim_table_model_rel      | 模型关系定义             |
| mdx_info                 | MDX 信息                 |
| mdx_query                | MDX 查询历史             |
| measure_group            | 度量组定义               |
| named_dim_col            | 维度列定义               |
| named_dim_table          | 维度表定义               |
| named_measure            | 度量定义                 |
| named_set                | 命名集定义               |
| role_info                | 数据集角色信息           |
| sql_query                | SQL 查询历史             |
| user_info                | 用户信息                 |

