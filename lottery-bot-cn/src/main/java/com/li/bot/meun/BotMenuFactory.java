package com.li.bot.meun;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-09-29
 * @Description: 机器人菜单工厂
 */
@Component
public class BotMenuFactory {

    private Map<String , IBotMenu> map = new HashMap<>();

    public BotMenuFactory(List<IBotMenu> menus) {
        menus.forEach(menu -> map.put(menu.getMenuName(), menu));
    }

    public IBotMenu getMenu(String menuName) {
        if (menuName == null) {
            return null;
        }
        return map.get(menuName);
    }

}
