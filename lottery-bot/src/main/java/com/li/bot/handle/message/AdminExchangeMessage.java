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
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class AdminExchangeMessage implements IMessage{

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private LotteryInfoMapper lotteryInfoMapper ;



    @Override
    public String getMessageName() {
        return "adminExchange";
    }

    private User getUser(org.telegram.telegrambots.meta.api.objects.User from){
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, from.getId()));
        if(user == null){
            user = new User() ;
            user.setTgId(from.getId());
            String name = from.getFirstName() +from.getLastName();
            user.setTgName(name);
            userMapper.insert(user);
        }
        return user;
    }

    private static String getChatInfo(long userId, BotServiceImpl bot) {
        GetChat getChat = new GetChat();
        getChat.setChatId(String.valueOf(userId));

        try {
            Chat execute = bot.execute(getChat);
            return execute.getUserName();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public synchronized void execute(BotServiceImpl bot, Message message){
        String text = message.getText();
        if(text.equals("/adminExchange")){
            return;
        }
        User user = getUser(message.getFrom());
        if(!user.getIsAdmin()){
            return;
        }
        // 定义正则表达式来匹配UUID
        String regex = "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        // 查找并打印所有匹配的UUID
        if(matcher.find()) {
            UUID uuid = UUID.fromString(matcher.group(0));
            LotteryInfo lotteryInfo = lotteryInfoMapper.selectOne(new LambdaQueryWrapper<LotteryInfo>().eq(LotteryInfo::getLotteryInfoId, String.valueOf(uuid)));
            if(lotteryInfo == null){
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("incorrect uid input error").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }

            if(lotteryInfo.getStatus() == 0){
                lotteryInfo.setStatus(1);
                lotteryInfo.setUpdateTime(LocalDateTime.now());
                lotteryInfoMapper.updateById(lotteryInfo);
                try {
                    String url = "<a href=\"tg://user?id="+lotteryInfo.getTgId()+"\">"+getChatInfo(lotteryInfo.getTgId(),bot)+"</a>" ;
                    String t = "userid:"+lotteryInfo.getTgId()+"\n"+
                            "username:"+url+"\n"+
                            "money:<b>"+lotteryInfo.getMoney()+"</b>"+"\n\n"+
                            "lucky draw successful";
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text(t).parseMode("html").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }else {
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("has been exchanged").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
}
