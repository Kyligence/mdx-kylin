

CREATE TABLE  if not exists "calculate_measure" (
   "id" serial4 ,
   "dataset_id"   int NOT NULL,
   "name"   varchar(300) NOT NULL,
   "expression"   varchar(5000) NOT NULL,
   "format"   varchar(150) NOT NULL,
   "format_type" varchar(50) DEFAULT '',
   "measure_folder"   varchar(200) NOT NULL DEFAULT '' ,
   "extend" varchar(3000) NOT NULL default '',
   "visible_flag"  BOOLEAN not null DEFAULT true ,
   "translation"  varchar(500),
   "subfolder"  varchar(100),
   "non_empty_behavior" varchar(3000) not null DEFAULT '[]',
   primary key ("id")
)    ;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40000 ALTER TABLE calculate_measure DISABLE KEYS */;


--
-- Table structure for table common_dim_relation
--

CREATE TABLE  if not exists "common_dim_relation" (
   "id" serial4 ,
   "dataset_id"   int NOT NULL,
   "model"   varchar(100) NOT NULL,
   "model_related"   varchar(100) NOT NULL,
   "relation"   varchar(2000) NOT NULL ,
   primary key ("id"),
 unique ("dataset_id", "model", "model_related")
)    ;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40000 ALTER TABLE common_dim_relation DISABLE KEYS */;


--
-- Table structure for table custom_hierarchy
--

CREATE TABLE  if not exists "custom_hierarchy" (
   "id" serial4 ,
   "dataset_id"   int NOT NULL,
   "model"   varchar(100) NOT NULL,
   "dim_table"   varchar(300) NOT NULL ,
   "name"   varchar(300) NOT NULL,
   "dim_col"   varchar(300) NOT NULL,
   "description"   varchar(1000) NOT NULL DEFAULT '',
   "translation"  varchar(500),
   "weight_col" varchar(300),
   primary key ("id")
)    ;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40000 ALTER TABLE custom_hierarchy DISABLE KEYS */;


--
-- Table structure for table dataset
--

CREATE TABLE  if not exists "dataset" (
   "id" serial4 ,
   "project"   varchar(100) NOT NULL,
   "dataset"   varchar(100) NOT NULL,
   "type"   int NOT NULL ,
   "canvas" varchar(5000) NOT NULL DEFAULT '',
   "front_v" varchar(20) NOT NULL DEFAULT 'v0.1',
   "status" varchar(10) NOT NULL DEFAULT 'NORMAL',
   "broken_msg" text,
   "create_user"   varchar(255) NOT NULL,
   "create_time"   bigint NOT NULL DEFAULT 0,
   "modify_time"   bigint NOT NULL DEFAULT 0,
   "extend" varchar(5000) NOT NULL default '',
   "translation_types" varchar(100),
   "access" int DEFAULT 0,
   primary key ("id"),
   unique ("project", "dataset", "type")
)    ;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40000 ALTER TABLE dataset DISABLE KEYS */;


--
-- Table structure for table dim_table_model_rel
--

CREATE TABLE  if not exists "dim_table_model_rel" (
   "id" serial4 ,
   "dataset_id"   int NOT NULL,
   "model"   varchar(100) NOT NULL,
   "dim_table"   varchar(300) NOT NULL,
   "relation"   int NOT NULL ,
   "intermediate_dim_table"   varchar(300) NOT NULL DEFAULT '' ,
   "primary_dim_col"   varchar(300) NOT NULL DEFAULT '' ,
   primary key ("id")
)    ;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40000 ALTER TABLE dim_table_model_rel DISABLE KEYS */;


--
-- Table structure for table measure_group
--

CREATE TABLE if not exists "measure_group" (
   "id" serial4 ,
   "dataset_id"   int NOT NULL,
   "name"   varchar(300) NOT NULL,
   "dim_table"   varchar(300) NOT NULL DEFAULT '',
   "calculate_measure"   varchar(300) NOT NULL DEFAULT '',
   primary key ("id")
)   ;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40000 ALTER TABLE measure_group DISABLE KEYS */;
/*!40000 ALTER TABLE measure_group ENABLE KEYS */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;


--
-- Table structure for table named_dim_col
--

