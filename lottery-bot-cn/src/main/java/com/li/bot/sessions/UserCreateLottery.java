package com.li.bot.sessions;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.Takeout;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.LotteryMapper;
import com.li.bot.mapper.TakeoutMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.meun.BotMenuFactory;
import com.li.bot.meun.IBotMenu;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.service.impl.PrizePoolService;
import com.li.bot.sessions.enums.UserCreateLotterySessionState;
import com.li.bot.sessions.enums.UserTakeoutSessionState;
import com.li.bot.utils.BotSendMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
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
@Slf4j
public class UserCreateLottery {

    private BotServiceImpl bot;

    private Message message;

    private UserCreateLotterySessionList userCreateLotterySessionList;


    private UserMapper userMapper;

    private TakeoutMapper takeoutMapper;


    private LotteryMapper lotteryMapper;

    private PrizePoolService prizePoolService;

    private BotConfig botConfig;


    public UserCreateLottery(BotServiceImpl bot, Message message, UserCreateLotterySessionList userCreateLotterySessionList, UserMapper userMapper, TakeoutMapper takeoutMapper, LotteryMapper lotteryMapper, PrizePoolService prizePoolService, BotConfig botConfig) {
        this.bot = bot;
        this.message = message;
        this.userCreateLotterySessionList = userCreateLotterySessionList;
        this.userMapper = userMapper;
        this.takeoutMapper = takeoutMapper;
        this.lotteryMapper = lotteryMapper;
        this.prizePoolService = prizePoolService;
        this.botConfig = botConfig;
    }

    public void execute(BotMenuFactory botMenuFactory) {
        IBotMenu menu = botMenuFactory.getMenu(message.getText());
        if (menu != null) {
            userCreateLotterySessionList.removeUserSession(message.getChatId());
            menu.execute(bot, message);
            return;
        }
        UserCreateLotterySession userSession = userCreateLotterySessionList.getUserTakeoutSession(message.getFrom().getId());
        UserCreateLotterySessionState state = userSession.getState();
        switch (state) {
            case WAITING_FOR_USER_MESSAGE:
                handleUserMessageInput(message, userSession);
                break;
            default:
                break;
        }
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(String uid, String name, String link) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).url(link).build());
        buttonList.add(InlineKeyboardButton.builder().text("点击抽奖").url("https://" + botConfig.getBotname() + "?start=" + uid.toString()).build());
        buttonList.add(InlineKeyboardButton.builder().text("茶社大群").url("https://t.me/chashe666666").build());
        buttonList.add(InlineKeyboardButton.builder().text("供需发布").url("https://t.me/chashe1_Bot").build());
        buttonList.add(InlineKeyboardButton.builder().text("供需频道").url("https://t.me/chashe0").build());
        buttonList.add(InlineKeyboardButton.builder().text("TRX兑换").url("https://t.me/AutoTronTRXbot").build());

        // 创建行列表
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        // 将第一个按钮放在第一行
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        firstRow.add(buttonList.get(0));
        rowList.add(firstRow);

        // 将第二个按钮放在第二行
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        secondRow.add(buttonList.get(1));
        rowList.add(secondRow);

        // 将剩余的按钮按每两个一组分组
        for (int i = 2; i < buttonList.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(buttonList.get(i));
            if (i + 1 < buttonList.size()) {
                row.add(buttonList.get(i + 1));
            }
            rowList.add(row);
        }

        // 构建并返回InlineKeyboardMarkup对象
        return InlineKeyboardMarkup.builder().keyboard(rowList).build();
    }

    private void handleUserMessageInput(Message message, UserCreateLotterySession userSession) {
        String text = message.getText();
        String regex = "^https://t\\.me/";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String link = text;

            Integer type = userSession.getType();
            String name = "", name02 = "";
            if (type == 1) {
                String n = link.substring(link.lastIndexOf("/") + 1);
                name = "加入群聊<a href=\"" + link + "\">@" + n + "</a>";
                name02 = "加入群聊";
            } else if (type == 2) {
                String n = link.substring(link.lastIndexOf("/") + 1);
                name = "订阅频道<a href=\"" + link + "\">@" + n + "</a>";
                name02 = "订阅频道";
            }

            Lottery lottery = lotteryMapper.selectOne(new LambdaQueryWrapper<Lottery>().eq(Lottery::getLotteryId, userSession.getUid()));
            prizePoolService.add(lottery.getLotteryId(), lottery.getMoney(), lottery.getNumber());
            Message execute = null;
            try {
                BigDecimal money = lottery.getMoney();
                if (userSession.getIsAdmin()) {
                    money = lottery.getVirtuaMoney();
                }
                execute = bot.execute(SendMessage.builder().chatId(message.getChatId()).text(BotSendMessageUtils.createLotteryMessage(money, lottery.getNumber(), lottery.getLotteryId()) + "\n\n抢红包条件:<b>" + name + "</b>").parseMode("html").disableWebPagePreview(true).replyMarkup(createInlineKeyboardButton(lottery.getLotteryId(), name02, link)).build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            Integer messageId = execute.getMessageId();
            lottery.setMessageId(Long.valueOf(messageId));
            lottery.setLink(link);
            lottery.setLinkType(type);
            lotteryMapper.updateById(lottery);
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, lottery.getTgId()));
            user.setMoney(user.getMoney().subtract(lottery.getMoney()));
            userMapper.updateById(user);
        } else {
            SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text("输入链接错误\n请重新创建抽奖\n必须是https://t.me/开头").parseMode("html").build();

            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        userCreateLotterySessionList.removeUserSession(message.getChatId());
    }
}
