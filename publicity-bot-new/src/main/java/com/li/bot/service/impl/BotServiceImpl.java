package com.li.bot.service.impl;


import com.li.bot.config.BotConfig;
import com.li.bot.handle.CallbackQueryHandle;
import com.li.bot.handle.ChannelPostHandle;
import com.li.bot.handle.MessageHandle;
import com.li.bot.handle.callback.CallbackFactory;
import com.li.bot.handle.ChatMemberUpdatedHandle;
import com.li.bot.handle.callback.SelectConvoysListCallback;
import com.li.bot.handle.message.MessageFactory;
import com.li.bot.mapper.ButtonMapper;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.sessions.UpdateConvoysSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
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
    private InviteMapper inviteMapper ;
    @Autowired
    private ConvoysMapper convoysMapper;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper;

    @Autowired
    private UpdateConvoysSessionList updateConvoysSessionList ;

    @Autowired
    private FileService fileService ;

    @Autowired
    private ButtonMapper buttonMapper ;




    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                try {
                    new MessageHandle(this, update.getMessage(),messageFactory,updateConvoysSessionList).handle();
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
        if(update.hasMyChatMember()){
            ChatMemberUpdated myChatMember = update.getMyChatMember();
            try {
                new ChatMemberUpdatedHandle(this,myChatMember, inviteMapper, convoysMapper, convoysInviteMapper).handle();
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        if(update.hasChannelPost()){
            Message channelPost = update.getChannelPost();
            new ChannelPostHandle(this, channelPost, convoysInviteMapper, inviteMapper, fileService, buttonMapper).handle();
        }

        return null;
    }
}
