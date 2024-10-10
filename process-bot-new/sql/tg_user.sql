/*
 Navicat Premium Data Transfer

 Source Server         : 本地PostgreSQL
 Source Server Type    : PostgreSQL
 Source Server Version : 170000 (170000)
 Source Host           : localhost:5432
 Source Catalog        : process_bot
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 170000 (170000)
 File Encoding         : 65001

 Date: 06/10/2024 18:54:27
*/


-- ----------------------------
-- Table structure for tg_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."tg_user";
CREATE TABLE "public"."tg_user" (
  "tg_id" int8 NOT NULL,
  "tg_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "money" numeric(10,2) DEFAULT 0.00,
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "is_admin" bool DEFAULT false
)
;

-- ----------------------------
-- Primary Key structure for table tg_user
-- ----------------------------
ALTER TABLE "public"."tg_user" ADD CONSTRAINT "tg_user_pkey" PRIMARY KEY ("tg_id");
