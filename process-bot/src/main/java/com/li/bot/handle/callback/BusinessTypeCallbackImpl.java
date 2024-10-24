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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class BusinessTypeCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "business:type";
    }

    @Autowired
    private BusinessMapper businessMapper;

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();

        // 定义正则表达式
        // 匹配 type 后面的值和 businessId 后面的值
        Pattern pattern = Pattern.compile("business:type:(\\d+):businessId:(\\d+)");

        // 创建 Matcher 对象
        Matcher matcher = pattern.matcher(data);

        // 查找匹配
        if (matcher.find()) {
            // 提取 type 后面的值
            String type = matcher.group(1);
            // 提取 businessId 后面的值
            String businessId = matcher.group(2);

            Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, Long.parseLong(businessId)));
            if(business != null){
                business.setType(Integer.parseInt(type));
                businessMapper.updateById(business);
                String t = "";
                if(type.equals("0")){
                    t= "机主";
                }else {
                    t = "杂单";
                }
                SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("<b>"+business.getName()+"</b>\n"+t+"\n设置成功").parseMode("html").build();
                try {
                    bot.execute(sendMessage);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

}
