package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
@TableName("tg_lottery")
@Data
public class Lottery {
    @TableId(value = "lottery_id", type = IdType.INPUT)
    private String lotteryId;
    @TableField("tg_id")
    private Long tgId;
    @TableField("chat_id")
    private Long chatId ;
    @TableField("message_id")
    private Long messageId;
    @TableField("money")
    private BigDecimal money ;
    @TableField("number")
    private Integer number;
    @TableField("status")
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

}
