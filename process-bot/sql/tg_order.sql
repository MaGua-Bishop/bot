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

 Date: 06/10/2024 18:54:03
*/


-- ----------------------------
-- Table structure for tg_order
-- ----------------------------
DROP TABLE IF EXISTS "public"."tg_order";
CREATE TABLE "public"."tg_order" (
  "order_id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "tg_id" int8 NOT NULL,
  "review_tg_id" int8,
  "business_id" int8 NOT NULL,
  "message_id" int8 NOT NULL,
  "status" int4 NOT NULL DEFAULT 0,
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "update_time" timestamp(6)
)
;

-- ----------------------------
-- Primary Key structure for table tg_order
-- ----------------------------
ALTER TABLE "public"."tg_order" ADD CONSTRAINT "tg_order_pkey" PRIMARY KEY ("order_id");
