package com.li.bot.sessions;

import com.li.bot.entity.database.Business;
import com.li.bot.sessions.enums.AdminEditSessionState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class AdminEditSessionList {


    private Map<Long, AdminEditSession> userSessionMap = new HashMap<>();

    public void addUserSession(Long tgId, Business business,Integer type) {
        AdminEditSession businessSession = new AdminEditSession(business);
        businessSession.setState(AdminEditSessionState.WAITING_FOR_USER_MESSAGE);
        businessSession.setType(type);
        userSessionMap.put(tgId, businessSession);
    }
    public AdminEditSession getUserSession(Long tgId) {
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
