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
import com.li.bot.sessions.UserCreateLotterySessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
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
public class AdminCreateLotteryMessage implements IMessage {

    @Override
    public String getMessageName() {
        return "adminCreateLotteryMessage";
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private LotteryMapper lotteryMapper;

    @Autowired
    private PrizePoolService prizePoolService;

    @Autowired
    private UserCreateLotterySessionList userCreateLotterySessionList ;


        private InlineKeyboardMarkup createInlineKeyboardButton() {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("无需条件").callbackData("admin:set:lottery:condition:type:0").build());
        buttonList.add(InlineKeyboardButton.builder().text("加入群聊").callbackData("admin:set:lottery:condition:type:1").build());
        buttonList.add(InlineKeyboardButton.builder().text("订阅频道").callbackData("admin:set:lottery:condition:type:2").build());
        List<List<InlineKeyboardButton>> list = Lists.partition(buttonList, 1);
        return InlineKeyboardMarkup.builder().keyboard(list).build();
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        String text = message.getText();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, message.getFrom().getId()));
        if(user == null){
            user = new User();
            org.telegram.telegrambots.meta.api.objects.User from = message.getFrom();
            user.setTgId(from.getId());
            String firstName = message.getFrom().getFirstName();
            String lastName = message.getFrom().getLastName();
            user.setTgName(firstName + (lastName != null ? lastName : ""));
            user.setTgUserName(from.getUserName());
            userMapper.insert(user);
            return;
        }
        if(!user.getIsAdmin()){
            return;
        }
        Pattern pattern = Pattern.compile("adminGift\\s+([0-9]+(?:\\.\\d+)?)\\s+([0-9]+(?:\\.\\d+)?)\\s+(\\d+)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            // 提取金额和个数
            try {
                BigDecimal money = new BigDecimal(matcher.group(1));
                BigDecimal virtuaMoney = new BigDecimal(matcher.group(2));
                int count = Integer.parseInt(matcher.group(3));
                if (count <= 0 || money.compareTo(BigDecimal.ZERO) <= 0) {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("积分输入错误或个数输入错误").build());
                    return;
                }

                if (user.getMoney().compareTo(BigDecimal.ZERO) <= 0 || user.getMoney().subtract(money).compareTo(BigDecimal.ZERO) <0) {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("创建抽奖失败\n您当前积分:<b>"+user.getMoney()+"</b>\n积分不足\n请充值后使用").parseMode("html").build());
                    return;
                }

                // 插入数据库
                Lottery lottery = new Lottery();
                String uuid = IdUtil.randomUUID();
                lottery.setLotteryId(uuid);
                lottery.setTgId(message.getFrom().getId());
                GetChat getChat = new GetChat();
                getChat.setChatId(lottery.getTgId());
                String firstName = message.getFrom().getFirstName();
                String lastName = message.getFrom().getLastName();
                lottery.setTgName(firstName + (lastName != null ? lastName : ""));
                lottery.setChatId(message.getChatId());
                lottery.setMoney(money);
                lottery.setVirtuaMoney(virtuaMoney);
                lottery.setNumber(count);
                lottery.setStatus(LotteryStatus.START.getCode());
                int index = lotteryMapper.insert(lottery);
                if (index == 1) {
                    SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text("请选择需要设置抢红包的条件").parseMode("html").build();
                    sendMessage.setReplyMarkup(createInlineKeyboardButton());
                    bot.execute(sendMessage);
                    userCreateLotterySessionList.addUserSession(message.getFrom().getId(), uuid, -1);
                } else {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("创建抽奖失败，请重新创建").build());
                }
            } catch (NumberFormatException e) {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("输入的积分不正确").build());
            }
        } else {
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("输入格式不正确\n格式:gift 积分 个数").parseMode("html").build());
        }

    }
}
