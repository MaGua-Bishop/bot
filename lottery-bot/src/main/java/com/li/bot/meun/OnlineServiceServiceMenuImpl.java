package com.li.bot.meun;

import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class OnlineServiceServiceMenuImpl implements IBotMenu{

    @Override
    public String getMenuName() {
        return "在线客服/Online service";
    }

    private String getUrlByUserName(String userName){
        userName = userName.replace("@","");
        return "https://t.me/"+userName;
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        String name = "@NANA_77NGGAME";
        sendMessage.setText("click:<a  href=\""+getUrlByUserName(name)+"\">"+name+"</a>");
        sendMessage.setParseMode("html");
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
