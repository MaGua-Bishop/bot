package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.UserMapper;
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

    @Autowired
    private UserMapper userMapper ;

    private InlineKeyboardMarkup createInlineKeyboardButton(Long tgId,Integer type){
        //查出全部业务只要名称和主键
        LambdaQueryWrapper<Business> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Business::getBusinessId,Business::getName,Business::getIsShelving);
        wrapper.eq(Business::getStatus,type);
        List<Business> businesses = businessMapper.selectList(wrapper);

        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));
        if(user != null && user.getIsAdmin() ){
            for (Business business : businesses) {
                if(business.getIsShelving()){
                    buttonList.add(InlineKeyboardButton.builder().text("("+"上架中"+")"+business.getName()).callbackData("businessId:"+String.valueOf(business.getBusinessId())).build());
                }else {
                    buttonList.add(InlineKeyboardButton.builder().text("("+"下架中"+")"+business.getName()).callbackData("businessId:"+String.valueOf(business.getBusinessId())).build());
                }
            }
        }else {
            for (Business business : businesses) {
                if(business.getIsShelving()){
                    buttonList.add(InlineKeyboardButton.builder().text(business.getName()).callbackData("businessId:"+String.valueOf(business.getBusinessId())).build());
                }
            }
        }



        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);


        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();

        return inlineKeyboardMarkup;
    }



    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery){
        String data = callbackQuery.getData();

        String substring = data.substring(data.lastIndexOf(":") + 1);


        InlineKeyboardMarkup inlineKeyboardButton = createInlineKeyboardButton(callbackQuery.getFrom().getId(),Integer.valueOf(substring));

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
