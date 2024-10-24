package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Data
@TableName("tg_business")
public class Business {

    @TableId(value = "business_id",type = IdType.AUTO)
    private Long businessId;

    @TableField("tg_id")
    private Long tgId;

    @TableField("name")
    private String name;

    @TableField("message_id")
    private Integer messageId;

    @TableField("money")
    private BigDecimal money;

    @TableField("status")
    private Integer status;
    @TableField("type")
    private Integer type ;
    @TableField("is_shelving")
    private Boolean isShelving;


    @TableField(exist = false)
    private String messageText;

}
