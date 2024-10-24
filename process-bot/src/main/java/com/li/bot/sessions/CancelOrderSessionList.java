package com.li.bot.sessions;

import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.entity.database.User;
import com.li.bot.sessions.enums.AdminEditSessionState;
import com.li.bot.sessions.enums.CancelOrderSessionState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class CancelOrderSessionList {


    private Map<Long, CancelOrderSession> userSessionMap = new HashMap<>();

    public void addUserSession(Long tgId, Reply reply, Order order,Integer messageId) {
        CancelOrderSession cancelOrderSession = new CancelOrderSession();
        cancelOrderSession.setOrder(order);
        cancelOrderSession.setReply(reply);
        cancelOrderSession.setMessageId(messageId);
        cancelOrderSession.setState(CancelOrderSessionState.WAITING_FOR_USER_MESSAGE);
        userSessionMap.put(tgId, cancelOrderSession);
    }
    public CancelOrderSession getCancelOrderSession(Long tgId) {
        return userSessionMap.get(tgId);
    }

    public void removeUserSession(Long tgId) {
        userSessionMap.remove(tgId);
    }

    //清空map
    public void clear() {
        userSessionMap.clear();
    }


}
