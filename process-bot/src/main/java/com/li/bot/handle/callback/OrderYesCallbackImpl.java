package com.li.bot.handle.callback;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.Workgroup;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.utils.BotMessageUtils;
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

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private FileService fileService ;

        private InlineKeyboardMarkup createInlineKeyboardButton(String orderId){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("领取").callbackData("receive:order:"+orderId).build());
        buttonList.add(InlineKeyboardButton.builder().text("放弃").callbackData("waiver:order:"+orderId).build());
        buttonList.add(InlineKeyboardButton.builder().text("取消订单").callbackData("admin:cancel:order:"+orderId).build());
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

    private List<String> getGroupList(){
        String fileContent = fileService.readFileContent();
        Workgroup workgroup = JSONObject.parseObject(fileContent, Workgroup.class);
        return workgroup.getGroupList();
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

                List<String> groupList = getGroupList();
                for (String groupChatId : groupList) {
//                    CopyMessage copyMessage = new CopyMessage();
//                    copyMessage.setChatId(groupChatId);
//                    copyMessage.setMessageId(order.getMessageId());
//                    copyMessage.setFromChatId(order.getTgId());
//                    copyMessage.setReplyMarkup(createInlineKeyboardButton(order.getOrderId()));

                    Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, order.getBusinessId()));

                    SendMessage sendMessage = SendMessage.builder().chatId(groupChatId).text(BotMessageUtils.getOrderInfoMessage(order.getCreateTime(), order.getMessageText(),business.getName(), order.getOrderId())).replyMarkup(createInlineKeyboardButton(order.getOrderId())).parseMode("html").build();
                    try {
                        bot.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        System.out.println("忽略重复点击错误");
                    }
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
