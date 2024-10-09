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

 Date: 06/10/2024 18:53:56
*/


-- ----------------------------
-- Table structure for tg_business
-- ----------------------------
DROP TABLE IF EXISTS "public"."tg_business";
CREATE TABLE "public"."tg_business" (
  "business_id" int8 NOT NULL DEFAULT nextval('tg_business_business_id_seq'::regclass),
  "tg_id" int8 NOT NULL,
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "message_id" int8 NOT NULL,
  "money" numeric(10,2) NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table tg_business
-- ----------------------------
ALTER TABLE "public"."tg_business" ADD CONSTRAINT "tg_business_pkey" PRIMARY KEY ("business_id");
