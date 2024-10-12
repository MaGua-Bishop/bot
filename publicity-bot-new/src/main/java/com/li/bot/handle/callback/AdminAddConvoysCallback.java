package com.li.bot.handle.callback;

import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class AdminAddConvoysCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminAddConvoys";
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("创建车队格式:\n<code>#车队标题 车队标题\n#车队介绍 车队介绍\n#车队容量 车队容量\n#最低订阅数量 订阅数量\n#最低阅读数量 阅读数量</code>").parseMode("html").build();
        bot.execute(sendMessage);
    }
}
