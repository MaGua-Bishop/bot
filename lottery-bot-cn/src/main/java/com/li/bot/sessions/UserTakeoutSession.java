package com.li.bot.sessions;

import com.li.bot.entity.database.User;
import com.li.bot.sessions.enums.UserTakeoutSessionState;

import java.math.BigDecimal;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class UserTakeoutSession {
    private UserTakeoutSessionState state;
    private User user ;

    private BigDecimal money;

    public UserTakeoutSessionState getState() {
        return state;
    }

    public void setState(UserTakeoutSessionState state) {
        this.state = state;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }
}
