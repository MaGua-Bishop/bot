package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.vo.OrderAndBusinessVo;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import com.li.bot.utils.OrderPageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
@Slf4j
public class GroupSelectOrderCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "group:select:order";
    }

    @Autowired
    private OrderMapper orderMapper;


    private InlineKeyboardMarkup createInlineKeyboardButton(String  orderId){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("接单").callbackData("receive:order:"+orderId).build());
        buttonList.add(InlineKeyboardButton.builder().text("返回").callbackData("waiver:order:"+orderId).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        String orderId = data.substring(data.lastIndexOf(":") + 1);
        UUID uuid = UUID.fromString(orderId);

        Order order = orderMapper.getOrderByIdAndStatus(uuid,OrderStatus.PENDING.getCode());
        if (order == null) {
            try {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("该订单已被领取").build());
                return;
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

        CopyMessage copyMessage = new CopyMessage();
        copyMessage.setChatId(callbackQuery.getMessage().getChatId());
        copyMessage.setMessageId(order.getMessageId());
        copyMessage.setFromChatId(order.getTgId());
        copyMessage.setReplyMarkup(createInlineKeyboardButton(order.getOrderId()));
        try {
            bot.execute(copyMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


    }
}
