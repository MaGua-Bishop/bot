package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.User;
import com.li.bot.sessions.AddBusinessSessionList;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class AdminAddBusinessCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminAddBusiness";
    }

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private AddBusinessSessionList addBusinessSessionList;

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        Long tgId = callbackQuery.getFrom().getId();
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getTgId,tgId);
        User user = userMapper.selectOne(userWrapper);
        if(user.getIsAdmin()){
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId().toString()).text("请输入业务名称").build());
            addBusinessSessionList.addUserSession(tgId);
        }

    }

}
