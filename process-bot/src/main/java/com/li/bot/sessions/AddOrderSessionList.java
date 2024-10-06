package com.li.bot.sessions;

import com.li.bot.entity.database.Business;
import com.li.bot.sessions.enums.OrderSessionState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class AddOrderSessionList {


    private Map<Long, OrderSession> userSessionMap = new HashMap<>();

    public void addUserSession(Long tgId, Business business) {
        OrderSession orderSession = new OrderSession(business);
        orderSession.setState(OrderSessionState.WAITING_FOR_USER_MESSAGE);
        userSessionMap.put(tgId, orderSession);
    }
    public OrderSession getUserSession(Long tgId) {
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
