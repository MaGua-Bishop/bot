package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@TableName("tg_luckydraw")
public class Luckydraw {
    @TableId(value = "luckydraw_id", type = IdType.AUTO)
    private Long luckydrawId;
    @TableField("tg_id")
    private Long tgId;

    @TableField("tg_full_name")
    private String tgFullName;

    @TableField("tg_username")
    private String tgUserName;

    @TableField("money")
    private BigDecimal money;
    @TableField("status")
    private Integer status;

    @TableField("luckydraw_time")
    private LocalDateTime luckydrawTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getLuckydrawId() {
        return luckydrawId;
    }

    public void setLuckydrawId(Long luckydrawId) {
        this.luckydrawId = luckydrawId;
    }

    public Long getTgId() {
        return tgId;
    }

    public void setTgId(Long tgId) {
        this.tgId = tgId;
    }

    public String getTgFullName() {
        return tgFullName;
    }

    public void setTgFullName(String tgFullName) {
        this.tgFullName = tgFullName;
    }

    public String getTgUserName() {
        return tgUserName;
    }

    public void setTgUserName(String tgUserName) {
        this.tgUserName = tgUserName;
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

    public LocalDateTime getLuckydrawTime() {
        return luckydrawTime;
    }

    public void setLuckydrawTime(LocalDateTime luckydrawTime) {
        this.luckydrawTime = luckydrawTime;
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
