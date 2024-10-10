package com.li.bot.handle.menu;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.Workgroup;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class SetGroupMenuImpl implements IBotMenu{

    @Override
    public String getMenuName() {
        return "设置工作群";
    }

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private FileService fileService ;



    private boolean isAdmin(Message message){

        //判断用户是否管理员
        Long tgId = message.getFrom().getId();
        LambdaQueryWrapper<User> UserWrapper = new LambdaQueryWrapper<>();
        UserWrapper.eq(User::getTgId,tgId);
        User user = userMapper.selectOne(UserWrapper);

        Boolean admin = user.getIsAdmin();
        return admin;
    }

    @Override
    public void execute(BotServiceImpl bot, Message message) {
        boolean admin = isAdmin(message);
        System.out.println(admin);
        if(!admin){
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("您不是管理员，无法设置工作群").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return ;
        }

        String string = fileService.readFileContent();
        Workgroup workgroup = JSONObject.parseObject(string, Workgroup.class);
        boolean b = workgroup.getGroupList().contains(message.getChatId().toString());
        if(b){
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("该群已存在").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return ;
        }
        fileService.addGroupId(message.getChatId().toString());
        try {
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("设置成功").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }





    }
}
