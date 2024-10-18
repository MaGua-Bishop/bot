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
public class AdminCancelOrderCallbackImpl implements ICallback{

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private OrderMapper orderMapper ;
    @Autowired
    private BusinessMapper businessMapper ;

    @Override
    public String getCallbackName() {
        return "adminCancelOrder";
    }



    private InlineKeyboardMarkup createButton(String name){
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
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, callbackQuery.getFrom().getId()));
        if(!user.getIsAdmin()){
            System.out.println("非管理员操作");
            return;
        }
        Order order = orderMapper.getOrderByIdAndStatus(UUID.fromString(orderId), OrderStatus.PENDING.getCode());
        if(order == null){
            try {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text("该订单已被领取或已取消").parseMode("MarkdownV2").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        order.setStatus(OrderStatus.Cancel.getCode());
        order.setReviewTgId(callbackQuery.getFrom().getId());
        int index = orderMapper.updateOrderById(order.getStatus(), order.getReviewTgId(), UUID.fromString(order.getOrderId()), LocalDateTime.now());
        if(index == 1) {
            //获取业务的金额
            Long businessId = order.getBusinessId();
            Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, businessId));
            BigDecimal money = business.getMoney();
            //退还给用户
            User selectOne = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, order.getTgId()));
            selectOne.setMoney(selectOne.getMoney().add(money));
            userMapper.updateById(selectOne);

            EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton("已取消")).build();
            try {
                bot.execute(editMessageReplyMarkup);
            } catch (TelegramApiException e) {
                System.out.println("忽略重复点击错误");
            }
            try {
                bot.execute(SendMessage.builder().chatId(order.getTgId()).text("订单已取消\n" +
                        "订单id：\n" +
                        "<code>" + order.getOrderId() + "</code>").parseMode("html").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        }
}
