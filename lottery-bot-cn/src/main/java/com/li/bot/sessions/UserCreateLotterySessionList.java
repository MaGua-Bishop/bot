package com.li.bot.sessions;

import com.li.bot.sessions.enums.UserCreateLotterySessionState;
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
public class UserCreateLotterySessionList {


    private Map<Long, UserCreateLotterySession> userSessionMap = new HashMap<>();

    public void addUserSession(Long tgId, String uid,Integer type) {
        UserCreateLotterySession userCreateLotterySession = new UserCreateLotterySession();
        userCreateLotterySession.setUid(uid);
        userCreateLotterySession.setType(type);
        userCreateLotterySession.setState(UserCreateLotterySessionState.CREATE_LOTTERY);
        userSessionMap.put(tgId, userCreateLotterySession);
    }
    public UserCreateLotterySession getUserTakeoutSession(Long tgId) {
        return userSessionMap.get(tgId);
    }

    public void updateUserSession(Long tgId, UserCreateLotterySessionState state,Integer type,boolean isAdmin) {
        UserCreateLotterySession userCreateLotterySession = userSessionMap.get(tgId);
        userCreateLotterySession.setState(state);
        userCreateLotterySession.setType(type);
        userCreateLotterySession.setIsAdmin(isAdmin);
    }

    public void removeUserSession(Long tgId) {
        userSessionMap.remove(tgId);
    }

    //清空map
    public void clear() {
        userSessionMap.clear();
    }


}
