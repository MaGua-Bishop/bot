package com.li.bot.sessions;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.entity.database.User;
import com.li.bot.enums.OrderStatus;
import com.li.bot.handle.key.BotKeyFactory;
import com.li.bot.handle.key.IKeyboard;
import com.li.bot.handle.menu.BotMenuFactory;
import com.li.bot.handle.menu.IBotMenu;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.ReplyMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.enums.AdminEditSessionState;
import com.li.bot.sessions.enums.CancelOrderSessionState;
import com.li.bot.utils.BotMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Slf4j
public class CancelOrder {

    private BotServiceImpl bot;

    private Message message;

    private CancelOrderSessionList cancelOrderSessionList;

    private OrderMapper orderMapper;

    private BusinessMapper businessMapper;

    private UserMapper userMapper;

    private ReplyMapper replyMapper;

    public CancelOrder(BotServiceImpl bot, Message message, CancelOrderSessionList cancelOrderSessionList, OrderMapper orderMapper, BusinessMapper businessMapper, UserMapper userMapper, ReplyMapper replyMapper) {
        this.bot = bot;
        this.message = message;
        this.cancelOrderSessionList = cancelOrderSessionList;
        this.orderMapper = orderMapper;
        this.businessMapper = businessMapper;
        this.userMapper = userMapper;
        this.replyMapper = replyMapper;
    }

    private InlineKeyboardMarkup createButton(String name) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).callbackData("无").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    public void execute(BotMenuFactory botMenuFactory, BotKeyFactory botKeyFactory) {
        if (message.getChat().getType().equals("private")) {
            return;
        }
        IBotMenu menu = botMenuFactory.getMenu(message.getText());
        if (menu != null) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("操作已取消").build());
                cancelOrderSessionList.removeUserSession(message.getChatId());
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
                    cancelOrderSessionList.removeUserSession(message.getChatId());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                key.execute(bot, message);
                return;
            }
        }
        CancelOrderSession userSession = cancelOrderSessionList.getCancelOrderSession(message.getFrom().getId());
        CancelOrderSessionState state = userSession.getState();
        switch (state) {
            case WAITING_FOR_USER_MESSAGE:
                handleUserMessageInput(message, userSession);
                break;
            default:
                break;
        }
    }

    private BigDecimal isMoney(String money) {
        // 使用正则表达式验证是否是数字且最多保留两位小数
        Pattern pattern = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
        Matcher matcher = pattern.matcher(money);

        if (matcher.matches()) {
            // 使用 DecimalFormat 保留两位小数
            DecimalFormat df = new DecimalFormat("0.00");
            String format = df.format(Double.parseDouble(money));
            return new BigDecimal(format);
        } else {
            // 如果不是有效的数字或格式不正确，返回 null
            return null;
        }
    }

    private void handleUserMessageInput(Message message, CancelOrderSession userSession) {
        String text = message.getText();
        Order order = userSession.getOrder();
        order.setStatus(OrderStatus.Cancel.getCode());
        order.setReviewTgId(message.getFrom().getId());
        int index = orderMapper.updateOrderById(order.getStatus(), order.getReviewTgId(), UUID.fromString(order.getOrderId()), LocalDateTime.now());
        if (index == 1) {
            replyMapper.updateStatusByOrderId02(order.getOrderId());
            //获取业务的金额
            Long businessId = order.getBusinessId();
            Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, businessId));
            BigDecimal money = business.getMoney();
            //退还给用户
            User selectOne = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, order.getTgId()));
            selectOne.setMoney(selectOne.getMoney().add(money));
            userMapper.updateById(selectOne);
//            EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(message.getChatId()).messageId(userSession.getMessageId()).replyMarkup(createButton("已取消")).build();
//            try {
//                bot.execute(editMessageReplyMarkup);
//            } catch (TelegramApiException e) {
//                throw new RuntimeException(e);
////                System.out.println("忽略重复点击错误");
//            }
            String userName = message.getFrom().getLastName() + message.getFrom().getFirstName();
            SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text("<a href=\"tg://user?id=" + message.getFrom().getId() + "\">" + userName + "</a>" +
                    "\n订单id:<code>" + order.getOrderId() + "</code>\n<b>已取消</b>").parseMode("html").build();
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                System.out.println("忽略重复点击错误");
            }
            try {
                bot.execute(SendMessage.builder().chatId(order.getTgId()).text("您报单的<b>" + business.getName() + "</b>订单已取消\n" +
                        "订单id：\n" +
                        "<code>" + order.getOrderId() + "</code>\n" + "<b>取消原因:</b>\n" + text).parseMode("html").build());
            } catch (TelegramApiException e) {
                System.out.println("忽略重复点击错误");
            }
        }


        cancelOrderSessionList.removeUserSession(message.getChatId());
    }
}
