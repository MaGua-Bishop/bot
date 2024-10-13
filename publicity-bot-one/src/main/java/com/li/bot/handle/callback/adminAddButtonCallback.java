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
public class adminAddButtonCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminAddButton";
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("创建互推导航按钮格式:\n<code>#按钮名 按钮名\n#链接地址 链接地址</code>").parseMode("html").build();
        bot.execute(sendMessage);
    }
}
