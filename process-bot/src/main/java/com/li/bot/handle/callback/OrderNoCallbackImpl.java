package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.User;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.UserMapper;
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
public class OrderNoCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "order:no";
    }

    @Autowired
    private OrderMapper orderMapper ;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BusinessMapper businessMapper ;

    private InlineKeyboardMarkup createButton(String name){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).callbackData("无").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    @Override
    @Transactional
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery){
        String data = callbackQuery.getData();
        String orderId = data.substring(data.lastIndexOf(":") + 1);

        UUID id = UUID.fromString(orderId);
        Order order = orderMapper.getOrderByIdAndStatus(id, OrderStatus.REVIEW.getCode());

        if(order != null){
            order.setStatus(OrderStatus.Review_FAILED.getCode());
            order.setReviewTgId(callbackQuery.getFrom().getId());
            int index = orderMapper.updateOrderById(order.getStatus(), order.getReviewTgId(),id, LocalDateTime.now());
            if(index == 1){
                //获取业务的金额
                Long businessId = order.getBusinessId();
                Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, businessId));
                BigDecimal money = business.getMoney();
                //退还给用户
                User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, order.getTgId()));
                user.setMoney(user.getMoney().add(money));
                userMapper.updateById(user);

                EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton("未通过")).build();
                try {
                    bot.execute(editMessageReplyMarkup);
                } catch (TelegramApiException e) {
                    System.out.println("忽略重复点击错误");
                }

                try {
                    bot.execute(SendMessage.builder().chatId(order.getTgId()).text("订单审核未通过\n" +
                            "订单id：\n" +
                            "<code>"+order.getOrderId()+"</code>").parseMode("html").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }


            }else {
                try {
                    bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("订单审核处理失败").parseMode("MarkdownV2").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }else {
            EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton("未通过")).build();
            try {
                bot.execute(editMessageReplyMarkup);
            } catch (TelegramApiException e) {
                System.out.println("忽略重复点击错误");
            }
        }

    }
}
