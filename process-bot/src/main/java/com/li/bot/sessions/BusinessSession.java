package com.li.bot.sessions;

import com.li.bot.entity.database.Business;
import com.li.bot.sessions.enums.BusinessSessionState;
/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class BusinessSession {
    private BusinessSessionState state;
    private Business business;

    public BusinessSession() {
        this.business = new Business();
    }

    public BusinessSessionState getState() {
        return state;
    }

    public void setState(BusinessSessionState state) {
        this.state = state;
    }


    public Business getBusiness() {
        return business;
    }

}
