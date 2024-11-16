package com.li.bot.handle.callback;

import com.li.bot.entity.Code;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
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
public class admindeletecodesCallbackImpl implements ICallback {


    @Autowired
    private FileService fileService;

    @Override
    public String getCallbackName() {
        return "admindeletecode";
    }

    @Override
    @Transactional
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String id = data.substring(data.lastIndexOf(":") + 1);
        Code codeImage = fileService.getCodeImage();
        codeImage.getFiles().removeIf(file -> file.getId() == Integer.parseInt(id));
        fileService.setCodeImage(codeImage);
        bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("删除成功").build());
        bot.execute(DeleteMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .build());
    }
}
