package com.li.bot.handle.message;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.LotteryInfo;
import com.li.bot.entity.database.PrizePool;
import com.li.bot.entity.database.User;
import com.li.bot.enums.LotteryStatus;
import com.li.bot.mapper.LotteryInfoMapper;
import com.li.bot.mapper.LotteryMapper;
import com.li.bot.mapper.PrizePoolMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.PrizePoolService;
import com.li.bot.utils.UserStartKeyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class StartMessage implements IMessage{

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private LotteryInfoMapper lotteryInfoMapper ;

    @Autowired
    private PrizePoolService prizePoolService;
    @Autowired
    private PrizePoolMapper prizePoolMapper;

    @Autowired
    private LotteryMapper lotteryMapper;


    @Override
    public String getMessageName() {
        return "start";
    }
    private User getUser(org.telegram.telegrambots.meta.api.objects.User from){
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, from.getId()));
        if(user == null){
            user = new User() ;
            user.setTgId(from.getId());
            String name = from.getFirstName() +from.getLastName();
            user.setTgName(name);
            user.setTgUserName(from.getUserName());
            userMapper.insert(user);
        }
        return user;
    }

    private String getChatUrl(String name) {
        name = name.replace("@","");
//        String url = "<a href=\"tg://user?id="+lotteryInfo.getTgId()+"\">"+getChatInfo(lotteryInfo.getTgId(),bot)+"</a>" ;
        return "<a  href=\"https://t.me/"+name+"\">@"+name+"</a>";
    }

    private ReplyKeyboardMarkup createReplyKeyboardMarkup(){
        List<String> userStartKey = UserStartKeyUtils.userStartKeyList;
        List<KeyboardButton> keyList = new ArrayList<>();
        userStartKey.forEach(key -> {
            KeyboardButton button = KeyboardButton.builder().text(key).build();
            keyList.add(button);
        });
        List<List<KeyboardButton>> partition = Lists.partition(keyList, 2);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        partition.forEach(p -> {
            KeyboardRow row = new KeyboardRow(p);
            keyboardRows.add(row);
        });
        ReplyKeyboardMarkup replyKeyboardMarkup = ReplyKeyboardMarkup.builder().keyboard(keyboardRows).build();
        replyKeyboardMarkup.setResizeKeyboard(true);
        return replyKeyboardMarkup;
    }

    @Override
    public synchronized void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        String text = message.getText();
        if(text.equals("/start")){
            getUser(message.getFrom());
            SendMessage executeMessage = SendMessage.builder().replyMarkup(createReplyKeyboardMarkup()).text("hello").chatId(message.getChatId().toString()).build();
            try {
                bot.execute(executeMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        String lotteryId = text.replace("/start ","");
        System.out.println("获取到当前抽奖id:"+text);

        //获取用户数据
        org.telegram.telegrambots.meta.api.objects.User from = message.getFrom();
        User user = getUser(from);

        //判断用户是否抽过该奖（抽过）
        LotteryInfo lotteryInfo = lotteryInfoMapper.selectOne(new LambdaQueryWrapper<LotteryInfo>().eq(LotteryInfo::getTgId, user.getTgId()).eq(LotteryInfo::getLotteryId, lotteryId));
        if(lotteryInfo != null){
            bot.execute(SendMessage.builder().chatId(message.getChatId()).replyMarkup(createReplyKeyboardMarkup()).text("You have already participated in this lottery").build());
            return;
        }

        boolean b = new Random().nextBoolean();
        System.out.println("用户是否中奖:"+b);
        if(!b){
            lotteryInfo = new LotteryInfo();
            lotteryInfo.setLotteryId(lotteryId);
            lotteryInfo.setTgId(user.getTgId());
            String uuid = IdUtil.randomUUID();
            lotteryInfo.setStatus(-1);
            lotteryInfo.setTgName(user.getTgName());
            lotteryInfo.setLotteryInfoId(uuid);
            lotteryInfoMapper.insert(lotteryInfo);
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("unfortunately, you did not win the prize").replyMarkup(createReplyKeyboardMarkup()).build());
            return;
        }

//        //判断抽奖是否结束（结束）
//        boolean b = prizePoolService.containsKey(lotteryId);
//        if(!b){
//            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("The lottery has ended or does not exist").build());
//            //更新状态
//            Lottery lottery = lotteryMapper.selectOne(new LambdaQueryWrapper<Lottery>().eq(Lottery::getLotteryId, lotteryId));
//            lottery.setStatus(LotteryStatus.END.getCode());
//            lottery.setUpdateTime(LocalDateTime.now());
//            lotteryMapper.updateById(lottery);
//            //更新消息状态
//            EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(lottery.getChatId()).messageId(Integer.valueOf(String.valueOf(lottery.getMessageId()))).replyMarkup(createInlineKeyboardButton()).build();
//            try {
//                bot.execute(editMessageReplyMarkup);
//            } catch (TelegramApiException e) {
//                throw new RuntimeException(e);
//            }
//            return;
//        }

        //判断奖池是否有剩余金额（没有）
        String randomId = prizePoolService.getRandomMoney(lotteryId);
        if(randomId == null){
            bot.execute(SendMessage.builder().chatId(message.getChatId()).replyMarkup(createReplyKeyboardMarkup()).text("The lottery has ended or does not exist").build());
            return;
        }
        PrizePool prizePool = prizePoolMapper.selectOne(new LambdaQueryWrapper<PrizePool>().eq(PrizePool::getPrizePoolId, randomId).eq(PrizePool::getStatus, 0));
        if(prizePool == null){
            lotteryInfo = new LotteryInfo();
            lotteryInfo.setLotteryId(lotteryId);
            lotteryInfo.setTgId(user.getTgId());
            String uuid = IdUtil.randomUUID();
            lotteryInfo.setTgName(user.getTgName());
            lotteryInfo.setStatus(-1);
            lotteryInfo.setLotteryInfoId(uuid);
            lotteryInfoMapper.insert(lotteryInfo);
            bot.execute(SendMessage.builder().chatId(message.getChatId()).replyMarkup(createReplyKeyboardMarkup()).text("unfortunately, you did not win the prize").build());
            return;
        }

        lotteryInfo = new LotteryInfo();
        lotteryInfo.setLotteryId(lotteryId);
        lotteryInfo.setTgId(user.getTgId());
        lotteryInfo.setPrizePoolId(prizePool.getPrizePoolId());
        lotteryInfo.setMoney(prizePool.getMoney());
        lotteryInfo.setTgName(user.getTgName());
        String uuid = IdUtil.randomUUID();
        lotteryInfo.setLotteryInfoId(uuid);
        int index = lotteryInfoMapper.insert(lotteryInfo);
        prizePool.setStatus(1);
        int index1 = prizePoolMapper.updateById(prizePool);
        if(index == 1 && index1 == 1){
            SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).replyMarkup(createReplyKeyboardMarkup()).text("Congratulations, you got it <b>₦" + prizePool.getMoney() + "</b>!\n" + "save lottery id:\n<code>" + lotteryInfo.getLotteryInfoId()+"</code>").parseMode("html").build();
            bot.execute(sendMessage);
//            String chatUrl = getChatUrl("@Nana_77nggame");
            String str = "Congratulations, you've won! \uD83C\uDF89\n" +
                    "Please contact our online customer service and send your lottery ID to claim your reward.";
            SendMessage message1 = SendMessage.builder().chatId(message.getChatId()).text(str).parseMode("html").replyMarkup(createReplyKeyboardMarkup()).disableWebPagePreview(true).build();
            bot.execute(message1);
        }


    }
}
