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
public class adminAddBottomTextCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminAddBottomText";
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请输入底部文案\n格式:\n<code>#底部文案\n#文案 输入文案\n#AD 链接 文字</code>\n提示:文案只能有一个,AD不限制.请按格式填写").parseMode("html").build();
        bot.execute(sendMessage);
    }
}
