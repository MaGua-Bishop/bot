package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.AddOrderSessionList;
import com.li.bot.utils.BotMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
@Slf4j
public class SendOrderCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "send:order";
    }

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private AddOrderSessionList addOrderSessionList;


    private InlineKeyboardMarkup createInlineKeyboardButton(String orderId) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("机主后台").callbackData("order:yes:" + orderId+":type:"+0).build());
        buttonList.add(InlineKeyboardButton.builder().text("杂单后台").callbackData("order:yes:" + orderId+":type:"+1).build());
        buttonList.add(InlineKeyboardButton.builder().text("不通过").callbackData("order:no:" + orderId).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private void sendMessageAdmin(User user, Business business, Order order, BotServiceImpl bot) {

        List<User> useList = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getIsAdmin, true));

        Integer type = business.getType();
        useList.forEach(u -> {
            String desc = "";
            if(type == 0){
                desc = "机主后台";
            }else {
                desc = "杂单后台";
            }
            SendMessage sendMessage = SendMessage.builder().chatId(u.getTgId()).text(BotMessageUtils.getOrderInfoMessage(new Date(), order.getMessageText(), business.getName(), order.getOrderId())+"\n注意属于:<b>"+desc+"</b>").replyMarkup(createInlineKeyboardButton(order.getOrderId())).parseMode("html").build();
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            log.info("已转发给管理员:" + u.getTgName());
        });

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
        System.out.println(data);
        Long tgId = callbackQuery.getFrom().getId();

        if (!"send:order:yes".equals(data)) {
            addOrderSessionList.removeUserSession(tgId);
            try {
                bot.execute(SendMessage.builder().chatId(tgId).text("操作已取消").build());
                bot.execute(DeleteMessage.builder()
                        .chatId(callbackQuery.getMessage().getChatId())
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        Business business = addOrderSessionList.getUserSession(tgId).getBusiness();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));

        BigDecimal businessMoney = business.getMoney();
        BigDecimal userMoney = user.getMoney();

        //用户金额 - 订单金额
        BigDecimal remainingMoney = userMoney.subtract(businessMoney);
        if (remainingMoney.compareTo(BigDecimal.ZERO) < 0) {
            bot.execute(DeleteMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .build());
            try {
                bot.execute(SendMessage.builder().chatId(tgId).text("抱歉 您的余额不足").parseMode("MarkdownV2").build());
                bot.execute(SendMessage.builder().chatId(tgId).text("操作已取消").build());
                addOrderSessionList.removeUserSession(tgId);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        Order order = new Order();
        order.setMessageText(business.getMessageText());
        order.setTgId(tgId);
        order.setBusinessId(business.getBusinessId());
        int insert = orderMapper.insertOrder(order);
        user.setMoney(user.getMoney().subtract(businessMoney));
        int update = 0;
        if (insert == 1) {
            update = userMapper.updateById(user);
        }
        if (insert == 1 && update == 1) {
            try {
                bot.execute(SendMessage.builder().chatId(tgId).text("报单成功\n回执id\n" + "<code>" + order.getOrderId() + "</code>").parseMode("html").build());


                //转发给管理员
                sendMessageAdmin(user, business, order, bot);


                EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton("报单成功")).build();
                bot.execute(editMessageReplyMarkup);


            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                bot.execute(SendMessage.builder().chatId(tgId).text("报单失败").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        addOrderSessionList.removeUserSession(tgId);


    }
}
