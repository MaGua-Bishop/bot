package com.li.bot.handle.callback;

import com.google.common.collect.Lists;
import com.li.bot.entity.database.Convoys;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class ReturnConvoysListCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "returnConvoysList";
    }

    @Autowired
    private ConvoysMapper convoysMapper ;

    private InlineKeyboardMarkup createInlineKeyboardButton(){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();


        List<Convoys> convoys = convoysMapper.selectList(null);
        if(convoys.isEmpty()){
            buttonList.add(InlineKeyboardButton.builder().text("暂无车队").callbackData("null").build());
        }else {
            for (Convoys convoy : convoys) {
                buttonList.add(InlineKeyboardButton.builder().text(convoy.getCopywriter()).callbackData("selectConvoysInfo:"+convoy.getConvoysId()).build());
            }
        }
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }



    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        bot.execute(EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text("请选择需要申请的车队").replyMarkup(createInlineKeyboardButton())
                .build());
    }
}