CREATE TABLE if not exists "named_dim_col" (
   "id" serial4 ,
   "dataset_id"   int NOT NULL,
   "model"   varchar(100) NOT NULL,
   "dim_table"   varchar(300) NOT NULL ,
   "dim_col"   varchar(300) NOT NULL,
   "dim_col_alias"   varchar(300) NOT NULL,
   "col_type"   int NOT NULL DEFAULT '0' ,
   "data_type" varchar(50) NOT NULL DEFAULT '',
   "extend" varchar(3000) NOT NULL default '',
   "visible_flag"  BOOLEAN not null DEFAULT true ,
   "name_column"  varchar(300),
   "value_column"  varchar(300),
   "translation"  varchar(500),
   "subfolder"  varchar(100),
   "default_member" varchar(1000),
   primary key ("id")
)    ;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40000 ALTER TABLE named_dim_col DISABLE KEYS */;


--
-- Table structure for table named_dim_table
--

CREATE TABLE if not exists "named_dim_table" (
   "id" serial4 ,
   "dataset_id"   int NOT NULL,
   "model"   varchar(100) NOT NULL,
   "dim_table"   varchar(300) NOT NULL ,
   "dim_table_alias"   varchar(300) NOT NULL DEFAULT '',
   "dim_table_type"   varchar(100) NOT NULL DEFAULT 'regular' ,
   "actual_table" varchar(300) NOT NULL DEFAULT '',
   "fact_table" varchar(300) NOT NULL DEFAULT '',
   "translation"  varchar(500),
   primary key ("id")
)    ;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40000 ALTER TABLE named_dim_table DISABLE KEYS */;


--
-- Table structure for table named_measure
--

CREATE TABLE if not exists "named_measure" (
   "id" serial4 ,
   "dataset_id"   int NOT NULL,
   "model"   varchar(100) NOT NULL,
   "name"   varchar(300) NOT NULL,
   "alias"   varchar(300) NOT NULL ,
   "expression" varchar(200) NOT NULL default '',
   "data_type" varchar(50) NOT NULL default '',
   "dim_column" varchar(300) NOT NULL default '',
   "extend" varchar(3000) NOT NULL default '',
   "visible_flag"  BOOLEAN not null DEFAULT true ,
   "translation"  varchar(500),
   "format" varchar(150),
   "format_type" varchar(50) DEFAULT '',
   "subfolder"  varchar(100),
   primary key ("id")
)    ;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40000 ALTER TABLE named_measure DISABLE KEYS */;


CREATE TABLE if not exists "user_info" (
  "id" serial4 ,
  "username" varchar(256) NOT NULL,
  "password" varchar(4096) NOT NULL,
  "active" int NOT NULL,
  "license_auth" int NOT NULL,
  "login_count" int NOT NULL DEFAULT 0,
  "last_login" bigint NOT NULL DEFAULT 0,
  "create_time" bigint NOT NULL DEFAULT 0,
  "conf_usr" int,
  PRIMARY KEY ("id"),
  unique  ("username")
) ;

CREATE TABLE  if not exists "named_set" (
   "id" serial4 ,
   "dataset_id"   int NOT NULL,
   "name"   varchar(300) NOT NULL,
   "expression"   varchar(5000) NOT NULL,
   "folder"   varchar(200) NOT NULL DEFAULT '' ,
   "location" varchar(300) NOT NULL DEFAULT '',
   "extend" varchar(3000) NOT NULL default '',
   "visible_flag"  BOOLEAN not null DEFAULT true ,
   "translation"  varchar(500),
   primary key ("id")
) ;

CREATE TABLE  if not exists "role_info" (
   "id" serial4 ,
   "name"   varchar(20) NOT NULL,
   "extend"   varchar(20000) NOT NULL DEFAULT '' ,
   "description" varchar(500) NOT NULL DEFAULT '',
   "create_time" bigint NOT NULL DEFAULT 0,
   "modify_time" bigint NOT NULL DEFAULT 0,
   primary key ("id")
) ;


-- ----------------------------
--  Table structure for "mdx_query"
-- ----------------------------
CREATE TABLE if not exists "mdx_query"   (
  "id" serial4,
  "mdx_query_id" varchar(36) NOT NULL,
  "mdx_text" text,
  "start" bigint,
  "total_execution_time" bigint,
  "username" varchar(255) NOT NULL,
  "success" BOOLEAN not null DEFAULT true ,
  "project" varchar(255) NOT NULL,
  "application" varchar(64) NOT NULL,
  "mdx_cache_used" BOOLEAN not null DEFAULT true ,
  "before_connection" bigint,
  "connection" bigint,
  "hierarchy_load" bigint,
  "olaplayout_construction" bigint,
  "aggregationqueries_construction" bigint,
  "aggregationqueries_execution" bigint,
  "otherresult_construction" bigint,
  "network_package" int,
  "timeout" BOOLEAN not null DEFAULT true ,
  "message" text,
  "calculate_axes" bigint,
  "calculate_cell" bigint,
  "calculate_cellrequest_num" bigint,
  "create_rolapresult" bigint,
  "create_multidimensional_dataset" bigint,
  "marshall_soap_message" bigint,
  "dataset_name" varchar(255) NOT NULL,
  "gateway" BOOLEAN not null DEFAULT true ,
  "other_used" BOOLEAN not null DEFAULT true ,
  "node" varchar(64),
  "reserved_field_1" varchar(2000),
  "reserved_field_2" varchar(2000),
  "reserved_field_3" varchar(5000),
  "reserved_field_4" varchar(5000),
  "reserved_field_5" text,
  PRIMARY KEY ("id"),
  unique  ("mdx_query_id")
) ;

