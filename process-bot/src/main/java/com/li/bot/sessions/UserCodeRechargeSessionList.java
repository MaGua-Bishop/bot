package com.li.bot.sessions;

import com.li.bot.sessions.enums.BusinessSessionState;
import com.li.bot.sessions.enums.UserCodeRechargeState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class UserCodeRechargeSessionList {


    private Map<Long, UserCodeRechargeSession> userSessionMap = new HashMap<>();

    public void addUserSession(Long tgId) {
        UserCodeRechargeSession userCodeRechargeSession = new UserCodeRechargeSession();
        userCodeRechargeSession.setState(UserCodeRechargeState.WAITING_FOR_USER_MONEY);
        userSessionMap.put(tgId, userCodeRechargeSession);
    }
    public UserCodeRechargeSession getUserSession(Long tgId) {
        return userSessionMap.get(tgId);
    }

    public void removeUserSession(Long tgId) {
        userSessionMap.remove(tgId);
    }

    public void clear() {
        userSessionMap.clear();
    }

}
