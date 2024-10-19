package com.li.bot.handle.callback;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class CallbackFactory {

    private Map<String , ICallback> map = new HashMap<>();

    public CallbackFactory(List<ICallback> callback) {
        callback.forEach(c -> map.put(c.getCallbackName(), c));
    }

    public ICallback getCallback(String callbackName) {
        if (callbackName == null) {
            return null;
        }
        return map.get(callbackName);
    }

}
