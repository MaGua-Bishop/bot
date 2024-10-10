package com.li.bot.handle.key;

import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
public interface IKeyboard {

    String getKeyName();

    void execute(BotServiceImpl bot , Message message);

}