-- ----------------------------
--  Table structure for "sql_query"
-- ----------------------------
CREATE TABLE if not exists "sql_query"   (
  "id" serial4,
  "mdx_query_id" varchar(36) NOT NULL,
  "sql_text" text,
  "sql_execution_time" bigint,
  "sql_cache_used" BOOLEAN not null DEFAULT true ,
  "exec_status" BOOLEAN NOT NULL default true,
  "ke_query_id" varchar(100),
  "reserved_field_1" varchar(2000),
  "reserved_field_2" varchar(2000),
  "reserved_field_3" varchar(5000),
  "reserved_field_4" varchar(5000),
  "reserved_field_5" text,
  PRIMARY KEY ("id"),
  CONSTRAINT "sql_mdx_query_id" FOREIGN KEY ("mdx_query_id") REFERENCES "mdx_query" ("mdx_query_id") ON DELETE CASCADE
) ;

-- ----------------------------
--  Table structure for "mdx_info"
-- ----------------------------
CREATE TABLE if not exists "mdx_info" (
   "id" serial4,
   "mdx_version" varchar(50),
   "create_time" bigint NOT NULL DEFAULT 0,
   "modify_time" bigint NOT NULL DEFAULT 0,
   PRIMARY KEY ("id")
) ;

CREATE INDEX if not exists "named_set_dataset_id_idx" ON "named_set" USING btree ("dataset_id");
CREATE INDEX if not exists "calculate_measure_dataset_id_idx" ON "calculate_measure" USING btree ("dataset_id");
CREATE INDEX if not exists "common_dim_relation_dataset_id_idx" ON "common_dim_relation" USING btree ("dataset_id");
CREATE INDEX if not exists "custom_hierarchy_dataset_id_idx" ON "custom_hierarchy" USING btree ("dataset_id");
CREATE INDEX if not exists "dim_table_model_rel_dataset_id_idx" ON "dim_table_model_rel" USING btree ("dataset_id");
CREATE INDEX if not exists "measure_group_dataset_id_idx" ON "measure_group" USING btree ("dataset_id");
CREATE INDEX if not exists "named_dim_col_dataset_id_idx" ON "named_dim_col" USING btree ("dataset_id");
CREATE INDEX if not exists "named_dim_table_dataset_id_idx" ON "named_dim_table" USING btree ("dataset_id");
CREATE INDEX if not exists "named_measure_dataset_id_idx" ON "named_measure" USING btree ("dataset_id");
CREATE INDEX if not exists "role_info_name_idx" ON "role_info" USING btree ("name");
CREATE INDEX if not exists "mdx_query_id_idx" ON "mdx_query" USING btree ("mdx_query_id");
CREATE INDEX if not exists "project_key" ON "mdx_query" USING btree ("project");
CREATE INDEX if not exists "start_key" ON "mdx_query" USING btree ("start");
CREATE INDEX if not exists "success_key" ON "mdx_query" USING btree ("success");
CREATE INDEX if not exists "total_execution_time_key" ON "mdx_query" USING btree ("total_execution_time");
CREATE INDEX if not exists "dataset_name_key" ON "mdx_query" USING btree ("dataset_name");
CREATE INDEX if not exists "node_key" ON "mdx_query" USING btree ("node");
CREATE INDEX if not exists "sql_query_id_idx" ON "sql_query" USING btree ("mdx_query_id");

INSERT INTO "mdx_info" VALUES (1, '', 0, 0) on conflict do nothing;

INSERT INTO "role_info" VALUES (1, 'Admin', '{"contains":[{"name":"ADMIN","type":"user"}]}', 'This role is an admin role with all semantic information access to all datasets', 0, 0) on conflict do nothing;

select setval('role_info_id_seq',(select max(id) from role_info));
