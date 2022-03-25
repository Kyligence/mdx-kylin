## Metadata import and export

All metadata and query history of MDX instances are stored in the database, so backing up and restoring the database is a very important part of the daily operation and maintenance.

MDX currently supports MySQL and PostgreSQL as metadata storage. Among them, MySQL recommends 5.7 and later versions, and PostgreSQL recommends 10.1 and later versions.

---

### Metadata export

#### MySQL

+ MySQL is exported through the mysqldump command. For security reasons, it is recommended to leave the -P option blank and enter the password in subsequent interactive input.

  ```shell
  mysqldump -h[insight.database.ip] -P[insight.database.port] -u[insight.database.username] -p[password] [insight.database.name] > backup.sql
  ```

  - Example:
  
  ```shell
  mysqldump -hlocalhost -P3306 -uroot -proot insight > backup.sql
  ```

#### PostgreSQL

+ PostgreSQL is exported through the pg_dump command. Pay attention to entering the password in the interactive input that follows.

  ```shell
  pg_dump -h [insight.database.ip] -p [insight.database.port] -U [insight.database.username] -d [insight.database.name] -W -F c -b -v -f backup.bak
  ```

---

### Metadata import

#### MySQL

+ MySQL uses the mysql command to import the metadata that has been backed up.

  ```shell
  mysql -h[insight.database.ip] -P[insight.database.port] -u[insight.database.username] -p[password] [insight.database.name] < backup.sql
  ```

+ If the specified database does not exist, first create the corresponding database through the MySQL client tool, or the command line.

  ```sql
  create database [insight.database.name];
  ```

#### PostgreSQL

+ PostgreSQL uses the pg_restore command to import the backup files.

  ```shell
  pg_restore -h [insight.database.ip] -p [insight.database.port] -U [insight.database.username] -W -d [insight.database.name] -v backup.bak
  ```

+ If the specified database does not exist, create the database through the createdb command first.

  ```shell
  createdb [insight.database.name]
  ```

---

### Appendix: Data Sheet Description

| Table                    | Features                            |
|--------------------------|-------------------------------------|
| calculate_measure        | Calculated measure definition       |
| common_dim_relation      | Dimension relationship definition   |
| custom_hierarchy         | User-defined hierarchy              |
| dataset                  | Dataset                             |
| dim_table_model_rel      | Model relationship definition       |
| mdx_info                 | MDX information                     |
| mdx_query                | MDX query history                   |
| measure_group            | Measure group definition            |
| named_dim_col            | Dimension column definition         |
| named_dim_table          | Dimension table definition          |
| named_measure            | Measure definition                  |
| named_set                | Named set definition                |
| role_info                | Dataset role information            |
| sql_query                | SQL query history                   |
| user_info                | User information                    |

