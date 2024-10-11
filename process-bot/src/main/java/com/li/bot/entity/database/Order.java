package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@TableName("tg_order")
@Data
public class Order {

    @TableId(value = "order_id")
    private String orderId;

    @TableField("tg_id")
    private Long tgId;

    @TableField("review_tg_id")
    private Long reviewTgId;

    @TableField("business_id")
    private Long businessId;

    @TableField("status")
    private Integer status;  // 发单状态，用整数表示

    @TableField("create_time")
    private Date createTime;  // 创建时间

    @TableField("update_time")
    private Date updateTime;  // 更新时间

    @TableField("message_text")
    private String messageText;

}
