package com.li.bot.handle.callback;

import com.li.bot.entity.Code;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.sessions.UserCodeRechargeSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class UserCodeRechargeCallbackImpl implements ICallback {


    @Autowired
    private UserCodeRechargeSessionList userCodeRechargeSessionList;
    @Autowired
    private FileService fileService;

    @Override
    public String getCallbackName() {
        return "userCodeRecharge";
    }

    @Override
    @Transactional
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        Code codeImage = fileService.getCodeImage();

        bot.execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text(codeImage.getText()).build());
        userCodeRechargeSessionList.addUserSession(callbackQuery.getFrom().getId());

    }
}
