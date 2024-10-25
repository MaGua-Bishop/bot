package com.li.bot.meun;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
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
public class PersonalCenterServiceMenuImpl implements IBotMenu{

    @Override
    public String getMenuName() {
        return "个人中心";
    }

    @Autowired
    private UserMapper userMapper ;

    private InlineKeyboardMarkup createInlineKeyboardButton() {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("提现").callbackData("userTakeoutMoney").build());
        buttonList.add(InlineKeyboardButton.builder().text("提现记录").callbackData("selectTakeoutMoney").build());
//        buttonList.add(InlineKeyboardButton.builder().text("发布红包记录").callbackData("selectCreateLottery").build());
        buttonList.add(InlineKeyboardButton.builder().text("领取红包记录").callbackData("selectReceiveLottery").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) {

        User from = message.getFrom();
        Long id = from.getId();
        String username = from.getUserName().isEmpty()? from.getFirstName()+ from.getLastName():from.getUserName();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        com.li.bot.entity.database.User user = userMapper.selectOne(new LambdaQueryWrapper<com.li.bot.entity.database.User>().eq(com.li.bot.entity.database.User::getTgId, id));
        if(user == null){
            return;
        }
        sendMessage.setText("个人中心\n用户名:<a href=\"tg://user?id="+id+"\">"+username+"</a>\nID:<code>"+id+"</code>\n积分:"+user.getMoney()+"");
        sendMessage.setParseMode("html");
        sendMessage.setReplyMarkup(createInlineKeyboardButton());
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
