package com.li.bot.handle.callback;

import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.UserCodeRechargeSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class UserCancelCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "userCancel";
    }

    @Autowired
    private UserCodeRechargeSessionList userCodeRechargeSessionList;

    @Override
    @Transactional
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        bot.execute(DeleteMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .build());
        userCodeRechargeSessionList.removeUserSession(callbackQuery.getFrom().getId());


    }
}
