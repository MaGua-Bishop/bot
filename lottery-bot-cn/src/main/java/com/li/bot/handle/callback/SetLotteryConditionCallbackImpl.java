package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.Takeout;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.LotteryMapper;
import com.li.bot.mapper.TakeoutMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.PrizePoolService;
import com.li.bot.sessions.UserCreateLotterySession;
import com.li.bot.sessions.UserCreateLotterySessionList;
import com.li.bot.sessions.enums.UserCreateLotterySessionState;
import com.li.bot.utils.BotSendMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class SetLotteryConditionCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "setLotteryCondition";
    }

    @Autowired
    private TakeoutMapper takeoutMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PrizePoolService prizePoolService;

    @Autowired
    private LotteryMapper lotteryMapper;
    @Autowired
    private BotConfig botConfig;
    @Autowired
    private UserCreateLotterySessionList userCreateLotterySessionList;

    private InlineKeyboardMarkup createInlineKeyboardButton(String uid) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("点击抽奖").url("https://" + botConfig.getBotname() + "?start=" + uid.toString()).build());
//        buttonList.add(InlineKeyboardButton.builder().text("茶社大群").url("https://t.me/chashe666666").build());
//        buttonList.add(InlineKeyboardButton.builder().text("供需发布").url("https://t.me/chashe1_Bot").build());
//        buttonList.add(InlineKeyboardButton.builder().text("供需频道").url("https://t.me/chashe0").build());
//        buttonList.add(InlineKeyboardButton.builder().text("TRX兑换").url("https://t.me/AutoTronTRXbot").build());
//        // 创建行列表
//        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
//        // 将前两个按钮放在第一行
//        List<InlineKeyboardButton> firstRow = new ArrayList<>();
//        firstRow.add(buttonList.get(0));
//        rowList.add(firstRow);
//        // 将剩余的按钮按每两个一组分组
//        for (int i = 1; i < buttonList.size(); i += 2) {
//            List<InlineKeyboardButton> row = new ArrayList<>();
//            row.add(buttonList.get(i));
//            if (i + 1 < buttonList.size()) {
//                row.add(buttonList.get(i + 1));
//            }
//            rowList.add(row);
//        }
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);
        // 构建并返回InlineKeyboardMarkup对象
        return InlineKeyboardMarkup.builder().keyboard(rowList).build();
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();

        //正则解析
        String regex = "set:lottery:condition:type:(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);

        if (matcher.matches()) {
            //获取type和takeoutId
            Integer type = Integer.valueOf(matcher.group(1));
            UserCreateLotterySession userCreateLotterySession = userCreateLotterySessionList.getUserTakeoutSession(callbackQuery.getFrom().getId());
            String uid = userCreateLotterySession.getUid();
            Lottery lottery = lotteryMapper.selectOne(new LambdaQueryWrapper<Lottery>().eq(Lottery::getLotteryId, uid));
            if (Objects.isNull(lottery)) {
                return;
            }
            if (type == 0) {
                prizePoolService.add(lottery.getLotteryId(), lottery.getMoney(), lottery.getNumber());
                Message execute = bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(BotSendMessageUtils.createLotteryMessage(lottery.getMoney(), lottery.getNumber(), lottery.getLotteryId())).parseMode("html").disableWebPagePreview(true).replyMarkup(createInlineKeyboardButton(lottery.getLotteryId())).build());
                Integer messageId = execute.getMessageId();
                lottery.setMessageId(Long.valueOf(messageId));
                lotteryMapper.updateById(lottery);
                User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, lottery.getTgId()));
                user.setMoney(user.getMoney().subtract(lottery.getMoney()));
                userMapper.updateById(user);
                userCreateLotterySessionList.removeUserSession(callbackQuery.getFrom().getId());
            } else if (type == 1) {
                SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请输入群聊邀请链接\n⚠\uFE0F注意: \n1.群聊必须是<b>公开群聊</b>\n2.需要把机器人添加<b>到群聊</b>并<b>给管理员权限</b>,否则抽奖无效").parseMode("html").build();
                bot.execute(sendMessage);
                userCreateLotterySessionList.updateUserSession(callbackQuery.getFrom().getId(), UserCreateLotterySessionState.WAITING_FOR_USER_MESSAGE,1,false);
            } else if (type == 2) {
                SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请输入频道邀请链接\n⚠\uFE0F注意: 需要把机器人添加<b>到频道</b>并<b>给管理员权限</b>,否则抽奖无效").parseMode("html").build();
                bot.execute(sendMessage);
                userCreateLotterySessionList.updateUserSession(callbackQuery.getFrom().getId(), UserCreateLotterySessionState.WAITING_FOR_USER_MESSAGE,2,false);
            }
        }
    }
}
