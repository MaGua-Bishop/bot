package com.li.bot.handle.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.vo.BusinessVo;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
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


        //查出全部业务只要名称和主键
        LambdaQueryWrapper<Business> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Business::getBusinessId,Business::getName);
        List<Business> businesses = businessMapper.selectList(wrapper);

        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        for (Business business : businesses) {
            buttonList.add(InlineKeyboardButton.builder().text(business.getName()).callbackData("businessId:"+String.valueOf(business.getBusinessId())).build());
        }
        //判断用户是否管理员
        Long tgId = message.getFrom().getId();
        LambdaQueryWrapper<User> UserWrapper = new LambdaQueryWrapper<>();
        UserWrapper.eq(User::getTgId,tgId);
        User user = userMapper.selectOne(UserWrapper);

        if(user == null){
            user = new User();
            user.setTgId(tgId);
            user.setTgName(message.getFrom().getLastName()+message.getFrom().getFirstName());
            userMapper.insert(user);
        }else {
            if(user.getIsAdmin()){
                buttonList.add(InlineKeyboardButton.builder().text("添加业务").callbackData("adminAddBusiness").build());
            }
        }



        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);


        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();

        return inlineKeyboardMarkup;
    }

    @Override
    public void execute(BotServiceImpl bot, Message message) {
        InlineKeyboardMarkup inlineKeyboardButton = createInlineKeyboardButton(message);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText("请选择需要了解的业务");
        sendMessage.enableMarkdownV2(true);
        sendMessage.setReplyMarkup(inlineKeyboardButton);
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
