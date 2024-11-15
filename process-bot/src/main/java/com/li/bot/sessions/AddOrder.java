package com.li.bot.sessions;

import com.google.common.collect.Lists;
import com.li.bot.handle.key.BotKeyFactory;
import com.li.bot.handle.key.IKeyboard;
import com.li.bot.handle.menu.BotMenuFactory;
import com.li.bot.handle.menu.IBotMenu;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.enums.OrderSessionState;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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
@Slf4j
public class AddOrder {

    private BotServiceImpl bot;

    private OrderSession orderSession;

    private Message message;

    private AddOrderSessionList addOrderSessionList;
    private OrderMapper orderMapper;

    private UserMapper userMapper;

    private CallbackQuery callbackQuery;

    public AddOrder(BotServiceImpl bot, OrderSession orderSession, Message message, AddOrderSessionList addOrderSessionList, OrderMapper orderMapper, UserMapper userMapper) {
        this.bot = bot;
        this.orderSession = orderSession;
        this.message = message;
        this.addOrderSessionList = addOrderSessionList;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
    }

    public void setCallbackFactory(CallbackQuery callbackQuery) {
        this.callbackQuery = callbackQuery;
    }

    public void execute(BotMenuFactory botMenuFactory, BotKeyFactory botKeyFactory) {

        IBotMenu menu = botMenuFactory.getMenu(message.getText());
        if (menu != null) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("操作已取消").build());
                addOrderSessionList.removeUserSession(message.getFrom().getId());
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
                    addOrderSessionList.removeUserSession(message.getFrom().getId());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                key.execute(bot, message);
                return;
            }
        }

        OrderSessionState state = orderSession.getState();
        switch (state) {
            case WAITING_FOR_USER_MESSAGE:
                handleUserMessageInput(message);
                break;
            default:
                break;
        }
    }

    private void handleUserMessageInput(Message message) {
        OrderSession userSession = addOrderSessionList.getUserSession(message.getFrom().getId());
        userSession.setState(OrderSessionState.WAITING_FOR_PURCHASE);
        if (message.hasPhoto()) {
            PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
            String fileId = photo.getFileId();
            userSession.getBusiness().setFileId(fileId);
            String caption = message.getCaption() != null ? message.getCaption() : "";
            userSession.getBusiness().setMessageText(caption);
        }else{
            userSession.getBusiness().setMessageText(message.getText());
        }
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("提交").callbackData("send:order:yes").build());
        buttonList.add(InlineKeyboardButton.builder().text("不提交").callbackData("send:order:no").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();

        BigDecimal money = userSession.getBusiness().getMoney();

        try {
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("是否提交该业务报单\n业务价格:" + money).replyMarkup(inlineKeyboardMarkup).build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
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


}
