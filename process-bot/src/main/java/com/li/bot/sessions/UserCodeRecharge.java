package com.li.bot.sessions;

import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import com.li.bot.entity.Workgroup;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.UserMoney;
import com.li.bot.handle.key.BotKeyFactory;
import com.li.bot.handle.key.IKeyboard;
import com.li.bot.handle.menu.BotMenuFactory;
import com.li.bot.handle.menu.IBotMenu;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.UserMoneyMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.sessions.enums.BusinessSessionState;
import com.li.bot.sessions.enums.UserCodeRechargeState;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class UserCodeRecharge {

    private BotServiceImpl bot;

    private UserCodeRechargeSession userCodeRechargeSession;

    private Message message;
    private UserCodeRechargeSessionList userCodeRechargeSessionList;

    private FileService fileService;

    private UserMoneyMapper userMoneyMapper;

    public UserCodeRecharge(BotServiceImpl bot, Message message, UserCodeRechargeSession userCodeRechargeSession, UserCodeRechargeSessionList userCodeRechargeSessionList, FileService fileService, UserMoneyMapper userMoneyMapper) {
        this.bot = bot;
        this.message = message;
        this.userCodeRechargeSession = userCodeRechargeSession;
        this.userCodeRechargeSessionList = userCodeRechargeSessionList;
        this.fileService = fileService;
        this.userMoneyMapper = userMoneyMapper;
    }

    public void execute(BotMenuFactory botMenuFactory, BotKeyFactory botKeyFactory) {

        IBotMenu menu = botMenuFactory.getMenu(message.getText());
        if (menu != null) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("操作已取消").build());
                userCodeRechargeSessionList.removeUserSession(message.getFrom().getId());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            menu.execute(bot, message);
            return;
        } else {
            IKeyboard key = botKeyFactory.getKey(message.getText());
            if (key != null) {
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("操作已取消").build());
                    userCodeRechargeSessionList.removeUserSession(message.getFrom().getId());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                key.execute(bot, message);
                return;
            }
        }

        UserCodeRechargeState state = userCodeRechargeSession.getState();
        switch (state) {
            case WAITING_FOR_USER_MONEY:
                handleUserMoneyInput(message);
                break;
            case WAITING_FOR_USER_CODE:
                handleUserCodeInput(message);
                break;
            default:
                // 处理其他状态
                break;
        }
    }

    public static BigDecimal parseBusinessPrice(String input) {
        // 使用正则表达式验证是否是数字且最多保留两位小数
        Pattern pattern = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            // 使用 DecimalFormat 保留两位小数
            DecimalFormat df = new DecimalFormat("0.00");
            String format = df.format(Double.parseDouble(input));
            return new BigDecimal(format);
        } else {
            // 如果不是有效的数字或格式不正确，返回 null
            return null;
        }
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(Long tgId) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("取消充值").callbackData("user:cancel:" + tgId).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup createInlineKeyboardButton02(Long moneyId) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("确认转账").callbackData("admin:confirm:money:" + moneyId).build());
        buttonList.add(InlineKeyboardButton.builder().text("取消转账").callbackData("admin:cancel:money:" + moneyId).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private void handleUserMoneyInput(Message message) {
        String userMoney = message.getText();
        BigDecimal bigDecimal = parseBusinessPrice(userMoney);
        if (bigDecimal == null) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("金额格式错误").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        userCodeRechargeSession.setMoney(bigDecimal);
        userCodeRechargeSession.setState(UserCodeRechargeState.WAITING_FOR_USER_CODE);

        SendPhoto sendPhotoRequest = new SendPhoto();
        sendPhotoRequest.setChatId(message.getChatId());
        InputFile inputFile = new InputFile(fileService.getCodeImage());
        sendPhotoRequest.setPhoto(inputFile);
        sendPhotoRequest.setCaption("请发送转账成功的图片或点击下方按钮取消充值");
        sendPhotoRequest.setReplyMarkup(createInlineKeyboardButton(message.getChatId()));
        try {
            bot.execute(sendPhotoRequest);  // 执行发送图片的操作
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleUserCodeInput(Message message) {
        PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
        String fileId = photo.getFileId();

        // 创建发送图片的请求
        SendPhoto sendPhotoRequest = new SendPhoto();
        String string = fileService.readFileContent03();
        Workgroup workgroup = JSONObject.parseObject(string, Workgroup.class);
        UserMoney userMoney = new UserMoney();
        userMoney.setTgId(message.getFrom().getId());
        userMoney.setMoney(userCodeRechargeSession.getMoney());
        userMoney.setType(5);
        userMoneyMapper.insertUserMoney(userMoney);
        List<String> groupList = workgroup.getGroupList();
        for (String s : groupList) {
            sendPhotoRequest.setChatId(s);
            InputFile inputFile = new InputFile(fileId);
            sendPhotoRequest.setPhoto(inputFile);
            String firstName = message.getFrom().getFirstName();
            String lastName = message.getFrom().getLastName();
            String a = "<a href=\"tg://user?id=" + message.getFrom().getId() + "\">" + firstName + (lastName != null ? lastName : "") + "</a>";
            sendPhotoRequest.setCaption("用户扫码充值:\n用户id:<code>" + message.getFrom().getId() + "</code>\n用户名:@" + a + "\n转账金额:<code>" + userCodeRechargeSession.getMoney() + "</code>");
            sendPhotoRequest.setParseMode("html");
            System.out.println("转账id:" + userMoney.getMoneyId());
            sendPhotoRequest.setReplyMarkup(createInlineKeyboardButton02(userMoney.getMoneyId()));
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("已发送转账图片，请等待充值").build());
                bot.execute(sendPhotoRequest);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        userCodeRechargeSessionList.removeUserSession(message.getFrom().getId());
    }


}
