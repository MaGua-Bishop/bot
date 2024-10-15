package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.AddBusinessSessionList;
import com.li.bot.utils.BotMessageUtils;
import com.li.bot.utils.UserInfoPageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class AdminSelectUserInfoCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminSelectUserInfo";
    }

    @Autowired
    private UserMapper userMapper ;


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String current = data.substring(data.lastIndexOf(":") + 1);

        Page<User> userPageInfo = new Page<>(Long.valueOf(current), UserInfoPageUtils.PAGESIZE);
        Page<User> userPage = userMapper.selectPage(userPageInfo, null);
        if(userPage.getRecords().isEmpty()){
            SendMessage message = SendMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .text("暂无用户")
                    .build();
            bot.execute(message);
            return;
        }
        EditMessageText messageText = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(BotMessageUtils.getAdminQueryUserInfo(userPage))
                .parseMode("html")
                .replyMarkup(UserInfoPageUtils.createInlineKeyboardButton(userPage))
                .build();
        bot.execute(messageText);


    }

}
