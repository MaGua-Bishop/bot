package com.li.bot.handle.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.enums.OrderStatus;
import com.li.bot.enums.ReplyStatus;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.ReplyMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class ReplyOrderServiceMenuImpl implements IBotMenu {

    @Override
    public String getMenuName() {
        return "回复订单";
    }


    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ReplyMapper replyMapper;


    private String getUUID(String text) {
        Pattern uuidPattern = Pattern.compile("#回单 ([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");
        // 创建Matcher对象
        Matcher matcher = uuidPattern.matcher(text);

        // 查找匹配项
        if (matcher.find()) {
            // 获取找到的UUID字符串
            String foundUuidString = matcher.group(1);
            return foundUuidString;
        } else {
            return null;
        }
    }

    @Override
    public void execute(BotServiceImpl bot, Message message) {
        String text = "";
        if (message.getText() != null) {
            text = message.getText();
        } else if (message.getCaption() != null) {
            text = message.getCaption();
        }


        String uuid = getUUID(text);
        if (uuid == null) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId().toString()).text("uuid错误，请使用#回单 [uuid] 回复订单").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        UUID uId = UUID.fromString(uuid);
        Reply reply = replyMapper.selectOne(new LambdaQueryWrapper<Reply>().eq(Reply::getOrderId, uuid).eq(Reply::getTgId, message.getFrom().getId()));
        if (reply == null) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId().toString()).text("未找到该订单或该订单不是您的").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }


        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, uId));
        if (order == null) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId().toString()).text("请检查订单id是否正确").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }


        if (order.getStatus().equals(OrderStatus.IN_PROGRESS.getCode())) {
            orderMapper.updateOrderStatusById(OrderStatus.COMPLETED.getCode(), uId, LocalDateTime.now());
        }


        reply.setOrderId(uuid);
        Long[] chatId = {message.getChatId()};
        Long[] messageId = {Long.valueOf(message.getMessageId())};
        String[] messageType = {message.getChat().getType()};
        reply.setMessageChatId(chatId);
        reply.setMessageId(messageId);
        reply.setMessageType(messageType);
        reply.setStatus(ReplyStatus.PENDING.getCode());
        int index = replyMapper.updateReply(reply);
        if (index == 1) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("回复订单成功\n" +
                        "订单id:\n" +
                        "<code>" + order.getOrderId() + "</code>").parseMode("html").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            CopyMessage copyMessage = new CopyMessage();
            copyMessage.setChatId(order.getTgId());
            copyMessage.setMessageId(message.getMessageId());
            copyMessage.setFromChatId(message.getChatId());

            try {
                bot.execute(copyMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

        }


    }
}
