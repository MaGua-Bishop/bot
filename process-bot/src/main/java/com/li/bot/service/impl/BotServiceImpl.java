package com.li.bot.service.impl;


import com.li.bot.config.BotConfig;
import com.li.bot.handle.CallbackQueryHandle;
import com.li.bot.handle.MessageHandle;
import com.li.bot.handle.callback.AdminEditBusinessCallbackImpl;
import com.li.bot.handle.callback.CallbackFactory;
import com.li.bot.handle.key.BotKeyFactory;
import com.li.bot.handle.menu.BotMenuFactory;
import com.li.bot.handle.menu.IBotMenu;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.UserMapper;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BotServiceImpl extends TelegramWebhookBot {


    private final BotConfig botConfig;


    public BotServiceImpl(BotConfig botConfig) {
        super(botConfig.getDefaultBotOptions());
        this.botConfig = botConfig;
    }

    public void registerCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Start the bot"));
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
    private BotMenuFactory botMenuFactory;
    @Autowired
    private BotKeyFactory botKeyFactory;

    @Autowired
    private CallbackFactory callbackFactory;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AddBusinessSessionList addBusinessSessionList;

    @Autowired
    private AddOrderSessionList addOrderSessionList;

    @Autowired
    private AdminEditSessionList adminEditSessionList;

    private Long getUserId(String text){
        // 正则表达式模式，用于匹配固定十位数的ID
        Pattern idPattern = Pattern.compile("\\b\\d{10}\\b");
        Matcher idMatcher = idPattern.matcher(text);

        // 查找并打印所有匹配的ID
        if (idMatcher.find()) {
            String id = idMatcher.group(0);  // 提取ID
            return Long.parseLong(id);
        }
        return null;
    }




    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage()) {


            String text = "";
            if(update.getMessage().getText() !=null){
                text = update.getMessage().getText();
            }else if(update.getMessage().getCaption() !=null){
                text = update.getMessage().getCaption();
            }
            if(!"".equals(text)){
                if(text.indexOf("#回单 ") == 0){
                    IBotMenu menu = botMenuFactory.getMenu("回复订单");
                    menu.execute(this,update.getMessage());
                    return null;
                }
                Long userId = getUserId(text);
                if(userId != null){
                    IBotMenu menu = botMenuFactory.getMenu("查询用户余额");
                    menu.execute(this,update.getMessage());
                    return null;
                }
                if(text.indexOf("#修改用户金额") == 0){
                    IBotMenu menu = botMenuFactory.getMenu("修改用户金额");
                    menu.execute(this,update.getMessage());
                    return null;
                }
            }


            if(update.getMessage().getChat().getType().equals("private")){
                Long tgId = update.getMessage().getFrom().getId();
                OrderSession orderSession = addOrderSessionList.getUserSession(tgId);

                if (orderSession != null) {
                    AddOrder addOrder = new AddOrder(this, orderSession, update.getMessage(), addOrderSessionList, orderMapper, userMapper);
                    addOrder.execute(botMenuFactory,botKeyFactory);
                    return null;
                }
                if(adminEditSessionList.getUserSession(tgId) != null){
                    new AdminEdit(this,update.getMessage(), adminEditSessionList, businessMapper).execute(botMenuFactory,botKeyFactory);
                    return null;
                }

                BusinessSession businessSession = addBusinessSessionList.getUserSession(tgId);
                if (businessSession != null) {
                    new AddBusiness(this, businessSession, update.getMessage(), addBusinessSessionList, businessMapper).execute(botMenuFactory,botKeyFactory);
                    return null;
                }
            }

            if (update.getMessage().hasText()) {
                try {
                    new MessageHandle(botKeyFactory, botMenuFactory, this, update.getMessage()).handle();
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
            return null;
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
