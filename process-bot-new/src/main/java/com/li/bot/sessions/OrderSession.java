package com.li.bot.sessions;

import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.sessions.enums.BusinessSessionState;
import com.li.bot.sessions.enums.OrderSessionState;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class OrderSession {
    private OrderSessionState state;
    private Business business;

    public OrderSession(Business business) {
        this.business = business;
    }

    public OrderSessionState getState() {
        return state;
    }

    public void setState(OrderSessionState state) {
        this.state = state;
    }


    public Business getBusiness() {
        return business;
    }

}
