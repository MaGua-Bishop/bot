package com.li.bot.handle.callback;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Order;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.ReplyMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
public class ReceiveOrderListCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "receiveOrderList";
    }

    private InlineKeyboardMarkup createButton(String name){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).callbackData("无").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Autowired
    private OrderMapper orderMapper;


    @Autowired
    private ReplyMapper replyMapper ;

    @Override
    @Transactional
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery)  {
        String data = callbackQuery.getData();
        String businessId  = data.substring(data.lastIndexOf(":") + 1);

        List<Order> list = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getBusinessId, Long.valueOf(businessId)).eq(Order::getStatus, OrderStatus.PENDING.getCode()));

        for (Order order : list) {
            if(order != null){
                UUID uuid = UUID.fromString(order.getOrderId());
                orderMapper.updateOrderStatusById(OrderStatus.IN_PROGRESS.getCode(), uuid, LocalDateTime.now());
                replyMapper.insertReply(uuid,callbackQuery.getFrom().getId());
            }
        }
        String name = callbackQuery.getFrom().getLastName()+callbackQuery.getFrom().getFirstName()+"接单成功";
        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton(name)).build();
        try {
            bot.execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            System.out.println("忽略重复点击错误");
        }




    }
}
