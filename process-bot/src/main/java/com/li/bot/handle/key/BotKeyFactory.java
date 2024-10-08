package com.li.bot.handle.key;

import com.li.bot.handle.menu.IBotMenu;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class BotKeyFactory {

    private Map<String , IKeyboard> map = new HashMap<>();

    public BotKeyFactory(List<IKeyboard> key) {
        key.forEach(menu -> map.put(menu.getKeyName(), menu));
    }

    public IKeyboard getKey(String menuName) {
        if (menuName == null) {
            return null;
        }
        return map.get(menuName);
    }

}
