package com.li.bot.handle.message;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.User;
import com.li.bot.enums.LotteryStatus;
import com.li.bot.mapper.LotteryMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.PrizePoolService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
@Component
public class GroupAdminLotteryMessage implements IMessage {

    @Override
    public String getMessageName() {
        return "GroupAdminLotteryMessage";
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private LotteryMapper lotteryMapper;

    @Autowired
    private PrizePoolService prizePoolService;

    private InlineKeyboardMarkup createInlineKeyboardButton(String uid) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("click lottery").url("https://" + botConfig.getBotname() + "?start=" + uid.toString()).build());

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        String text = message.getText();
        System.out.println("用户输入的:" + text);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, message.getFrom().getId()));
        if(user == null){
            user = new User();
            org.telegram.telegrambots.meta.api.objects.User from = message.getFrom();
            user.setTgId(from.getId());
            String name = from.getFirstName() + from.getLastName();
            user.setTgName(name);
            userMapper.insert(user);
            return;
        }
        if (!user.getIsAdmin()) {
            return;
        }
        Pattern pattern = Pattern.compile("gift\\s+([0-9]+(?:\\.\\d+)?)\\s+(\\d+)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            // 提取金额和个数
            try {
                BigDecimal money = new BigDecimal(matcher.group(1));
                int count = Integer.parseInt(matcher.group(2));
                if (count <= 0 || money.compareTo(BigDecimal.ZERO) <= 0) {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("incorrect amount input or Incorrect number input error").build());
                    return;
                }
                // 插入数据库
                Lottery lottery = new Lottery();
                String uuid = IdUtil.randomUUID();
                lottery.setLotteryId(uuid);
                lottery.setTgId(message.getFrom().getId());
                lottery.setChatId(message.getChatId());
                lottery.setMoney(money);
                lottery.setNumber(count);
                lottery.setStatus(LotteryStatus.START.getCode());
                int index = lotteryMapper.insert(lottery);
                if (index == 1) {
                    prizePoolService.add(uuid, money, count);
                    Message execute = bot.execute(SendMessage.builder().chatId(message.getChatId()).text(BotSendMessageUtils.createLotteryMessage(money, count, uuid)).parseMode("html").replyMarkup(createInlineKeyboardButton(lottery.getLotteryId())).build());
                    Integer messageId = execute.getMessageId();
                    lottery.setMessageId(Long.valueOf(messageId));
                    lotteryMapper.updateById(lottery);
                } else {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("create lottery failed please recreate").build());
                }
            } catch (NumberFormatException e) {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("incorrect amount input").build());
            }
        } else {
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("incorrect input format\nformat:CreateLuckyDraw amount number").parseMode("html").build());
        }

    }
}
