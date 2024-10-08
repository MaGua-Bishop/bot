package com.li.bot.handle.callback;

import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class WaiverOrderCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "waiverOrder";
    }


    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        bot.execute(DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }
}
