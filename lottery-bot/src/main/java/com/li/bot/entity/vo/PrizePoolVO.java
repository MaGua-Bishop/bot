package com.li.bot.entity.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: li
 * @CreateTime: 2024-10-19
 */
@Data
@TableName("tg_prize_pool")
public class PrizePoolVO {
    @TableId(value = "prize_pool_id")
    private String prizePoolId;

    @TableField("money")
    private BigDecimal money ;

}
