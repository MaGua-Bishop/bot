package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Luckydraw;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.LuckydrawIdMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.UserTakeoutSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class UserDailyDrawCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "userDailyDraw";
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LuckydrawIdMapper luckydrawIdMapper;


    private User getUser(org.telegram.telegrambots.meta.api.objects.User from) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, from.getId()));
        if (user == null) {
            user = new User();
            user.setTgId(from.getId());
            String firstName = from.getFirstName();
            String lastName = from.getLastName();
            user.setTgName(firstName + (lastName != null ? lastName : ""));
            userMapper.insert(user);
        }
        return user;
    }


    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {
        try {
            bot.execute(DeleteMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        User user = getUser(callbackQuery.getFrom());
        BigDecimal userMoney = user.getMoney();

        // 判断用户是否有10的余额
        if (userMoney.compareTo(BigDecimal.TEN) >= 0) {
            // 检查用户是否已经参与当天的抽奖
            LocalDate currentDate = LocalDate.now();
            int participationCount = luckydrawIdMapper.countByTgIdAndDate(user.getTgId(), currentDate);

            if (participationCount > 0) {
                SendMessage sendMessage = SendMessage.builder()
                        .chatId(callbackQuery.getMessage().getChatId())
                        .text("您今天已经参与过抽奖了！")
                        .parseMode("html")
                        .build();
                try {
                    bot.execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            // 扣除用户的10积分
            user.setMoney(userMoney.subtract(BigDecimal.TEN));
            userMapper.updateById(user);

            // 获取当前日期和时间
            LocalDateTime currentDateTime = LocalDateTime.now();
            LocalDateTime lotteryDateTime;

            // 判断当前时间是否大于当天晚上21:59
            if (currentDateTime.toLocalTime().isAfter(LocalTime.of(21, 59))) {
                lotteryDateTime = currentDateTime.plusDays(1).withHour(22).withMinute(0); // 第二天的22:00
            } else {
                lotteryDateTime = currentDateTime.withHour(22).withMinute(0); // 今天的22:00
            }

            // 格式化开奖时间
            String formattedDateTime = lotteryDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            // 创建新的Luckydraw对象
            Luckydraw luckydraw = new Luckydraw();
            luckydraw.setTgId(user.getTgId());
            org.telegram.telegrambots.meta.api.objects.User from = callbackQuery.getFrom();
            luckydraw.setTgFullName(from.getFirstName() + (from.getLastName() != null ? from.getLastName() : ""));
            luckydraw.setTgUserName(callbackQuery.getFrom().getUserName());
            luckydraw.setLuckydrawTime(lotteryDateTime); // 设置开奖时间

            int insert = luckydrawIdMapper.insert(luckydraw);
            if (insert > 0) {
                SendMessage sendMessage = SendMessage.builder()
                        .chatId(callbackQuery.getMessage().getChatId())
                        .text("参与成功\n开奖时间: " + formattedDateTime)
                        .build();
                try {
                    bot.execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else {
                SendMessage sendMessage = SendMessage.builder()
                        .chatId(callbackQuery.getMessage().getChatId())
                        .text("参与失败")
                        .parseMode("html")
                        .build();
                try {
                    bot.execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }

        } else {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .text("积分不足\n当前积分:<b>" + user.getMoney() + "</b>")
                    .parseMode("html")
                    .build();
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
