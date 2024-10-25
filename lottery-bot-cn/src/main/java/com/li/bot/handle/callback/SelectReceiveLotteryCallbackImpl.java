package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.LotteryInfo;
import com.li.bot.entity.database.Takeout;
import com.li.bot.enums.LotteryInfoStatus;
import com.li.bot.enums.TakeoutStatus;
import com.li.bot.mapper.LotteryInfoMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class SelectReceiveLotteryCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "selectReceiveLottery";
    }

    @Autowired
    private LotteryInfoMapper lotteryInfoMapper;

    private InlineKeyboardMarkup createButton(){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("返回").callbackData("DeleteMessage").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    @Transactional
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {
        //用户领取红包记录
        List<LotteryInfo> lotteryInfoList = lotteryInfoMapper.getLotteryInfoListByTgId(callbackQuery.getFrom().getId());
        StringBuilder text = new StringBuilder();
        if(lotteryInfoList.isEmpty()){
            text.append("暂无领取红包记录");
        }else {
            int i = 1 ;
            text.append("说明:序号|中奖id|积分|状态\n");
            for (LotteryInfo lotteryInfo : lotteryInfoList) {
                text.append(i).append(". ").append("<code>"+lotteryInfo.getLotteryId()+"</code>").append(" ").append(lotteryInfo.getMoney()).append(" ").append("<b>"+LotteryInfoStatus.getMessageByCode(lotteryInfo.getStatus())+"</b>\n\n");
                i++ ;
            }
        }
        //用户提现记录
        SendMessage messageText = SendMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .text(String.valueOf(text))
                .parseMode("html")
                .replyMarkup(createButton())
                .build();
        try {
            bot.execute(messageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
