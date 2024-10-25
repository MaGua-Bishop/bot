package com.li.bot.handle.callback;

import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class DeleteMessageCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "DeleteMessage";
    }



    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery)  {
        DeleteMessage message = DeleteMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .build();
        try {
            bot.execute(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
