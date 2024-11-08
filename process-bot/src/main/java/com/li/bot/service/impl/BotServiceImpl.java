package com.li.bot.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.User;
import com.li.bot.handle.CallbackQueryHandle;
import com.li.bot.handle.MessageHandle;
import com.li.bot.handle.callback.AdminEditBusinessCallbackImpl;
import com.li.bot.handle.callback.CallbackFactory;
import com.li.bot.handle.key.BotKeyFactory;
import com.li.bot.handle.menu.BotMenuFactory;
import com.li.bot.handle.menu.IBotMenu;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.ReplyMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.sessions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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

    @Autowired
    private FileService fileService;

    @Autowired
    private ChannelMembersServiceImpl channelMembersService;

    @Autowired
    private CancelOrderSessionList cancelOrderSessionList ;

    @Autowired
    private ReplyMapper replyMapper ;


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

    private InlineKeyboardMarkup createInlineKeyboardButton() {
        String channelLink = fileService.getChannelLink();
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("加入频道").url(channelLink).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    private Long getChannelId() {
        String channelLink = fileService.getChannelLink();
        channelLink = channelLink.substring(channelLink.lastIndexOf("/") + 1);
        GetChat getChat = new GetChat();
        getChat.setChatId("@" + channelLink);
        Chat execute = null;
        try {
            execute = execute(getChat);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return execute.getId();
    }


    private Boolean addChannelMember(Message message) {
        if (!message.getChat().getType().equals("private")) {
            return true;
        }
        Long tgId = message.getFrom().getId();
        if (channelMembersService.isChannelMember(tgId)) {
            return true;
        }
        Long chatId = getChannelId();
        try {
            ChatMember member = execute(GetChatMember.builder().chatId(chatId).userId(Long.valueOf(tgId)).build());
            if (!member.getStatus().equals("left")) {
                channelMembersService.addChannelMember(tgId);
                return true;
            } else {
                if (channelMembersService.isChannelMember(tgId)) {
                    channelMembersService.removeChannelMember(tgId);
                }
                execute(SendMessage.builder().chatId(message.getChatId().toString()).text("您不是频道成员，无法使用本机器人").replyMarkup(createInlineKeyboardButton()).build());
                return false;
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean addChannelMember(Update update) {
        Message message = (Message) update.getCallbackQuery().getMessage();
        if (!message.getChat().getType().equals("private")) {
            return true;
        }
        Long tgId = update.getCallbackQuery().getFrom().getId();
        if (channelMembersService.isChannelMember(tgId)) {
            return true;
        }
        Long chatId = getChannelId();
        try {
            ChatMember member = execute(GetChatMember.builder().chatId(chatId).userId(Long.valueOf(tgId)).build());
            if (!member.getStatus().equals("left")) {
                channelMembersService.addChannelMember(tgId);
                return true;
            } else {
                if (channelMembersService.isChannelMember(tgId)) {
                    channelMembersService.removeChannelMember(tgId);
                }
                execute(SendMessage.builder().chatId(tgId).text("您不是频道成员，无法使用本机器人").replyMarkup(createInlineKeyboardButton()).build());
                return false;
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Boolean b = addChannelMember(update.getMessage());
            if (!b) {
                return null;
            }
            if (update.getMessage().getChat().getType().equals("private")) {

                Long tgId = update.getMessage().getFrom().getId();
                OrderSession orderSession = addOrderSessionList.getUserSession(tgId);

                if (orderSession != null) {
                    AddOrder addOrder = new AddOrder(this, orderSession, update.getMessage(), addOrderSessionList, orderMapper, userMapper);
                    addOrder.execute(botMenuFactory, botKeyFactory);
                    return null;
                }
                if (adminEditSessionList.getUserSession(tgId) != null) {
                    new AdminEdit(this, update.getMessage(), adminEditSessionList, businessMapper).execute(botMenuFactory, botKeyFactory);
                    return null;
                }

                BusinessSession businessSession = addBusinessSessionList.getUserSession(tgId);
                if (businessSession != null) {
                    new AddBusiness(this, businessSession, update.getMessage(), addBusinessSessionList, businessMapper).execute(botMenuFactory, botKeyFactory);
                    return null;
                }
            }else{
                Long tgId = update.getMessage().getFrom().getId();
                if(cancelOrderSessionList.getCancelOrderSession(tgId) !=null){
                    new CancelOrder(this, update.getMessage(), cancelOrderSessionList, orderMapper, businessMapper, userMapper,replyMapper).execute(botMenuFactory, botKeyFactory);
                    return null;
                }
            }
            String text = "";
            if (update.getMessage().getText() != null) {
                text = update.getMessage().getText();
            } else if (update.getMessage().getCaption() != null) {
                text = update.getMessage().getCaption();
            }
            if (!"".equals(text)) {
                if (text.indexOf("#回单 ") == 0) {
                    IBotMenu menu = botMenuFactory.getMenu("回复订单");
                    menu.execute(this, update.getMessage());
                    return null;
                }
                if (text.indexOf("#减少余额 ") == 0) {
                    IBotMenu menu = botMenuFactory.getMenu("减少余额");
                    menu.execute(this, update.getMessage());
                    return null;
                }
                Long userId = getUserId(text);
                if (userId != null) {
                    IBotMenu menu = botMenuFactory.getMenu("查询用户余额");
                    menu.execute(this, update.getMessage());
                    return null;
                }
                if (isAdminUpdateUserMoney(text)) {
                    IBotMenu menu = botMenuFactory.getMenu("修改用户金额");
                    menu.execute(this, update.getMessage());
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
            Boolean b = addChannelMember(update);
            if (!b) {
                return null;
            }
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
