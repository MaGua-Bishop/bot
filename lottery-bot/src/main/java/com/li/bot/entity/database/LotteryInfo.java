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
@TableName("tg_lottery_info")
@Data
public class LotteryInfo {
    @TableId(value = "lottery_info_id")
    private String lotteryInfoId;
    @TableField(value="lottery_id")
    private String lotteryId;
    @TableField(value="prize_pool_id")
    private String prizePoolId;
    @TableField("tg_id")
    private Long tgId;
    @TableField("tg_name")
    private String tgName;
    @TableField("money")
    private BigDecimal money ;
    @TableField("status")
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;


}
