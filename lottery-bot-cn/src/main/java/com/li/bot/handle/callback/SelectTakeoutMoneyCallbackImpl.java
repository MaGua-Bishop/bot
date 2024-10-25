package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Takeout;
import com.li.bot.enums.TakeoutStatus;
import com.li.bot.mapper.TakeoutMapper;
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
public class SelectTakeoutMoneyCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "selectTakeoutMoney";
    }

    @Autowired
    private TakeoutMapper takeoutMapper ;

    private InlineKeyboardMarkup createButton(){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("返回").callbackData("DeleteMessage").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    @Override
    public  void execute(BotServiceImpl bot, CallbackQuery callbackQuery)  {

        List<Takeout> takeoutList = takeoutMapper.selectList(new LambdaQueryWrapper<Takeout>().eq(Takeout::getTgId, callbackQuery.getFrom().getId()));
        StringBuilder text = new StringBuilder();
        if(takeoutList.isEmpty()){
            text.append("暂无提现记录");
        }else {
            int i = 1 ;
            text.append("说明:序号|提现积分|提现状态\n");
            for (Takeout takeout : takeoutList) {
                text.append(i).append(". ").append(takeout.getMoney()).append(" ").append("<b>"+TakeoutStatus.getMessageByCode(takeout.getStatus())+"</b>\n\n");
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
