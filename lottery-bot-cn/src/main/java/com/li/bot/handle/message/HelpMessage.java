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
            String name =from.getFirstName() + from.getLastName();
            user.setTgName(name);
            user.setTgUserName(from.getUserName());
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
            String text = "\uD83E\uDD16欢迎使用Bot，请输入以下命令\n" +
                    "/start uid-开始抽奖\n" +
                    "/view uid-查看获奖用户\n\n" +
                    "/exchange uid - 管理员兑换奖励\n\n" +
                    "功能:\n" +
                    "1. 管理员在群聊中发送 <b>gift money number</b> 创建抽奖\n" +
                    "2. 群组成员点击按钮开始抽奖\n" +
                    "3. 管理员私人聊天机器人发送 <b>/view uid</b>查看获奖用户";
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text(text).parseMode("html").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return;
    }
}
