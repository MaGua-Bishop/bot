package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Reply;
import com.li.bot.enums.ReplyStatus;
import com.li.bot.mapper.ReplyMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
@Slf4j
public class SelectReplyRecordsCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "select:reply:records";
    }

//    @Autowired
//    private ReplyMapper replyMapper ;

    // 将订单根据状态分组
//    public Map<Integer, List<Reply>> groupOrdersByStatus(List<Reply> replyList) {
//        return replyList.stream()
//                .collect(Collectors.groupingBy(Reply::getStatus));
//    }

    private InlineKeyboardMarkup createInlineKeyboardButton(){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("查看待处理").callbackData("select:reply:record:0").build());
        buttonList.add(InlineKeyboardButton.builder().text("查看已回复").callbackData("select:reply:record:1").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }



    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {

        SendMessage message = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请选择").replyMarkup(createInlineKeyboardButton()).build();

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


    }
}
