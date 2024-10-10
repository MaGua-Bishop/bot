package com.li.bot.handle.callback;

import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
public interface ICallback {

    String getCallbackName();

    void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException;

}
