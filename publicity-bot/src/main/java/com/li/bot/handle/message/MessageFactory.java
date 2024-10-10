package com.li.bot.handle.message;

import com.li.bot.handle.callback.ICallback;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class MessageFactory {

    private Map<String , IMessage> map = new HashMap<>();

    public MessageFactory(List<IMessage> message) {
        message.forEach(c -> map.put(c.getMessageName(), c));
    }

    public IMessage getMessage(String messageName) {
        if (messageName == null) {
            return null;
        }
        return map.get(messageName);
    }

}
