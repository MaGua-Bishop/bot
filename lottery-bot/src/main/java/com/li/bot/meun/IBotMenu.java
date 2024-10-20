package com.li.bot.meun;

import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * @Author: li
 * @CreateTime: 2024-09-29
 * @Description: 菜单接口
 */
public interface IBotMenu {
    String getMenuName();

    void execute(BotServiceImpl bot ,Message message);

}
