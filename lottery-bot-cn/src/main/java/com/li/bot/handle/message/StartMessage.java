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
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
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

    private InlineKeyboardMarkup createInlineKeyboardButton(String link) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("加入").url(link).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private Long getChannelId(String link,BotServiceImpl bot) {
        link = link.substring(link.lastIndexOf("/") + 1);
        GetChat getChat = new GetChat();
        getChat.setChatId("@" + link);
        Chat execute = null;
        try {
            execute = bot.execute(getChat);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return execute.getId();
    }


    private Boolean isUserAndChannelMember(Message message,BotServiceImpl bot,String link) {
        Long tgId = message.getFrom().getId();
        Long chatId = getChannelId(link, bot);
        try {
            ChatMember member = bot.execute(GetChatMember.builder().chatId(chatId).userId(Long.valueOf(tgId)).build());
            if (!member.getStatus().equals("left")) {
                return true;
            } else {
                bot.execute(SendMessage.builder().chatId(message.getChatId().toString()).text("请加入指定群聊/频道,再抢红包").replyMarkup(createInlineKeyboardButton(link)).build());
                return false;
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean isUserAndGroupMember(Message message,BotServiceImpl bot,String link) {
        Long tgId = message.getFrom().getId();
        Long chatId = getChannelId(link, bot);
        try {
            ChatMember member = bot.execute(GetChatMember.builder().chatId(chatId).userId(Long.valueOf(tgId)).build());
            if (!member.getStatus().equals("left")) {
                return true;
            } else {
                bot.execute(SendMessage.builder().chatId(message.getChatId().toString()).text("请加入指定群聊/频道,再抢红包").replyMarkup(createInlineKeyboardButton(link)).build());
                return false;
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
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

    private void noWinningUpdate(Lottery lottery,User user){
        LotteryInfo lotteryInfo = new LotteryInfo();
        lotteryInfo.setLotteryId(lottery.getLotteryId());
        lotteryInfo.setTgId(user.getTgId());
        String uuid = IdUtil.randomUUID();
        lotteryInfo.setStatus(-1);
        lotteryInfo.setTgName(user.getTgName());
        lotteryInfo.setLotteryInfoId(uuid);
        lotteryInfo.setLotteryCreateTgId(lottery.getTgId());
        lotteryInfoMapper.insert(lotteryInfo);
    }

    @Override
    public synchronized void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        String text = message.getText();
        //只是启动机器人
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

        //判断用户是否满足条件
        Lottery lottery = lotteryMapper.selectOne(new LambdaQueryWrapper<Lottery>().eq(Lottery::getLotteryId, lotteryId).eq(Lottery::getStatus, LotteryStatus.START.getCode()));
        if(lottery == null){
            bot.execute(SendMessage.builder().chatId(message.getChatId()).replyMarkup(createReplyKeyboardMarkup()).text("抽奖已结束").build());
            return;
        }
        if(lottery.getLink() != null && lottery.getLinkType() == 1){
            Boolean userMember = isUserAndGroupMember(message, bot, lottery.getLink());
            if(!userMember){
                return;
            }
        }

        if(lottery.getLink() != null && lottery.getLinkType() == 2){
            Boolean userMember = isUserAndChannelMember(message, bot, lottery.getLink());
            if(!userMember){
                return;
            }
        }



        //奖池没有剩余金额
        String randomId = prizePoolService.getRandomMoney(lotteryId);
        if(randomId == null){
            bot.execute(SendMessage.builder().chatId(message.getChatId()).replyMarkup(createReplyKeyboardMarkup()).text("抽奖已结束").build());
            return;
        }
        //获取用户数据
        org.telegram.telegrambots.meta.api.objects.User from = message.getFrom();
        User user = getUser(from);
        //判断用户是否抽过该奖
        LotteryInfo lotteryInfo = lotteryInfoMapper.selectOne(new LambdaQueryWrapper<LotteryInfo>().eq(LotteryInfo::getTgId, user.getTgId()).eq(LotteryInfo::getLotteryId, lotteryId));
        if(lotteryInfo != null){
            bot.execute(SendMessage.builder().chatId(message.getChatId()).replyMarkup(createReplyKeyboardMarkup()).text("你已经参加了这个抽奖").build());
            return;
        }
        //判断用户是否中奖
        boolean b = new Random().nextBoolean();
        System.out.println("用户是否中奖:"+b);
        if(!b){
            noWinningUpdate(lottery,user);
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("不幸的是，你没有获奖").replyMarkup(createReplyKeyboardMarkup()).build());
            return;
        }
        //判断奖励是否存在
        PrizePool prizePool = prizePoolMapper.selectOne(new LambdaQueryWrapper<PrizePool>().eq(PrizePool::getPrizePoolId, randomId).eq(PrizePool::getStatus, 0));
        if(prizePool == null){
            noWinningUpdate(lottery,user);
            bot.execute(SendMessage.builder().chatId(message.getChatId()).replyMarkup(createReplyKeyboardMarkup()).text("不幸的是，你没有获奖").build());
            return;
        }

        lotteryInfo = new LotteryInfo();
        lotteryInfo.setLotteryId(lotteryId);
        lotteryInfo.setTgId(user.getTgId());
        lotteryInfo.setPrizePoolId(prizePool.getPrizePoolId());
        lotteryInfo.setMoney(prizePool.getMoney());
        lotteryInfo.setTgName(user.getTgName());
        lotteryInfo.setLotteryCreateTgId(lottery.getTgId());
        String uuid = IdUtil.randomUUID();
        lotteryInfo.setLotteryInfoId(uuid);
        int index = lotteryInfoMapper.insert(lotteryInfo);
        prizePool.setStatus(1);
        int index1 = prizePoolMapper.updateById(prizePool);
        if(index == 1 && index1 == 1){
            SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).replyMarkup(createReplyKeyboardMarkup()).text("恭喜你，你得到了 <b>" + prizePool.getMoney() + "</b>!\n" + "保存抽奖 id:\n<code>" + lotteryInfo.getLotteryInfoId()+"</code>").parseMode("html").build();
            bot.execute(sendMessage);
            String str = "恭喜你，你中了! \uD83C\uDF89\n" +
                    "请联系<a href=\"tg://user?id="+lottery.getTgId()+"\">@"+lottery.getTgName()+"</a>并发送您的中奖ID以索取奖励。";
            SendMessage message1 = SendMessage.builder().chatId(message.getChatId()).text(str).parseMode("html").replyMarkup(createReplyKeyboardMarkup()).disableWebPagePreview(true).build();
            bot.execute(message1);
        }
    }
}
