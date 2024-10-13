package com.li.bot.sessions;

import com.li.bot.entity.database.Convoys;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class UpdateConvoysSessionList {


    private Map<Long, UpdateConvoysSession> convoysSessionMap = new HashMap<>();

    public void addConvoysSession(Long tgId,Convoys convoys,Integer type) {
        UpdateConvoysSession businessSession = new UpdateConvoysSession(convoys);
        businessSession.setState(UpdateConvoysSessionState.WAITING);
        businessSession.setType(type);
        convoysSessionMap.put(tgId, businessSession);
    }
    public UpdateConvoysSession getUserSession(Long tgId) {
        return convoysSessionMap.get(tgId);
    }

    public void removeUserSession(Long tgId) {
        convoysSessionMap.remove(tgId);
    }

    public void clear() {
        convoysSessionMap.clear();
    }

}
