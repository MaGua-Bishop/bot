package com.li.bot.sessions;

import com.li.bot.entity.database.User;
import com.li.bot.sessions.enums.UserTakeoutSessionState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class UserTakeoutSessionList {


    private Map<Long, UserTakeoutSession> userSessionMap = new HashMap<>();

    public void addUserSession(Long tgId, BigDecimal money) {
        UserTakeoutSession userTakeoutSession = new UserTakeoutSession();
        userTakeoutSession.setMoney(money);
        userTakeoutSession.setState(UserTakeoutSessionState.WAITING_FOR_USER_MESSAGE);
        userSessionMap.put(tgId, userTakeoutSession);
    }
    public UserTakeoutSession getUserTakeoutSession(Long tgId) {
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
