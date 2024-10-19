package com.li.bot.handle.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class CustomerServiceMenuImpl implements IBotMenu{

    @Override
    public String getMenuName() {
        return "联系客服";
    }

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private BusinessMapper businessMapper ;

    @Override
    public void execute(BotServiceImpl bot, Message message) {
//        InlineKeyboardMarkup inlineKeyboardButton = createInlineKeyboardButton(message);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText("待开发");
        sendMessage.enableMarkdownV2(true);
//        sendMessage.setReplyMarkup(inlineKeyboardButton);
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
