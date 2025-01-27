package com.li.bot.handle.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
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
public class RechargeServiceMenuImpl implements IBotMenu {

    @Override
    public String getMenuName() {
        return "充值";
    }

    @Autowired
    private UserMapper userMapper;


//    private InlineKeyboardMarkup createInlineKeyboardButton() {
//        List<InlineKeyboardButton> buttonList = new ArrayList<>();
//        buttonList.add(InlineKeyboardButton.builder().text("100U").callbackData("user:recharge:100").build());
//        buttonList.add(InlineKeyboardButton.builder().text("200U").callbackData("user:recharge:200").build());
//        buttonList.add(InlineKeyboardButton.builder().text("300U").callbackData("user:recharge:300U").build());
//        buttonList.add(InlineKeyboardButton.builder().text("500U").callbackData("user:recharge:500U").build());
//        buttonList.add(InlineKeyboardButton.builder().text("1000U").callbackData("user:recharge:1000U").build());
//        buttonList.add(InlineKeyboardButton.builder().text("2000U").callbackData("user:recharge:2000U").build());
//        buttonList.add(InlineKeyboardButton.builder().text("扫码充值").callbackData("user:code:recharge").build());
//        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 3);
//        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
//        return inlineKeyboardMarkup;
//    }

    private InlineKeyboardMarkup createInlineKeyboardButton() {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("USDT").callbackData("user:select:type:0").build());
        buttonList.add(InlineKeyboardButton.builder().text("人民币").callbackData("user:select:type:1").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) {

        Long tgId = message.getFrom().getId();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));
        if (user == null) {
            user = new User();
            user.setTgId(tgId);
            user.setTgName(message.getFrom().getFirstName() + message.getFrom().getLastName());
            userMapper.insert(user);
        }
        SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text("请选择您要充值的货币").parseMode("html").replyMarkup(createInlineKeyboardButton()).build();
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
