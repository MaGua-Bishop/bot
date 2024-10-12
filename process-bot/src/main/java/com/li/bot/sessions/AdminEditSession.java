package com.li.bot.sessions;

import com.li.bot.entity.database.Business;
import com.li.bot.sessions.enums.AdminEditSessionState;
import com.li.bot.sessions.enums.OrderSessionState;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class AdminEditSession {
    private AdminEditSessionState state;
    private Business business;

    private Integer type ;

    public void setBusiness(Business business) {
        this.business = business;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public AdminEditSession(Business business) {
        this.business = business;
    }

    public AdminEditSessionState getState() {
        return state;
    }

    public void setState(AdminEditSessionState state) {
        this.state = state;
    }


    public Business getBusiness() {
        return business;
    }

}
