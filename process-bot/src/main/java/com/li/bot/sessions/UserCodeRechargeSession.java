package com.li.bot.sessions;

import com.li.bot.entity.database.Business;
import com.li.bot.sessions.enums.BusinessSessionState;
import com.li.bot.sessions.enums.UserCodeRechargeState;

import java.math.BigDecimal;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class UserCodeRechargeSession {
    private UserCodeRechargeState state;
    private BigDecimal money;

    public UserCodeRechargeState getState() {
        return state;
    }

    public void setState(UserCodeRechargeState state) {
        this.state = state;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }
}
