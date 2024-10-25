package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
@TableName("tg_lottery_info")
public class LotteryInfo {
    @TableId(value = "lottery_info_id")
    private String lotteryInfoId;
    @TableField(value="lottery_id")
    private String lotteryId;
    @TableField(value="lottery_create_tg_id")
    private Long lotteryCreateTgId;
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


    public String getLotteryInfoId() {
        return lotteryInfoId;
    }

    public void setLotteryInfoId(String lotteryInfoId) {
        this.lotteryInfoId = lotteryInfoId;
    }

    public String getLotteryId() {
        return lotteryId;
    }

    public void setLotteryId(String lotteryId) {
        this.lotteryId = lotteryId;
    }

    public Long getLotteryCreateTgId() {
        return lotteryCreateTgId;
    }

    public void setLotteryCreateTgId(Long lotteryCreateTgId) {
        this.lotteryCreateTgId = lotteryCreateTgId;
    }

    public String getPrizePoolId() {
        return prizePoolId;
    }

    public void setPrizePoolId(String prizePoolId) {
        this.prizePoolId = prizePoolId;
    }

    public Long getTgId() {
        return tgId;
    }

    public void setTgId(Long tgId) {
        this.tgId = tgId;
    }

    public String getTgName() {
        return tgName;
    }

    public void setTgName(String tgName) {
        this.tgName = tgName;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
