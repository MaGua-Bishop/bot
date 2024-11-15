package com.li.bot.handle.callback;

import com.li.bot.entity.database.UserMoney;
import com.li.bot.entity.database.vo.OrderBusinessVo;
import com.li.bot.enums.OrderStatus;
import com.li.bot.enums.UserMoneyStatus;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.mapper.UserMoneyMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.AdminEditSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class adminUpdateCodeCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminUpdateCode";
    }

    @Autowired
    private AdminEditSessionList adminEditSessionList;


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text("请发送新的二维码图片").build();
        bot.execute(sendMessage);
        adminEditSessionList.addUserSession(callbackQuery.getFrom().getId(), null, 2);
    }

}
