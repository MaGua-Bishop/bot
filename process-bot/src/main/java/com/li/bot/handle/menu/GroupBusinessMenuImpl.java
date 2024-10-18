package com.li.bot.handle.menu;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.Workgroup;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class GroupBusinessMenuImpl implements IBotMenu{

    @Override
    public String getMenuName() {
        return "提取业务";
    }

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private BusinessMapper businessMapper ;

    @Autowired
    private OrderMapper orderMapper ;
    private InlineKeyboardMarkup createInlineKeyboardButton(Long businessId){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("一键领取").callbackData("list:order:receive:"+businessId).build());
        buttonList.add(InlineKeyboardButton.builder().text("放弃").callbackData("waiver:order:1").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) {
        String text = message.getText();

        String regex = "#提取业务 (\\S+)";

        // 创建 Pattern 对象
        Pattern pattern = Pattern.compile(regex);

        // 创建 Matcher 对象
        Matcher matcher = pattern.matcher(text);
        // 查找匹配项
        if (matcher.find()) {
            // 提取匹配的第一组
            String businessName = matcher.group(1);

            Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getName, businessName));
            if(business == null){
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("业务名错误").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            List<Order> orderList = orderMapper.getOrderByBusinessId(business.getBusinessId());
            if(orderList.isEmpty()){
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("没有该业务订单").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            StringBuilder str = new StringBuilder();
            int index = 1 ;
            str.append("业务:<b>"+businessName+"</b>所有未领取订单信息\n");
            str.append("序号\t报单内容\n");
            List<String> list = new ArrayList<>();
            for (Order order : orderList) {
                list.add(order.getOrderId());
                str.append(index+".\t<b>"+order.getMessageText()+"</b>\n");
                index++ ;
            }
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text(str.toString()).replyMarkup(createInlineKeyboardButton(business.getBusinessId())).parseMode("html").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("格式不正确，请输入：#提取业务 业务名称").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }



    }
}
