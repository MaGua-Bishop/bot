package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.AddOrderSessionList;
import com.li.bot.sessions.AdminEditSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class AdminEditTypeBusinessCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminEditType";
    }

    @Autowired
    private BusinessMapper businessMapper;

    private InlineKeyboardMarkup createInlineKeyboardButton(Long businessId ) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("机主").callbackData("business:type:" +"0"+":businessId:"+businessId).build());
        buttonList.add(InlineKeyboardButton.builder().text("杂单").callbackData("business:type:" +"1"+":businessId:"+businessId).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String businessId = data.substring(data.lastIndexOf(":") + 1);

        Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, Long.parseLong(businessId)));
        if (business != null){
            String t = "" ;
            if(business.getType() == 0){
                t = "机主";
            }else {
                t = "杂单";
            }
            SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(business.getName()+"\n当前业务类型:"+t+"\n请选择新的类型").replyMarkup(createInlineKeyboardButton(business.getBusinessId())).parseMode("html").build();
            bot.execute(sendMessage);
        }


    }

}
