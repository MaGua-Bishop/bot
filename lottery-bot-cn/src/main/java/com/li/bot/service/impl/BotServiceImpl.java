package com.li.bot.service.impl;


import com.li.bot.config.BotConfig;
import com.li.bot.handle.CallbackQueryHandle;
import com.li.bot.handle.MessageHandle;
import com.li.bot.handle.callback.CallbackFactory;
import com.li.bot.handle.message.MessageFactory;
import com.li.bot.mapper.LotteryMapper;
import com.li.bot.mapper.TakeoutMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.meun.BotMenuFactory;
import com.li.bot.meun.IBotMenu;
import com.li.bot.sessions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BotServiceImpl extends TelegramWebhookBot {


    private final BotConfig botConfig;


    public BotServiceImpl(BotConfig botConfig) {
//        super(botConfig.getDefaultBotOptions());
        this.botConfig = botConfig;
    }

    public void registerCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "启动机器人"));
        commands.add(new BotCommand("/view", "创建抽奖者查看中奖用户"));
        commands.add(new BotCommand("/exchange", "创建抽奖者核销中奖者奖励"));
        commands.add(new BotCommand("/help", "帮助|使用说明"));
        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(commands);
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void init() {
        setupWebhook();
        registerCommands();

    }

    public void setupWebhook() {
        SetWebhook setWebhook = new SetWebhook();
        setWebhook.setUrl(botConfig.getUrl());  // 例如 "https://yourdomain.com/bot/webhook"

        try {
            execute(setWebhook);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotname();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public String getBotPath() {
        return botConfig.getBotname();
    }

    @Autowired
    private CallbackFactory callbackFactory;

    @Autowired
    private MessageFactory messageFactory;

    @Autowired
    private BotMenuFactory botMenuFactory;

    @Autowired
    private UserTakeoutSessionList userTakeoutSessionList;
    @Autowired
    private UserCreateLotterySessionList userCreateLotterySessionList;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TakeoutMapper takeoutMapper;
    @Autowired
    private FileService fileService;
    @Autowired
    private LotteryMapper lotteryMapper;
    @Autowired
    private PrizePoolService prizePoolService;

    private Long getUserId(String text) {
        // 正则表达式模式，用于匹配固定十位数的ID
        Pattern idPattern = Pattern.compile("^\\d{5,15}$");
        Matcher idMatcher = idPattern.matcher(text);

        // 检查整个文本是否完全匹配十位数字
        if (idMatcher.matches()) {
            // 如果匹配成功，将文本转换为 Long 并返回
            return Long.parseLong(text);
        }
        // 如果不匹配，返回 null
        return null;
    }

    private Boolean isAdminUpdateUserMoney(String text) {
        String[] split = text.split(" ");
        if (split.length != 2) {
            return false;
        }
        String id = split[0];
        Long userId = getUserId(id);
        if (userId == null) {
            return false;
        }
        String money = split[1];
        try {
            BigDecimal amount = new BigDecimal(money);
            // 如果能成功转换为 BigDecimal，则返回 true
            return true;
        } catch (NumberFormatException e) {
            // 如果转换失败，说明 money 不是有效的数字
            return false;
        }
    }


    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().getChat().getType().equals("private")) {
                Long tgId = update.getMessage().getFrom().getId();
                UserTakeoutSession userTakeoutSession = userTakeoutSessionList.getUserTakeoutSession(tgId);
                if (userTakeoutSession != null) {
                    new UserTakeout(this, update.getMessage(), userTakeoutSessionList, userMapper, takeoutMapper, fileService).execute(botMenuFactory);
                    return null;
                }
                UserCreateLotterySession userCreateLotterySession = userCreateLotterySessionList.getUserTakeoutSession(tgId);
                if (userCreateLotterySession != null) {
                    new UserCreateLottery(this, update.getMessage(), userCreateLotterySessionList, userMapper, takeoutMapper, lotteryMapper, prizePoolService, botConfig).execute(botMenuFactory);
                    return null;
                }
            } else if (update.getMessage().getChat().getType().equals("group") || update.getMessage().getChat().getType().equals("supergroup")) {
                Long userId = getUserId(update.getMessage().getText());
                if (userId != null) {
                    IBotMenu menu = botMenuFactory.getMenu("查询用户余额");
                    menu.execute(this, update.getMessage());
                    return null;
                }
                if (isAdminUpdateUserMoney(update.getMessage().getText())) {
                    IBotMenu menu = botMenuFactory.getMenu("修改用户金额");
                    menu.execute(this, update.getMessage());
                    return null;
                }
            }
            if (update.getMessage().hasText()) {
                try {
                    new MessageHandle(this, update.getMessage(), messageFactory, botMenuFactory).handle();
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        }
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            try {
                new CallbackQueryHandle(this, callbackQuery, callbackFactory).executeCallbackQuery();
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
        return null;
    }
}
