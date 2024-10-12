package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Button;
import com.li.bot.mapper.ButtonMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
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
public class DeleteButtonCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "deleteButton";
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

        String data = callbackQuery.getData();
        String id = data.substring(data.lastIndexOf(":") + 1);

        Button button = buttonMapper.selectOne(new LambdaQueryWrapper<Button>().eq(Button::getButtonId, Long.valueOf(id)));
        if(button == null){
            SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("该互推按钮已删除").parseMode("html").build();
            bot.execute(sendMessage);
            return;
        }

        buttonMapper.deleteById(button);

        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(""+button.getName()+"按钮删除成功").parseMode("html").build();
        bot.execute(sendMessage);

        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createInlineKeyboardButton()).build();
        bot.execute(editMessageReplyMarkup);

    }
}
