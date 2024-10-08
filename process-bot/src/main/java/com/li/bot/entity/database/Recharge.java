package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author: li
 * @CreateTime: 2024-10-05
 */
@Data
@TableName("tg_recharge")
public class Recharge {

    @TableId(value = "recharge_id", type = IdType.AUTO)
    private Long rechargeId;
    @TableField("tg_id")
    private Long tgId;
    @TableField("money")
    private BigDecimal money ;
    @TableField("status")
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime ;
    @TableField("update_time")
    private LocalDateTime updateTime ;

}
