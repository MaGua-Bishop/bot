package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.AddOrderSessionList;
import com.li.bot.sessions.OrderSession;
import com.li.bot.sessions.enums.OrderSessionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
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
public class NextStepCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "nextstep";
    }

    @Autowired
    private AddOrderSessionList addOrderSessionList ;


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        //解析业务id
//        String data = callbackQuery.getData();
//        Long businessId = Long.parseLong(data.substring(data.lastIndexOf(":") + 1));
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text("请输入所需内容").build();

        bot.execute(sendMessage);
        OrderSession userSession = addOrderSessionList.getUserSession(callbackQuery.getFrom().getId());
        userSession.setState(OrderSessionState.WAITING_FOR_USER_MESSAGE);

    }

}
