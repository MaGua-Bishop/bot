package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.LotteryInfo;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.LotteryInfoMapper;
import com.li.bot.mapper.LotteryMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotSendMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class HelpMessage implements IMessage {

    @Autowired
    private UserMapper userMapper;


    @Override
    public String getMessageName() {
        return "/help";
    }

    private User getUser(org.telegram.telegrambots.meta.api.objects.User from) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, from.getId()));
        if (user == null) {
            user = new User();
            user.setTgId(from.getId());
            String name = from.getUserName() != null ? from.getUserName() : from.getFirstName() + from.getLastName();
            user.setTgName(name);
            userMapper.insert(user);
        }
        return user;
    }

    @Override
    public synchronized void execute(BotServiceImpl bot, Message message) {
        User user = getUser(message.getFrom());
        if (!user.getIsAdmin()) {
            return;
        }
        try {
            String text = "\uD83E\uDD16Welcome to the project Bot, please enter the following command\n" +
                    "/start uid - Start the lottery\n" +
                    "/view uid - view winning users\n\n" +
                    "/exchange uid - admin exchange\n\n" +
                    "function:\n" +
                    "1. admin send in group chat <b>gift money number</b> create project\n" +
                    "2. group members click the button to start the lottery\n" +
                    "3. admin private chat bot send <b>/view uid</b> view winning users";
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text(text).parseMode("html").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return;
    }
}
