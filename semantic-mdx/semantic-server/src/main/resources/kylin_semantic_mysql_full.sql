SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `calculate_measure`
-- ----------------------------
CREATE TABLE if not exists `calculate_measure`   (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `name` varchar(300) NOT NULL,
  `expression` varchar(5000) NOT NULL,
  `format` varchar(150) NOT NULL,
  `format_type` varchar(50) DEFAULT '' COMMENT 'format type, such as currency',
  `measure_folder` varchar(200) NOT NULL DEFAULT '' COMMENT 'the folder which CM belongs to',
  `extend` varchar(3000) NOT NULL default '',
  `visible_flag` tinyint(1) NOT NULL default 1,
  `translation` varchar(500),
  `subfolder` varchar(100),
  `non_empty_behavior` varchar(3000) NOT NULL DEFAULT '[]',
  PRIMARY KEY (`id`),
  KEY `dataset_id_key` (`dataset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='calculation measure table';

-- ----------------------------
--  Table structure for `common_dim_relation`
-- ----------------------------
CREATE TABLE if not exists `common_dim_relation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `model_related` varchar(100) NOT NULL,
  `relation` varchar(2000) NOT NULL COMMENT 'the relation between two models, like dim_t1=dim_t1,dim_t2=dim_t2',
  PRIMARY KEY (`id`),
  KEY `dataset_id_key` (`dataset_id`),
  UNIQUE KEY `unique_pk` (`dataset_id`,`model`,`model_related`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='common dimension among models';

-- ----------------------------
--  Table structure for `custom_hierarchy`
-- ----------------------------
CREATE TABLE if not exists `custom_hierarchy` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `dim_table` varchar(300) NOT NULL COMMENT 'such as：DEFAULT.KYLIN_SALES',
  `name` varchar(300) NOT NULL,
  `dim_col` varchar(300) NOT NULL,
  `description` varchar(1000) NOT NULL DEFAULT '',
  `translation` varchar(500),
  `weight_col` varchar(300) NULL,
  PRIMARY KEY (`id`),
  KEY `dataset_id_key` (`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='custom hierarchy table';

-- ----------------------------
--  Table structure for `dataset`
-- ----------------------------
CREATE TABLE if not exists `dataset` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project` varchar(100) NOT NULL,
  `dataset` varchar(100) NOT NULL,
  `status` varchar(10) NOT NULL DEFAULT 'NORMAL' COMMENT 'dataset status: NORMAL | BROKEN',
  `broken_msg` text COMMENT 'the broken detail info when dataset is broken',
  `canvas` varchar(5000) NOT NULL DEFAULT '',
  `front_v` varchar(20) NOT NULL DEFAULT 'v0.1',
  `create_user` varchar(255) NOT NULL,
  `create_time` bigint NOT NULL DEFAULT 0,
  `modify_time` bigint NOT NULL DEFAULT 0,
  `extend` varchar(5000) NOT NULL DEFAULT '',
  `translation_types` varchar(100),
  `access` int(11) DEFAULT '0' COMMENT '0: allow_list, 1:block_list',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_pk` (`project`,`dataset`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='semantic-dataset';

-- ----------------------------
--  Table structure for `dim_table_model_rel`
-- ----------------------------
CREATE TABLE if not exists `dim_table_model_rel` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `dim_table` varchar(300) NOT NULL,
  `relation` int(11) NOT NULL COMMENT 'joint type，0:joint | 1:not joint | 2: many to many',
  `intermediate_dim_table` varchar(300) NOT NULL DEFAULT '' COMMENT 'when relation is 2，need to specify it',
  `primary_dim_col` varchar(300) NOT NULL DEFAULT '' COMMENT 'when relation is 2，need to specify it',
  PRIMARY KEY (`id`),
  KEY `dataset_id_key` (`dataset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='the relationship between dimension table and model';

-- ----------------------------
--  Table structure for `measure_group`
-- ----------------------------
CREATE TABLE if not exists `measure_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `name` varchar(300) NOT NULL,
  `dim_table` varchar(300) NOT NULL DEFAULT '',
  `calculate_measure` varchar(300) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `dataset_id_key` (`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='measure group info table';

-- ----------------------------
--  Table structure for `named_dim_col`
-- ----------------------------
CREATE TABLE if not exists `named_dim_col` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `dim_table` varchar(300) NOT NULL COMMENT 'KYLIN_SALES',
  `dim_col` varchar(300) NOT NULL,
  `dim_col_alias` varchar(300) NOT NULL,
  `col_type` int(11) NOT NULL DEFAULT '0' COMMENT '0:default, 1:levelYears, 2:levelQuarters, 3:levelMonths, 4:levelWeeks',
  `data_type` varchar(50) NOT NULL default '',
  `extend` varchar(3000) NOT NULL default '',
  `visible_flag` tinyint(1) NOT NULL default 1,
  `name_column` varchar(300),
  `value_column` varchar(300),
  `translation` varchar(500),
  `subfolder` varchar(100),
  `default_member` varchar(1000),
  PRIMARY KEY (`id`),
  KEY `dataset_id_key` (`dataset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='table column alias and type info';

-- ----------------------------
--  Table structure for `named_dim_table`
-- ----------------------------
CREATE TABLE if not exists `named_dim_table` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `dim_table` varchar(300) NOT NULL COMMENT 'KYLIN_SALES',
  `dim_table_alias` varchar(300) NOT NULL DEFAULT '',
  `dim_table_type` varchar(100) NOT NULL DEFAULT 'regular' COMMENT 'values: regular|time',
  `actual_table` varchar(300) NOT NULL default '',
  `fact_table` varchar(300) NOT NULL default '',
  `translation` varchar(500),
  PRIMARY KEY (`id`),
  KEY `dataset_id_key` (`dataset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='dim table alias and type info';

-- ----------------------------
--  Table structure for `named_measure`
-- ----------------------------
CREATE TABLE if not exists `named_measure` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `name` varchar(300) NOT NULL,
  `alias` varchar(300) NOT NULL COMMENT 'measure name renamed in MDX',
  `dim_column` varchar(300) NOT NULL default '',
  `data_type` varchar(50) NOT NULL default '',
  `expression` varchar(200) NOT NULL default '',
  `extend` varchar(3000) NOT NULL default '',
  `visible_flag` tinyint(1) NOT NULL default 1,
  `translation` varchar(500),
  `format` varchar(150),
  `format_type` varchar(50) DEFAULT '' COMMENT 'format type, such as currency',
  `subfolder` varchar(100),
  PRIMARY KEY (`id`),
  KEY `dataset_id_key` (`dataset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='measure alias info';

CREATE TABLE if not exists `user_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(4096) NOT NULL,
  `active` int(11) NOT NULL,
  `license_auth` int(11) NOT NULL,
  `login_count` int(11) NOT NULL DEFAULT 0,
  `last_login` bigint NOT NULL DEFAULT 0,
  `create_time` bigint NOT NULL DEFAULT 0,
  `conf_usr` int(11),
  PRIMARY KEY (`id`),
  UNIQUE KEY `username_key` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='user information';

CREATE TABLE if not exists `named_set` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `name` varchar(300) NOT NULL,
  `expression` varchar(5000) NOT NULL,
  `folder` varchar(200) NOT NULL DEFAULT '' COMMENT 'the folder which named set belongs to',
  `location` varchar(300) NOT NULL DEFAULT '' COMMENT 'the location which named set locates',
  `extend` varchar(3000) NOT NULL default '',
  `visible_flag` tinyint(1) NOT NULL default 1,
  `translation` varchar(500),
  PRIMARY KEY (`id`),
  KEY `dataset_id_key` (`dataset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='named set info';

CREATE TABLE if not exists `role_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(20) NOT NULL,
  `extend` varchar(20000) NOT NULL default '',
  `description` varchar(500) NOT NULL,
  `create_time` bigint NOT NULL DEFAULT 0,
  `modify_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `name_key` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='role info';

-- ----------------------------
--  Table structure for `MDX_QUERY`
-- ----------------------------
CREATE TABLE if not exists `mdx_query`   (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mdx_query_id` char(36) NOT NULL,
  `mdx_text` text NOT NULL,
  `start` bigint,
  `total_execution_time` bigint,
  `username` varchar(255) NOT NULL,
  `success` tinyint(1) NOT NULL default 0,
  `project` varchar(255) NOT NULL,
  `application` varchar(64) NOT NULL,
  `mdx_cache_used` tinyint(1) NOT NULL default 0,
  `before_connection` bigint,
  `connection` bigint,
  `hierarchy_load` bigint,
  `olaplayout_construction` bigint,
  `aggregationqueries_construction` bigint,
  `aggregationqueries_execution` bigint,
  `otherresult_construction` bigint,
  `network_package` int,
  `timeout` tinyint(1) NOT NULL default 0,
  `message` text,
  `calculate_axes` bigint,
  `calculate_cell` bigint,
  `calculate_cellrequest_num` bigint,
  `create_rolapresult` bigint,
  `create_multidimensional_dataset` bigint,
  `marshall_soap_message` bigint,
  `dataset_name` varchar(255) NOT NULL,
  `gateway` tinyint(1) NOT NULL default 0,
  `other_used` tinyint(1) NOT NULL default 0,
  `node` varchar(64),
  `reserved_field_1` varchar(2000),
  `reserved_field_2` varchar(2000),
  `reserved_field_3` varchar(5000),
  `reserved_field_4` varchar(5000),
  `reserved_field_5` text,

  PRIMARY KEY (`id`),
  KEY `mdx_query_id_key` (`mdx_query_id`),
  KEY `project_key` (`project`),
  KEY `start_key` (`start`),
  KEY `success_key` (`success`),
  KEY `total_execution_time_key` (`total_execution_time`),
  KEY `dataset_name_key` (`dataset_name`),
  KEY `node_key` (`node`),
  UNIQUE KEY `unique_pk` (`mdx_query_id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='MDX QUERY table';

-- ----------------------------
--  Table structure for `SQL_QUERY`
-- ----------------------------
CREATE TABLE if not exists `sql_query`   (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mdx_query_id` char(36) NOT NULL,
  `sql_text` text NOT NULL,
  `sql_execution_time` bigint,
  `sql_cache_used` tinyint(1) NOT NULL default 0,
  `exec_status` tinyint(1) NOT NULL default 1,
  `ke_query_id` varchar(100),
  `reserved_field_1` varchar(2000),
  `reserved_field_2` varchar(2000),
  `reserved_field_3` varchar(5000),
  `reserved_field_4` varchar(5000),
  `reserved_field_5` text,

  PRIMARY KEY (`id`),
  KEY `mdx_query_id_key` (`mdx_query_id`),
  CONSTRAINT `sql_mdx_query_id` FOREIGN KEY (`mdx_query_id`) REFERENCES `mdx_query` (`mdx_query_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='SQL QUERY table';

-- ----------------------------
--  Table structure for `mdx_info`
-- ----------------------------
CREATE TABLE if not exists `mdx_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mdx_version` varchar(50),
  `create_time` bigint NOT NULL DEFAULT 0,
  `modify_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COMMENT='mdx info';

INSERT IGNORE INTO `mdx_info` VALUES (1, '', 0, 0);

INSERT IGNORE INTO `role_info` VALUES (1, 'Admin', '{"contains":[{"name":"ADMIN","type":"user"}]}', 'This role is an admin role with all semantic information access to all datasets', 0, 0);

SET FOREIGN_KEY_CHECKS = 1;
