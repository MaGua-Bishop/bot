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
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("创建车队格式:\n<code>#车队名 车队名\n#车队文案 车队文案\n#车队容量 车队容量\n#频道最低订阅数量 频道订阅数量</code>").parseMode("html").build();
        bot.execute(sendMessage);
    }
}
