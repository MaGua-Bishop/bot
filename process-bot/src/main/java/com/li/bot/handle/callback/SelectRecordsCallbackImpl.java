package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.ReplyMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
@Slf4j
public class SelectRecordsCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "select:reply:record";
    }

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private OrderMapper orderMapper ;

    @Autowired
    private BusinessMapper businessMapper ;



    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();

        String substring = data.substring(data.lastIndexOf(":") + 1);

        Integer status = 0;
        String type = "";
        if("0".equals(substring)){
            status = 0;
            type = "待回单";
        }else if("1".equals(substring)){
            status = 1;
            type = "已回单";
        }

        Long tgId = callbackQuery.getFrom().getId();

        List<Reply> replyList = replyMapper.getReplyListByStuta(tgId, status);
        if(replyList.isEmpty()){
            try {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("您还未接单").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        for (Reply reply : replyList) {
            String orderId = reply.getOrderId();
            UUID uuid = UUID.fromString(orderId);
            Order order = orderMapper.getOrderByOrderId(uuid);

            Long businessId = order.getBusinessId();
            Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, businessId));
            String name = business.getName();

//            CopyMessage copyMessage = new CopyMessage();
//            copyMessage.setChatId(callbackQuery.getMessage().getChatId());
//            copyMessage.setMessageId(order.getMessageId());
//            copyMessage.setFromChatId(order.getTgId());
            SendMessage message = SendMessage.builder().text(BotMessageUtils.getUserReplyMessage(type,reply,order.getMessageText(),name)).parseMode("html").chatId(callbackQuery.getMessage().getChatId()).build();
            try {
//                bot.execute(copyMessage);
                bot.execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
