package com.li.bot.sessions;

import com.li.bot.entity.database.User;
import com.li.bot.sessions.enums.UserCreateLotterySessionState;
import com.li.bot.sessions.enums.UserTakeoutSessionState;

import java.math.BigDecimal;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class UserCreateLotterySession {
    private UserCreateLotterySessionState state;
    private String uid ;

    private Integer type ;

    private boolean isAdmin = false;

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean admin) {
        isAdmin = admin;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public UserCreateLotterySessionState getState() {
        return state;
    }

    public void setState(UserCreateLotterySessionState state) {
        this.state = state;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
