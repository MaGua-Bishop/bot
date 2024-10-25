package com.li.bot.handle.callback;

import com.li.bot.entity.database.Recharge;
import com.li.bot.mapper.RechargeMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.UserTakeoutSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class UserTakeoutMoneyCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "userTakeoutMoney";
    }

    @Autowired
    private UserTakeoutSessionList userTakeoutSessionList ;

    @Override
    public  void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请输入提现积分").build();
        bot.execute(sendMessage);
        userTakeoutSessionList.addUserSession(callbackQuery.getFrom().getId(),null);

    }
}
