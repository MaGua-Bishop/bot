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
import java.util.ArrayList;
import java.util.List;

@Service
public class BotServiceImpl extends TelegramWebhookBot {


    private final BotConfig botConfig;


    public BotServiceImpl(BotConfig botConfig) {
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
    private CallbackFactory callbackFactory ;

    @Autowired
    private MessageFactory messageFactory ;

    @Autowired
    private BotMenuFactory botMenuFactory ;

    @Autowired
    private UserTakeoutSessionList userTakeoutSessionList;
    @Autowired
    private UserCreateLotterySessionList userCreateLotterySessionList ;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TakeoutMapper takeoutMapper;
    @Autowired
    private FileService fileService ;
    @Autowired
    private LotteryMapper lotteryMapper ;
    @Autowired
    private PrizePoolService prizePoolService ;



    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().getChat().getType().equals("private")) {
                Long tgId = update.getMessage().getFrom().getId();
                UserTakeoutSession userTakeoutSession = userTakeoutSessionList.getUserTakeoutSession(tgId);
                if (userTakeoutSession != null) {
                    new UserTakeout(this, update.getMessage(), userTakeoutSessionList, userMapper, takeoutMapper,fileService).execute(botMenuFactory);
                    return null;
                }
                UserCreateLotterySession userCreateLotterySession = userCreateLotterySessionList.getUserTakeoutSession(tgId);
                if (userCreateLotterySession != null) {
                    new UserCreateLottery(this, update.getMessage(), userCreateLotterySessionList, userMapper, takeoutMapper,lotteryMapper,prizePoolService,botConfig).execute(botMenuFactory);
                    return null;
                }
            }
            if (update.getMessage().hasText()) {
                try {
                    new MessageHandle(this, update.getMessage(), messageFactory,botMenuFactory).handle();
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
