package com.li.bot.handle.callback;

import com.google.common.collect.Lists;
import com.li.bot.entity.Code;
import com.li.bot.entity.Files;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.sessions.AdminEditSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class AdminUpdateRechargeCopyCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminUpdateRechargeCopy";
    }
    @Autowired
    private AdminEditSessionList adminEditSessionList ;




    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {

        try {
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请输入新的人民币充值文案（需提醒用户直接输入充值金额）").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        adminEditSessionList.addUserSession(callbackQuery.getFrom().getId(), null, 3);
    }

}
