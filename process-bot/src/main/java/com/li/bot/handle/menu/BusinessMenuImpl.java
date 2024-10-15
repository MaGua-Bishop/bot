package com.li.bot.handle.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class BusinessMenuImpl implements IBotMenu{

    @Override
    public String getMenuName() {
        return "报单";
    }

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private BusinessMapper businessMapper ;


    private InlineKeyboardMarkup createInlineKeyboardButton(Message message){

        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        buttonList.add(InlineKeyboardButton.builder().text("热门业务").callbackData("select:business:type:0").build());
        buttonList.add(InlineKeyboardButton.builder().text("冷门业务").callbackData("select:business:type:1").build());


        //判断用户是否管理员
        Long tgId = message.getFrom().getId();
        LambdaQueryWrapper<User> UserWrapper = new LambdaQueryWrapper<>();
        UserWrapper.eq(User::getTgId,tgId);
        User user = userMapper.selectOne(UserWrapper);

        if(user == null){
            user = new User();
            user.setTgId(tgId);
            user.setTgName(message.getFrom().getFirstName()+message.getFrom().getLastName());
            userMapper.insert(user);
        }else {
            if(user.getIsAdmin()){
                buttonList.add(InlineKeyboardButton.builder().text("添加业务").callbackData("adminAddBusiness").build());
                buttonList.add(InlineKeyboardButton.builder().text("提取未领取订单").callbackData("adminSelectListBusiness:0").build());
                buttonList.add(InlineKeyboardButton.builder().text("提取未回复订单").callbackData("adminSelectListBusiness:1").build());
            }
        }



        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);


        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();

        return inlineKeyboardMarkup;
    }

    @Override
    public void execute(BotServiceImpl bot, Message message) {
        InlineKeyboardMarkup inlineKeyboardButton = createInlineKeyboardButton(message);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText("请选择热门或冷门业务");
        sendMessage.enableMarkdownV2(true);
        sendMessage.setReplyMarkup(inlineKeyboardButton);
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
