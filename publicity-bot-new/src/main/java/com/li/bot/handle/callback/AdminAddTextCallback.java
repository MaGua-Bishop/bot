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
public class AdminAddTextCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminAddText";
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请输入顶部文案\n格式:\n<code>#顶部文案 顶部文案</code>").parseMode("html").build();
        bot.execute(sendMessage);
    }
}
