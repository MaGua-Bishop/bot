package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.LotteryInfo;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.LotteryInfoMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class ExchangeMessage implements IMessage{

    @Autowired
    private LotteryInfoMapper lotteryInfoMapper ;
    @Autowired
    private UserMapper userMapper ;

    @Override
    public String getMessageName() {
        return "exchange";
    }


    @Override
    public synchronized void execute(BotServiceImpl bot, Message message){
        String text = message.getText();
        if(text.equals("/Exchange")){
            return;
        }
        // 定义正则表达式来匹配UUID
        String regex = "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        // 查找并打印所有匹配的UUID
        if(matcher.find()) {
            UUID uuid = UUID.fromString(matcher.group(0));
            LotteryInfo lotteryInfo = lotteryInfoMapper.selectOne(new LambdaQueryWrapper<LotteryInfo>().eq(LotteryInfo::getLotteryInfoId, String.valueOf(uuid)).eq(LotteryInfo::getLotteryCreateTgId,message.getFrom().getId()));
            if(lotteryInfo == null){
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("不属于您的抽奖或uid输入错误").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            if(lotteryInfo.getStatus() == 0){
                //改成已兑换
                lotteryInfo.setStatus(1);
                lotteryInfo.setUpdateTime(LocalDateTime.now());
                lotteryInfoMapper.updateById(lotteryInfo);

                User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, lotteryInfo.getTgId()));
                if(user == null){
                    try {
                        bot.execute(SendMessage.builder().chatId(message.getChatId()).text("用户不存在").build());
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }
                //增加用户积分
                user.setMoney(user.getMoney().add(lotteryInfo.getMoney()));
                userMapper.updateById(user);
                try {
                    String url = "<a href=\"tg://user?id="+lotteryInfo.getTgId()+"\">"+lotteryInfo.getTgName()+"</a>" ;
                    String t = "用户id:"+lotteryInfo.getTgId()+"\n"+
                            "用户名:"+url+"\n"+
                            "抽中:<b>"+lotteryInfo.getMoney()+"</b>"+"\n\n"+
                            "核销成功";
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text(t).parseMode("html").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }else {
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("⚠\uFE0F已核销过了").parseMode("html").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
}
