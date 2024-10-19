package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
@TableName("tg_prize_pool")
@Data
public class PrizePool {
    @TableId(value = "prize_pool_id")
    private String prizePoolId;
    @TableField(value="lottery_id")
    private String lotteryId;
    @TableField("money")
    private BigDecimal money ;
    @TableField("status")
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;


}
