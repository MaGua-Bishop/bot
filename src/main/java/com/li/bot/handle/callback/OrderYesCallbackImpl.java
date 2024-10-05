package com.li.bot.handle.callback;

import com.google.common.collect.Lists;
import com.li.bot.entity.database.Order;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class OrderYesCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "order:yes";
    }

    @Autowired
    private OrderMapper orderMapper ;

        private InlineKeyboardMarkup createInlineKeyboardButton(String orderId){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("领取").callbackData("receive:order:"+orderId).build());
        buttonList.add(InlineKeyboardButton.builder().text("放弃").callbackData("waiver:order:"+orderId).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup createButton(String name){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).callbackData("无").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        String orderId = data.substring(data.lastIndexOf(":") + 1);
        UUID id = UUID.fromString(orderId);
        Order order = orderMapper.getOrderByIdAndStatus(id, OrderStatus.REVIEW.getCode());



        if(order != null){
            order.setStatus(OrderStatus.PENDING.getCode());
            order.setReviewTgId(callbackQuery.getFrom().getId());
            int index = orderMapper.updateOrderById(order.getStatus(), order.getReviewTgId(),id, LocalDateTime.now());
            if(index == 1){
                EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton("已通过")).build();
                try {
                    bot.execute(editMessageReplyMarkup);
                } catch (TelegramApiException e) {
                    System.out.println("忽略重复点击错误");
                }
                Long groupChatId = -4576426080L;
                CopyMessage copyMessage = new CopyMessage();
                copyMessage.setChatId(groupChatId);
                copyMessage.setMessageId(order.getMessageId());
                copyMessage.setFromChatId(order.getTgId());
                copyMessage.setReplyMarkup(createInlineKeyboardButton(order.getOrderId()));
                try {
                    bot.execute(copyMessage);
                } catch (TelegramApiException e) {
                    System.out.println("忽略重复点击错误");
                }

                try {
                    bot.execute(SendMessage.builder().chatId(order.getTgId()).text("订单审核通过\n" +
                            "订单id：\n" +
                            "<code>"+order.getOrderId()+"</code>").parseMode("html").build());
                } catch (TelegramApiException e) {
                    System.out.println("忽略重复点击错误");
                }

            }else {
                try {
                    bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("订单审核处理失败").parseMode("MarkdownV2").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }else {
            EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton("已通过")).build();
            try {
                bot.execute(editMessageReplyMarkup);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
