package com.li.bot.handle.callback;

import com.google.common.collect.Lists;
import com.li.bot.entity.Code;
import com.li.bot.entity.Files;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
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
public class AdminDeleteCodeCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminDeleteCode";
    }

    @Autowired
    private FileService fileService;

    private InlineKeyboardMarkup createInlineKeyboardButton(int id) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("删除二维码").callbackData("admin:deletecode:" + id).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        Code codeImage = fileService.getCodeImage();
        List<Files> files = codeImage.getFiles();
        for (Files file : files) {
            SendPhoto sendPhotoRequest = new SendPhoto();
            sendPhotoRequest.setChatId(callbackQuery.getMessage().getChatId());
            InputFile inputFile = new InputFile(file.getFile_id());
            sendPhotoRequest.setPhoto(inputFile);
            sendPhotoRequest.setReplyMarkup(createInlineKeyboardButton(file.getId()));
            try {
                bot.execute(sendPhotoRequest);  // 执行发送图片的操作
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        try {
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请点击图片下方的删除按钮（会直接删除）").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

}
