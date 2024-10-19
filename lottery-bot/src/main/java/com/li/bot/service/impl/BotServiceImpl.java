package com.li.bot.service.impl;


import com.li.bot.config.BotConfig;
import com.li.bot.handle.CallbackQueryHandle;
import com.li.bot.handle.MessageHandle;
import com.li.bot.handle.callback.CallbackFactory;
import com.li.bot.handle.message.MessageFactory;
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
        super(botConfig.getDefaultBotOptions());
        this.botConfig = botConfig;
    }

    public void registerCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Start the bot"));
        commands.add(new BotCommand("/view", "bring the lottery id"));
        commands.add(new BotCommand("/exchange", "admin exchange"));
        commands.add(new BotCommand("/help", "help"));
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




    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                try {
                    new MessageHandle(this, update.getMessage(), messageFactory).handle();
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
