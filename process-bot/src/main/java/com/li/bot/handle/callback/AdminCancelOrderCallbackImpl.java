package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.entity.database.User;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.ReplyMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.CancelOrderSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class AdminCancelOrderCallbackImpl implements ICallback {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private BusinessMapper businessMapper;
    @Autowired
    private CancelOrderSessionList cancelOrderSessionList;

    @Autowired
    private ReplyMapper replyMapper;

    @Override
    public String getCallbackName() {
        return "adminCancelOrder";
    }


    private InlineKeyboardMarkup createButton(String name) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).callbackData("无").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String orderId = data.substring(data.lastIndexOf(":") + 1);
        Reply reply = replyMapper.selectOne(new LambdaQueryWrapper<Reply>().eq(Reply::getOrderId, orderId));
        if (reply == null) {
            try {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("订单不存在").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        if (!reply.getTgId().equals(callbackQuery.getFrom().getId())) {
            try {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("订单不是您的,您不可操作").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        if (reply.getStatus() == -2) {
            return;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, callbackQuery.getFrom().getId()));
        Order order = orderMapper.getOrderByIdAndStatus(UUID.fromString(orderId), OrderStatus.IN_PROGRESS.getCode());
        if (order == null) {
            try {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text("该订单异常").parseMode("MarkdownV2").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        String a = "<a href=\"tg://user?id=" + callbackQuery.getFrom().getId() + "\">@" + user.getTgName() + "</a>";
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(a +
                "请输入取消订单的原因").parseMode("html").build();
        bot.execute(sendMessage);

        cancelOrderSessionList.addUserSession(callbackQuery.getFrom().getId(), reply, order, callbackQuery.getMessage().getMessageId());
    }
}
