package com.li.bot.meun;

import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class OfficialChannelServiceMenuImpl implements IBotMenu{

    @Override
    public String getMenuName() {
        return "供需频道";
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText("<a  href=\"https://t.me/chashe0\">@chashe0</a>");
        sendMessage.setParseMode("html");
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
