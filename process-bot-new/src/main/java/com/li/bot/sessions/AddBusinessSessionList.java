package com.li.bot.sessions;

import com.li.bot.sessions.enums.BusinessSessionState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class AddBusinessSessionList {


    private Map<Long, BusinessSession> userSessionMap = new HashMap<>();

    public void addUserSession(Long tgId) {
        BusinessSession businessSession = new BusinessSession();
        businessSession.setState(BusinessSessionState.WAITING_FOR_BUSINESS_NAME);
        userSessionMap.put(tgId, businessSession);
    }
    public BusinessSession getUserSession(Long tgId) {
        return userSessionMap.get(tgId);
    }

    public void removeUserSession(Long tgId) {
        userSessionMap.remove(tgId);
    }

    public void clear() {
        userSessionMap.clear();
    }

}
