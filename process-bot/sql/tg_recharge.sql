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

 Date: 06/10/2024 18:54:12
*/


-- ----------------------------
-- Table structure for tg_recharge
-- ----------------------------
DROP TABLE IF EXISTS "public"."tg_recharge";
CREATE TABLE "public"."tg_recharge" (
  "recharge_id" int8 NOT NULL DEFAULT nextval('tg_recharge_recharge_id_seq'::regclass),
  "tg_id" int8 NOT NULL,
  "money" numeric(10,2) NOT NULL,
  "status" int4 NOT NULL DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "update_time" timestamp(6)
)
;

-- ----------------------------
-- Primary Key structure for table tg_recharge
-- ----------------------------
ALTER TABLE "public"."tg_recharge" ADD CONSTRAINT "tg_recharge_pkey" PRIMARY KEY ("recharge_id");
