package com.li.bot.handle.message;

import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
public interface IMessage {

    String getMessageName();

    void execute(BotServiceImpl bot, Message message) throws TelegramApiException;

}
