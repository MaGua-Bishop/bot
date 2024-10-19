package com.li.bot.handle.message;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.LotteryInfo;
import com.li.bot.entity.database.PrizePool;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.LotteryInfoMapper;
import com.li.bot.mapper.LotteryMapper;
import com.li.bot.mapper.PrizePoolMapper;
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
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class ViewWinningUserMessage implements IMessage{

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private LotteryInfoMapper lotteryInfoMapper ;

    @Autowired
    private LotteryMapper lotteryMapper;


    @Override
    public String getMessageName() {
        return "viewWinningUser";
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("ended").callbackData("null").build());

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private User getUser(org.telegram.telegrambots.meta.api.objects.User from){
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, from.getId()));
        if(user == null){
            user = new User() ;
            user.setTgId(from.getId());
            String name = from.getUserName() != null ? from.getUserName() : from.getFirstName() +from.getLastName();
            user.setTgName(name);
            userMapper.insert(user);
        }
        return user;
    }

    @Override
    public synchronized void execute(BotServiceImpl bot, Message message){
        String text = message.getText();
        if(text.equals("/view")){
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
        if(matcher.find()){
            UUID uuid = UUID.fromString(matcher.group(0));
            Lottery lottery = lotteryMapper.selectOne(new LambdaQueryWrapper<Lottery>().eq(Lottery::getLotteryId, String.valueOf(uuid)));
            if(lottery != null){
                BigDecimal totalMoney = lottery.getMoney();
                List<LotteryInfo> lotteryInfoList = lotteryInfoMapper.getLotteryInfoList(String.valueOf(uuid));
                BigDecimal bigDecimal = new BigDecimal(0);
                if(!lotteryInfoList.isEmpty()){
                    bigDecimal = lotteryInfoList.stream().map(LotteryInfo::getMoney).reduce(BigDecimal::add).get();
                }
                lotteryInfoList.sort((o1, o2) -> o2.getMoney().compareTo(o1.getMoney()));
                totalMoney = totalMoney.subtract(bigDecimal);
                SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text(BotSendMessageUtils.adminQueryMessage(lottery, lotteryInfoList, totalMoney, bot)).parseMode("html").build();
                try {
                    bot.execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }else {
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("incorrect uid input error").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }else {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("incorrect uid input error").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }





    }
}
