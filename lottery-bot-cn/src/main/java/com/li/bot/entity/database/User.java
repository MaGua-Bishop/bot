package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@TableName("tg_user")
public class User {
    @TableId(value = "tg_id",type = IdType.INPUT)
    private Long tgId;
    @TableField(value = "tg_name")
    private String tgName ;
    @TableField("tg_username")
    private String tgUserName ;
    @TableField("is_admin")
    private Boolean isAdmin ;
    @TableField("create_time")
    private Date createTime;
    @TableField("money")
    private BigDecimal money ;

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

    public String getTgUserName() {
        return tgUserName;
    }

    public void setTgUserName(String tgUserName) {
        this.tgUserName = tgUserName;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean admin) {
        isAdmin = admin;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }
}
