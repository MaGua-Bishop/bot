package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class SelectBusinessTypeCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "select:business:type";
    }

    @Autowired
    private BusinessMapper businessMapper ;

    private InlineKeyboardMarkup createInlineKeyboardButton(Integer type){

        //查出全部业务只要名称和主键
        LambdaQueryWrapper<Business> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Business::getBusinessId,Business::getName);
        wrapper.eq(Business::getStatus,type);
        List<Business> businesses = businessMapper.selectList(wrapper);

        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        for (Business business : businesses) {
            buttonList.add(InlineKeyboardButton.builder().text(business.getName()).callbackData("businessId:"+String.valueOf(business.getBusinessId())).build());
        }

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);


        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();

        return inlineKeyboardMarkup;
    }



    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery){
        String data = callbackQuery.getData();

        String substring = data.substring(data.lastIndexOf(":") + 1);


        InlineKeyboardMarkup inlineKeyboardButton = createInlineKeyboardButton(Integer.valueOf(substring));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(callbackQuery.getMessage().getChatId());
        String text = "" ;
        if("0".equals(substring)){
            text = "请选择需要了解的热门业务";
        }else {
            text = "请选择需要了解的冷门业务";
        }
        sendMessage.setText(text);
        sendMessage.enableMarkdownV2(true);
        sendMessage.setReplyMarkup(inlineKeyboardButton);
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
