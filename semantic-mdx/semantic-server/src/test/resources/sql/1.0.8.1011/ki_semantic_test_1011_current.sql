/*
 Navicat Premium Data Transfer

 Source Server         : 10.1.3.28-autoInsight
 Source Server Type    : MySQL
 Source Server Version : 50726
 Source Host           : 10.1.3.28:3313
 Source Schema         : kylin_semantic_test

 Target Server Type    : MySQL
 Target Server Version : 50726
 File Encoding         : 65001

 Date: 28/10/2019 11:32:14
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for calculate_measure
-- ----------------------------
DROP TABLE IF EXISTS `calculate_measure`;
CREATE TABLE `calculate_measure` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `expression` varchar(5000) NOT NULL,
  `format` varchar(50) NOT NULL,
  `measure_folder` varchar(200) NOT NULL DEFAULT '' COMMENT 'the folder which CM belongs to',
  `extend` varchar(3000) NOT NULL DEFAULT '',
  `visible_flag` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2149 DEFAULT CHARSET=utf8 COMMENT='calculation measure table';

-- ----------------------------
-- Records of calculate_measure
-- ----------------------------
BEGIN;
INSERT INTO `calculate_measure` VALUES (4, 4, 'GMV', '[Measures].[GMV_销售额] + 1000', '$#,###', '', '{\"invisible\":[{\"name\":\"ADMIN\",\"type\":\"user\"}]}', 1);
INSERT INTO `calculate_measure` VALUES (5, 5, '销售数量CM', '[Measures].[saless amount] + 1000', '$#,###', '', '', 1);
INSERT INTO `calculate_measure` VALUES (480, 1454, '计算度量1', '[Measures].[PAF_FACT_FACT_LEG_COUNT] * 1.5', '$#,###', '', '', 1);
COMMIT;

-- ----------------------------
-- Table structure for common_dim_relation
-- ----------------------------
DROP TABLE IF EXISTS `common_dim_relation`;
CREATE TABLE `common_dim_relation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `model_related` varchar(100) NOT NULL,
  `relation` varchar(2000) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8199 DEFAULT CHARSET=utf8 COMMENT='common dimension among models';

-- ----------------------------
-- Records of common_dim_relation
-- ----------------------------
BEGIN;
INSERT INTO `common_dim_relation` VALUES (4, 4, 'snowflake_inner_hierarcy_cube', '', '');
INSERT INTO `common_dim_relation` VALUES (5, 5, 'snowflake_inner_hierarcy_cube', '', '');
INSERT INTO `common_dim_relation` VALUES (1452, 1454, 'EJ_Cube1', 'EJ_Cube2', 'PAF_DIM_FLIGHT=PAF_DIM_FLIGHT,PAF_DIM_CAL=PAF_DIM_CAL');
INSERT INTO `common_dim_relation` VALUES (1453, 1454, 'EJ_Cube2', 'EJ_Cube3', 'PAF_DIM_FLIGHT=PAF_DIM_FLIGHT,PAF_DIM_PASSENGER=PAF_DIM_PASSENGER,PAF_FACT_TRIP=PAF_FACT_TRIP,PAF_DIM_CAL=PAF_DIM_CAL');
INSERT INTO `common_dim_relation` VALUES (1454, 1454, 'EJ_Cube3', 'EJ_Cube4', 'PAF_DIM_FLIGHT=PAF_DIM_FLIGHT,PAF_DIM_PASSENGER=PAF_DIM_PASSENGER,PAF_DIM_CAL=PAF_DIM_CAL');
INSERT INTO `common_dim_relation` VALUES (1455, 1454, 'EJ_Cube4', 'EJ_Cube5', 'PAF_FACT_BOOKING=PAF_FACT_BOOKING,PAF_DIM_FLIGHT=PAF_DIM_FLIGHT,PAF_DIM_PASSENGER=PAF_DIM_PASSENGER,PAF_DIM_CAL=PAF_DIM_CAL');
INSERT INTO `common_dim_relation` VALUES (1456, 1454, 'EJ_Cube5', 'EJ_Cube6', 'PAF_DIM_CAL=PAF_DIM_CAL,PAF_FACT_BOOKING=PAF_FACT_BOOKING');
COMMIT;

-- ----------------------------
-- Table structure for custom_hierarchy
-- ----------------------------
DROP TABLE IF EXISTS `custom_hierarchy`;
CREATE TABLE `custom_hierarchy` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `dim_table` varchar(100) NOT NULL COMMENT 'such as：DEFAULT.KYLIN_SALES',
  `name` varchar(100) NOT NULL,
  `dim_col` varchar(100) NOT NULL,
  `description` varchar(1000) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6439 DEFAULT CHARSET=utf8 COMMENT='custom hierarchy table';

-- ----------------------------
-- Records of custom_hierarchy
-- ----------------------------
BEGIN;
INSERT INTO `custom_hierarchy` VALUES (10, 4, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEGORY', 'META_CATEG_NAME', '');
INSERT INTO `custom_hierarchy` VALUES (11, 4, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEGORY', 'CATEG_LVL2_NAME', '');
INSERT INTO `custom_hierarchy` VALUES (12, 4, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEGORY', 'CATEG_LVL3_NAME', '');
INSERT INTO `custom_hierarchy` VALUES (13, 5, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEGORY', 'META_CATEG_NAME', '');
INSERT INTO `custom_hierarchy` VALUES (14, 5, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEGORY', 'CATEG_LVL2_NAME', '');
INSERT INTO `custom_hierarchy` VALUES (15, 5, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEGORY', 'CATEG_LVL3_NAME', '');
INSERT INTO `custom_hierarchy` VALUES (1432, 1454, 'EJ_Cube1', 'PAF_DIM_FLIGHT', '飞行的层级结构', 'DIMFLIGHTID', '');
INSERT INTO `custom_hierarchy` VALUES (1433, 1454, 'EJ_Cube1', 'PAF_DIM_FLIGHT', '飞行的层级结构', 'DIMFLIGHTDATE', '');
INSERT INTO `custom_hierarchy` VALUES (1434, 1454, 'EJ_Cube1', 'PAF_DIM_FLIGHT', '飞行的层级结构', 'DIMFLIGHTCODE', '');
COMMIT;

-- ----------------------------
-- Table structure for dataset
-- ----------------------------
DROP TABLE IF EXISTS `dataset`;
CREATE TABLE `dataset` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project` varchar(100) NOT NULL,
  `dataset` varchar(100) NOT NULL,
  `type` int(11) NOT NULL COMMENT 'dataset type，0:MDX | 1:SQL',
  `create_user` varchar(100) NOT NULL,
  `create_time` bigint(20) NOT NULL DEFAULT '0',
  `modify_time` bigint(20) NOT NULL DEFAULT '0',
  `canvas` varchar(5000) NOT NULL DEFAULT '',
  `status` varchar(10) NOT NULL DEFAULT 'NORMAL' COMMENT 'dataset status: NORMAL | BROKEN',
  `broken_msg` varchar(5000) NOT NULL DEFAULT '' COMMENT 'the broken detail info when dataset is broken',
  `front_v` varchar(20) NOT NULL DEFAULT 'v0.1',
  `extend` varchar(1000) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7355 DEFAULT CHARSET=utf8 COMMENT='semantic-dataset';

-- ----------------------------
-- Records of dataset
-- ----------------------------
BEGIN;
INSERT INTO `dataset` VALUES (4, 'mdx_automation_test', 'snowflake_dataset', 0, 'ADMIN', 1556421057, 1556421057, '', 'BROKEN', '{}', 'v0.2', '');
INSERT INTO `dataset` VALUES (5, 'mdx_automation_test', 'snowflake_dataset_sql', 1, 'ADMIN', 1556421394, 1556421394, '', 'NORMAL', '', 'v0.2', '');
INSERT INTO `dataset` VALUES (1454, 'EasyJet', 'POC_Test_2', 0, 'ADMIN', 1559383752, 1559383752, '{\"models\":[{\"name\":\"EJ_Cube1\",\"x\":41,\"y\":12,\"top\":[],\"right\":[{\"direction\":\"Left\",\"name\":\"EJ_Cube2\"}],\"bottom\":[],\"left\":[]},{\"name\":\"EJ_Cube2\",\"x\":363,\"y\":13,\"top\":[],\"right\":[{\"direction\":\"Left\",\"name\":\"EJ_Cube3\"}],\"bottom\":[],\"left\":[]},{\"name\":\"EJ_Cube3\",\"x\":232,\"y\":205,\"top\":[],\"right\":[{\"direction\":\"Left\",\"name\":\"EJ_Cube4\"}],\"bottom\":[],\"left\":[]},{\"name\":\"EJ_Cube4\",\"x\":524,\"y\":202,\"top\":[],\"right\":[{\"direction\":\"Left\",\"name\":\"EJ_Cube5\"}],\"bottom\":[],\"left\":[]},{\"name\":\"EJ_Cube5\",\"x\":334,\"y\":378,\"top\":[],\"right\":[{\"direction\":\"Left\",\"name\":\"EJ_Cube6\"}],\"bottom\":[],\"left\":[]},{\"name\":\"EJ_Cube6\",\"x\":692,\"y\":367,\"top\":[],\"right\":[],\"bottom\":[],\"left\":[]}]}', 'NORMAL', '', 'v0.2', '');
COMMIT;

-- ----------------------------
-- Table structure for dim_table_model_rel
-- ----------------------------
DROP TABLE IF EXISTS `dim_table_model_rel`;
CREATE TABLE `dim_table_model_rel` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `dim_table` varchar(100) NOT NULL,
  `relation` int(11) NOT NULL COMMENT 'joint type，0:joint | 1:not joint | 2: many to many',
  `intermediate_dim_table` varchar(100) NOT NULL DEFAULT '' COMMENT 'when relation is 2，need to specify it',
  `primary_dim_col` varchar(100) NOT NULL DEFAULT '' COMMENT 'when relation is 2，need to specify it',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15356 DEFAULT CHARSET=utf8 COMMENT='the relationshiop beteween dimension table and model';

-- ----------------------------
-- Records of dim_table_model_rel
-- ----------------------------
BEGIN;
INSERT INTO `dim_table_model_rel` VALUES (7, 4, 'snowflake_inner_hierarcy_cube', 'SELLER_COUNTRY', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (8, 4, 'snowflake_inner_hierarcy_cube', 'BUYER_COUNTRY', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (9, 4, 'snowflake_inner_hierarcy_cube', 'BUYER_ACCOUNT', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (10, 4, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2738, 1454, 'EJ_Cube2', 'PAF_DIM_CAL', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2739, 1454, 'EJ_Cube2', 'PAF_DIM_FLIGHT', 2, 'PAF_DIM_PASSENGER', 'PAF_FACT_TRIP.FACTTRIPID');
INSERT INTO `dim_table_model_rel` VALUES (2740, 1454, 'EJ_Cube2', 'PAF_DIM_PASSENGER', 2, 'PAF_DIM_FLIGHT', 'PAF_FACT_TRIP.FACTTRIPID');
INSERT INTO `dim_table_model_rel` VALUES (2741, 1454, 'EJ_Cube2', 'PAF_FACT_TRIP', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2742, 1454, 'EJ_Cube1', 'PAF_DIM_CAL', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2743, 1454, 'EJ_Cube1', 'PAF_DIM_FLIGHT', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2744, 1454, 'EJ_Cube1', 'PAF_FACT_LEG', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2745, 1454, 'EJ_Cube4', 'PAF_DIM_CAL', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2746, 1454, 'EJ_Cube4', 'PAF_DIM_FLIGHT', 2, 'PAF_DIM_FLIGHT', 'PAF_FACT_BOOKING.FACTBOOKINGID');
INSERT INTO `dim_table_model_rel` VALUES (2747, 1454, 'EJ_Cube4', 'PAF_DIM_PASSENGER', 2, 'PAF_DIM_FLIGHT', 'PAF_FACT_BOOKING.FACTBOOKINGID');
INSERT INTO `dim_table_model_rel` VALUES (2748, 1454, 'EJ_Cube4', 'PAF_FACT_BOOKING', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2749, 1454, 'EJ_Cube3', 'PAF_DIM_CAL', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2750, 1454, 'EJ_Cube3', 'PAF_DIM_FLIGHT', 2, 'PAF_DIM_PASSENGER', 'PAF_FACT_TRIP.FACTTRIPID');
INSERT INTO `dim_table_model_rel` VALUES (2751, 1454, 'EJ_Cube3', 'PAF_DIM_PASSENGER', 2, 'PAF_DIM_FLIGHT', 'PAF_FACT_TRIP.FACTTRIPID');
INSERT INTO `dim_table_model_rel` VALUES (2752, 1454, 'EJ_Cube3', 'PAF_FACT_TRIP', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2753, 1454, 'EJ_Cube6', 'PAF_DIM_CAL', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2754, 1454, 'EJ_Cube6', 'PAF_FACT_BOOKING', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2755, 1454, 'EJ_Cube5', 'PAF_DIM_CAL', 0, '', '');
INSERT INTO `dim_table_model_rel` VALUES (2756, 1454, 'EJ_Cube5', 'PAF_DIM_FLIGHT', 2, 'PAF_DIM_PASSENGER', 'PAF_FACT_BOOKING.FACTBOOKINGID');
INSERT INTO `dim_table_model_rel` VALUES (2757, 1454, 'EJ_Cube5', 'PAF_DIM_PASSENGER', 2, 'PAF_DIM_FLIGHT', 'PAF_FACT_BOOKING.FACTBOOKINGID');
INSERT INTO `dim_table_model_rel` VALUES (2758, 1454, 'EJ_Cube5', 'PAF_FACT_BOOKING', 0, '', '');
COMMIT;

-- ----------------------------
-- Table structure for measure_group
-- ----------------------------
DROP TABLE IF EXISTS `measure_group`;
CREATE TABLE `measure_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `dim_table` varchar(100) NOT NULL DEFAULT '',
  `calculate_measure` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='measure group info table';

-- ----------------------------
-- Table structure for named_dim_col
-- ----------------------------
DROP TABLE IF EXISTS `named_dim_col`;
CREATE TABLE `named_dim_col` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `dim_table` varchar(100) NOT NULL COMMENT 'KYLIN_SALES',
  `dim_col` varchar(100) NOT NULL,
  `dim_col_alias` varchar(100) NOT NULL,
  `col_type` int(11) NOT NULL DEFAULT '0' COMMENT '0:default, 1:levelYears, 2:levelQuarters, 3:levelMonths, 4:levelWeeks',
  `data_type` varchar(50) NOT NULL DEFAULT '',
  `extend` varchar(3000) NOT NULL DEFAULT '',
  `visible_flag` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38496 DEFAULT CHARSET=utf8 COMMENT='table column alias and type info';

-- ----------------------------
-- Records of named_dim_col
-- ----------------------------
BEGIN;
INSERT INTO `named_dim_col` VALUES (13, 4, 'snowflake_inner_hierarcy_cube', 'SELLER_COUNTRY', 'NAME', 'NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (14, 4, 'snowflake_inner_hierarcy_cube', 'BUYER_COUNTRY', 'NAME', 'NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (15, 4, 'snowflake_inner_hierarcy_cube', 'BUYER_ACCOUNT', 'ACCOUNT_BUYER_LEVEL', 'ACCOUNT_BUYER_LEVEL', 0, 'INTEGER', '{\"invisible\":[{\"name\":\"ADMIN\",\"type\":\"user\"}]}', 1);
INSERT INTO `named_dim_col` VALUES (16, 4, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEG_LVL2_NAME', 'CATEG_LVL2_NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (17, 4, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEG_LVL3_NAME', 'CATEG_LVL3_NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (18, 4, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'META_CATEG_NAME', 'META_CATEG_NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (19, 5, 'snowflake_inner_hierarcy_cube', 'SELLER_COUNTRY', 'NAME', 'NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (20, 5, 'snowflake_inner_hierarcy_cube', 'BUYER_COUNTRY', 'NAME', 'NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (21, 5, 'snowflake_inner_hierarcy_cube', 'BUYER_ACCOUNT', 'ACCOUNT_BUYER_LEVEL', 'ACCOUNT_BUYER_LEVEL', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (22, 5, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEG_LVL2_NAME', 'CATEG_LVL2_NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (23, 5, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'CATEG_LVL3_NAME', 'CATEG_LVL3_NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (24, 5, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'META_CATEG_NAME', 'META_CATEG_NAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (1517, 1454, 'EJ_Cube1', 'PAF_DIM_CAL', 'DIMDATE', 'DIMDATE', 0, 'DATE', '', 1);
INSERT INTO `named_dim_col` VALUES (1518, 1454, 'EJ_Cube1', 'PAF_DIM_CAL', 'DIMWEEKDAY', 'DIMWEEKDAY', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (1519, 1454, 'EJ_Cube1', 'PAF_DIM_FLIGHT', 'DIMFLIGHTCODE', 'DIMFLIGHTCODE', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (1520, 1454, 'EJ_Cube1', 'PAF_DIM_FLIGHT', 'DIMFLIGHTDATE', 'DIMFLIGHTDATE', 0, 'DATE', '', 1);
INSERT INTO `named_dim_col` VALUES (1521, 1454, 'EJ_Cube1', 'PAF_DIM_FLIGHT', 'DIMFLIGHTID', 'DIMFLIGHTID', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (1522, 1454, 'EJ_Cube1', 'PAF_FACT_LEG', 'DIMFLIGHTID', 'DIMFLIGHTID', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (1523, 1454, 'EJ_Cube1', 'PAF_FACT_LEG', 'FACTLEGID', 'FACTLEGID', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (1524, 1454, 'EJ_Cube2', 'PAF_DIM_PASSENGER', 'DIMPASSDOJ', 'DIMPASSDOJ', 0, 'DATE', '', 1);
INSERT INTO `named_dim_col` VALUES (1525, 1454, 'EJ_Cube2', 'PAF_DIM_PASSENGER', 'DIMPASSID', 'DIMPASSID', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (1526, 1454, 'EJ_Cube2', 'PAF_DIM_PASSENGER', 'DIMPASSNAME', 'DIMPASSNAME', 0, 'VARCHAR', '', 1);
INSERT INTO `named_dim_col` VALUES (1527, 1454, 'EJ_Cube2', 'PAF_FACT_TRIP', 'DIMFLIGHTID', 'DIMFLIGHTID', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (1528, 1454, 'EJ_Cube2', 'PAF_FACT_TRIP', 'DIMPASSID', 'DIMPASSID', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (1529, 1454, 'EJ_Cube2', 'PAF_FACT_TRIP', 'FACTTRIPID', 'FACTTRIPID', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (1530, 1454, 'EJ_Cube4', 'PAF_FACT_BOOKING', 'BOOKEDDATE', 'BOOKEDDATE', 0, 'TIMESTAMP', '', 1);
INSERT INTO `named_dim_col` VALUES (1531, 1454, 'EJ_Cube4', 'PAF_FACT_BOOKING', 'DIMFLIGHTID', 'DIMFLIGHTID', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (1532, 1454, 'EJ_Cube4', 'PAF_FACT_BOOKING', 'DIMPASSID', 'DIMPASSID', 0, 'INTEGER', '', 1);
INSERT INTO `named_dim_col` VALUES (1533, 1454, 'EJ_Cube4', 'PAF_FACT_BOOKING', 'FACTBOOKINGID', 'FACTBOOKINGID', 0, 'INTEGER', '', 1);
COMMIT;

-- ----------------------------
-- Table structure for named_dim_table
-- ----------------------------
DROP TABLE IF EXISTS `named_dim_table`;
CREATE TABLE `named_dim_table` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `dim_table` varchar(100) NOT NULL COMMENT 'KYLIN_SALES',
  `dim_table_alias` varchar(100) NOT NULL DEFAULT '',
  `dim_table_type` varchar(100) NOT NULL DEFAULT 'regular' COMMENT 'values: regular|time',
  `actual_table` varchar(200) NOT NULL DEFAULT '',
  `fact_table` varchar(200) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17379 DEFAULT CHARSET=utf8 COMMENT='dim table alias and type info';

-- ----------------------------
-- Records of named_dim_table
-- ----------------------------
BEGIN;
INSERT INTO `named_dim_table` VALUES (7, 4, 'snowflake_inner_hierarcy_cube', 'SELLER_COUNTRY', 'SELLER_COUNTRY', 'regular', 'DEFAULT.KYLIN_COUNTRY', 'DEFAULT.KYLIN_SALES');
INSERT INTO `named_dim_table` VALUES (8, 4, 'snowflake_inner_hierarcy_cube', 'BUYER_COUNTRY', 'BUYER_COUNTRY', 'regular', 'DEFAULT.KYLIN_COUNTRY', 'DEFAULT.KYLIN_SALES');
INSERT INTO `named_dim_table` VALUES (9, 4, 'snowflake_inner_hierarcy_cube', 'BUYER_ACCOUNT', '购买者账户维表', 'regular', 'DEFAULT.KYLIN_ACCOUNT', 'DEFAULT.KYLIN_SALES');
INSERT INTO `named_dim_table` VALUES (10, 4, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'KYLIN_CATEGORY_GROUPINGS', 'regular', 'DEFAULT.KYLIN_CATEGORY_GROUPINGS', 'DEFAULT.KYLIN_SALES');
INSERT INTO `named_dim_table` VALUES (11, 5, 'snowflake_inner_hierarcy_cube', 'SELLER_COUNTRY', '卖家国家信息表', 'regular', 'DEFAULT.KYLIN_COUNTRY', 'DEFAULT.KYLIN_SALES');
INSERT INTO `named_dim_table` VALUES (12, 5, 'snowflake_inner_hierarcy_cube', 'BUYER_COUNTRY', '买家国家信息表', 'regular', 'DEFAULT.KYLIN_COUNTRY', 'DEFAULT.KYLIN_SALES');
INSERT INTO `named_dim_table` VALUES (13, 5, 'snowflake_inner_hierarcy_cube', 'BUYER_ACCOUNT', 'BUYER_ACCOUNT', 'regular', 'DEFAULT.KYLIN_ACCOUNT', 'DEFAULT.KYLIN_SALES');
INSERT INTO `named_dim_table` VALUES (14, 5, 'snowflake_inner_hierarcy_cube', 'KYLIN_CATEGORY_GROUPINGS', 'KYLIN_CATEGORY_GROUPINGS', 'regular', 'DEFAULT.KYLIN_CATEGORY_GROUPINGS', 'DEFAULT.KYLIN_SALES');
INSERT INTO `named_dim_table` VALUES (519, 1454, 'EJ_Cube1', 'PAF_DIM_CAL', 'PAF_DIM_CAL', 'regular', 'EASYJET.PAF_DIM_CAL', 'EASYJET.PAF_FACT_LEG');
INSERT INTO `named_dim_table` VALUES (520, 1454, 'EJ_Cube1', 'PAF_DIM_FLIGHT', 'PAF_DIM_FLIGHT', 'regular', 'EASYJET.PAF_DIM_FLIGHT', 'EASYJET.PAF_FACT_LEG');
INSERT INTO `named_dim_table` VALUES (521, 1454, 'EJ_Cube1', 'PAF_FACT_LEG', 'PAF_FACT_LEG', 'regular', 'EASYJET.PAF_FACT_LEG', 'EASYJET.PAF_FACT_LEG');
INSERT INTO `named_dim_table` VALUES (522, 1454, 'EJ_Cube2', 'PAF_DIM_PASSENGER', 'PAF_DIM_PASSENGER', 'regular', 'EASYJET.PAF_DIM_PASSENGER', 'EASYJET.PAF_FACT_TRIP');
INSERT INTO `named_dim_table` VALUES (523, 1454, 'EJ_Cube2', 'PAF_FACT_TRIP', 'PAF_FACT_TRIP', 'regular', 'EASYJET.PAF_FACT_TRIP', 'EASYJET.PAF_FACT_TRIP');
INSERT INTO `named_dim_table` VALUES (524, 1454, 'EJ_Cube4', 'PAF_FACT_BOOKING', 'PAF_FACT_BOOKING', 'regular', 'EASYJET.PAF_FACT_BOOKING', 'EASYJET.PAF_FACT_BOOKING');
COMMIT;

-- ----------------------------
-- Table structure for named_measure
-- ----------------------------
DROP TABLE IF EXISTS `named_measure`;
CREATE TABLE `named_measure` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `model` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `alias` varchar(100) NOT NULL COMMENT 'measure name renamed in KI',
  `expression` varchar(200) NOT NULL DEFAULT '',
  `data_type` varchar(50) NOT NULL DEFAULT '',
  `dim_column` varchar(200) NOT NULL DEFAULT '',
  `extend` varchar(3000) NOT NULL DEFAULT '',
  `visible_flag` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21163 DEFAULT CHARSET=utf8 COMMENT='measure alias info';

-- ----------------------------
-- Records of named_measure
-- ----------------------------
BEGIN;
INSERT INTO `named_measure` VALUES (8, 4, 'snowflake_inner_hierarcy_cube', '_COUNT_', '个数', 'COUNT', 'bigint', 'constant', '', 1);
INSERT INTO `named_measure` VALUES (9, 4, 'snowflake_inner_hierarcy_cube', 'GMV_销售额', 'GMV_销售额', 'SUM', 'decimal(19,4)', 'KYLIN_SALES.PRICE', '{\"invisible\":[{\"name\":\"ADMIN\",\"type\":\"user\"}]}', 1);
INSERT INTO `named_measure` VALUES (10, 4, 'snowflake_inner_hierarcy_cube', '销售数量总计', '销售数量总计', 'SUM', 'bigint', 'KYLIN_SALES.ITEM_COUNT', '', 1);
INSERT INTO `named_measure` VALUES (11, 5, 'snowflake_inner_hierarcy_cube', '_COUNT_', '个数(count)', 'COUNT', 'bigint', 'constant', '', 1);
INSERT INTO `named_measure` VALUES (12, 5, 'snowflake_inner_hierarcy_cube', 'GMV_销售额', 'GMV_销售额', 'SUM', 'decimal(19,4)', 'KYLIN_SALES.PRICE', '', 1);
INSERT INTO `named_measure` VALUES (13, 5, 'snowflake_inner_hierarcy_cube', '销售数量总计', 'saless amount', 'SUM', 'bigint', 'KYLIN_SALES.ITEM_COUNT', '', 1);
INSERT INTO `named_measure` VALUES (994, 1454, 'EJ_Cube1', 'MILES', 'MILES', 'SUM', 'bigint', 'PAF_FACT_LEG.MILES', '', 1);
INSERT INTO `named_measure` VALUES (995, 1454, 'EJ_Cube1', 'PAF_FACT_FACT_LEG_COUNT', 'PAF_FACT_FACT_LEG_COUNT', 'COUNT', 'bigint', 'constant', '', 1);
INSERT INTO `named_measure` VALUES (996, 1454, 'EJ_Cube1', 'STAGES', 'STAGES', 'SUM', 'bigint', 'PAF_FACT_LEG.STAGE', '', 1);
INSERT INTO `named_measure` VALUES (997, 1454, 'EJ_Cube2', 'FACT_INFLIGHT_SALES', 'FACT_INFLIGHT_SALES', 'SUM', 'double', 'PAF_FACT_TRIP.FACTINFLIGHTSALES', '', 1);
INSERT INTO `named_measure` VALUES (998, 1454, 'EJ_Cube2', 'PAF_FACT_TRIP_COUNT', 'PAF_FACT_TRIP_COUNT', 'COUNT', 'bigint', 'constant', '', 1);
INSERT INTO `named_measure` VALUES (999, 1454, 'EJ_Cube3', 'FACT_INFLIGHT_SALES', 'FACT_INFLIGHT_SALES_1', 'SUM', 'double', 'PAF_FACT_TRIP.FACTINFLIGHTSALES', '', 1);
INSERT INTO `named_measure` VALUES (1000, 1454, 'EJ_Cube3', 'PAF_FACT_TRIP_COUNT', 'PAF_FACT_TRIP_COUNT_1', 'COUNT', 'bigint', 'constant', '', 1);
INSERT INTO `named_measure` VALUES (1001, 1454, 'EJ_Cube4', 'FACT_PAID', 'FACT_PAID', 'SUM', 'double', 'PAF_FACT_BOOKING.FACTPAID', '', 1);
INSERT INTO `named_measure` VALUES (1002, 1454, 'EJ_Cube4', 'PAF_FACT_BOOKING', 'PAF_FACT_BOOKING', 'COUNT', 'bigint', 'constant', '', 1);
INSERT INTO `named_measure` VALUES (1003, 1454, 'EJ_Cube5', 'FACT_PAID', 'FACT_PAID_1', 'SUM', 'double', 'PAF_FACT_BOOKING.FACTPAID', '', 1);
INSERT INTO `named_measure` VALUES (1004, 1454, 'EJ_Cube5', 'PAF_FACT_BOOKING_COUNT', 'PAF_FACT_BOOKING_COUNT', 'COUNT', 'bigint', 'constant', '', 1);
INSERT INTO `named_measure` VALUES (1005, 1454, 'EJ_Cube6', 'FACT_PAID', 'FACT_PAID_2', 'SUM', 'double', 'PAF_FACT_BOOKING.FACTPAID', '', 1);
INSERT INTO `named_measure` VALUES (1006, 1454, 'EJ_Cube6', 'PAF_FACT_BOOKING', 'PAF_FACT_BOOKING_1', 'COUNT', 'bigint', 'constant', '', 1);
COMMIT;

-- ----------------------------
-- Table structure for named_set
-- ----------------------------
DROP TABLE IF EXISTS `named_set`;
CREATE TABLE `named_set` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dataset_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `expression` varchar(3000) NOT NULL,
  `folder` varchar(200) NOT NULL DEFAULT '' COMMENT 'the folder which named set belongs to',
  `extend` varchar(3000) NOT NULL DEFAULT '',
  `location` varchar(200) NOT NULL DEFAULT '',
  `visible_flag` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_pk` (`dataset_id`,`name`),
  KEY `dataset_id_key` (`dataset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=328 DEFAULT CHARSET=utf8 COMMENT='named set info';

-- ----------------------------
-- Records of named_set
-- ----------------------------
BEGIN;
INSERT INTO `named_set` VALUES (3, 4, 'Top CA Cities', 'TopCount([CA Cities], 2, [Measures].[Unit Sales])', 'named-set', '{\"invisible\":[{\"name\":\"admin\",\"type\":\"user\"}]}', '', 1);
COMMIT;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `password` varchar(256) NOT NULL,
  `active` int(11) NOT NULL,
  `license_auth` int(11) NOT NULL,
  `login_count` int(11) NOT NULL DEFAULT '0',
  `last_login` bigint(20) NOT NULL DEFAULT '0',
  `create_time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `username_key` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='user information';

-- ----------------------------
-- Records of user_info
-- ----------------------------
BEGIN;
INSERT INTO `user_info` VALUES (1, 'ADMIN', 'd4341cc91fed0bed9f6aae7c09bda0d4', 1, 1, 1, 0, 0);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
