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

 Date: 06/10/2024 18:54:18
*/


-- ----------------------------
-- Table structure for tg_reply
-- ----------------------------
DROP TABLE IF EXISTS "public"."tg_reply";
CREATE TABLE "public"."tg_reply" (
  "reply_id" int8 NOT NULL DEFAULT nextval('tg_reply_reply_id_seq'::regclass),
  "tg_id" int8 NOT NULL,
  "order_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "message_chat_id" int8[],
  "message_id" int8[],
  "message_type" varchar(50)[] COLLATE "pg_catalog"."default",
  "status" int4 NOT NULL DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "update_time" timestamp(6)
)
;

-- ----------------------------
-- Primary Key structure for table tg_reply
-- ----------------------------
ALTER TABLE "public"."tg_reply" ADD CONSTRAINT "tg_reply_pkey" PRIMARY KEY ("reply_id");
