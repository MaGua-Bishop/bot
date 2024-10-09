package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-03
 */
@Data
@TableName("tg_reply")
public class Reply {

    @TableId(value = "reply_id", type = IdType.AUTO)
    private Long replyId;

    @TableField("tg_id")
    private Long tgId;

    @TableField("order_id")
    private String orderId;

    @TableField("message_chat_id")
    private Long[] messageChatId;

    @TableField("message_id")
    private Long[] messageId;

    @TableField("message_type")
    private String[] messageType;

    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;




}
