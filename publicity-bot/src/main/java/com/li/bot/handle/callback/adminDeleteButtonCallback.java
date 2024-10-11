package com.li.bot.handle.callback;

import com.google.common.collect.Lists;
import com.li.bot.entity.database.Button;
import com.li.bot.entity.database.Convoys;
import com.li.bot.mapper.ButtonMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
public class adminDeleteButtonCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminDeleteButton";
    }

    @Autowired
    private ButtonMapper buttonMapper ;

    private InlineKeyboardMarkup createInlineKeyboardButton(){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        List<Button> buttons = buttonMapper.selectList(null);
        if(buttons.isEmpty()){
            buttonList.add(InlineKeyboardButton.builder().text("暂无互推按钮").callbackData("null").build());
        }else {
            buttons.forEach(button -> {
                buttonList.add(InlineKeyboardButton.builder().text(button.getName()).callbackData("deleteButton:"+button.getButtonId()).build());
            });
        }
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请点击要删除互推导航按钮(点击按钮直接删除)").replyMarkup(createInlineKeyboardButton()).parseMode("html").build();
        bot.execute(sendMessage);
    }
}
