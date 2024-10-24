package com.li.bot.sessions;

import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.entity.database.User;
import com.li.bot.sessions.enums.AdminEditSessionState;
import com.li.bot.sessions.enums.CancelOrderSessionState;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class CancelOrderSession {
    private CancelOrderSessionState state;
    private Reply reply;

    private Order order;

    private Integer messageId;

    public CancelOrderSessionState getState() {
        return state;
    }

    public void setState(CancelOrderSessionState state) {
        this.state = state;
    }

    public Reply getReply() {
        return reply;
    }

    public void setReply(Reply reply) {
        this.reply = reply;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }
}
