package com.li.bot.meun;

import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class OnlineServiceServiceMenuImpl implements IBotMenu{

    @Override
    public String getMenuName() {
        return "茶社大群";
    }

    private String getUrlByUserName(String userName){
        userName = userName.replace("@","");
        return "https://t.me/"+userName;
    }

    private String randomList(){
        return "https://t.me/chashe666666";
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        String url = randomList();
        String name ="@"+ url.substring(url.lastIndexOf("/")+1);
        sendMessage.setText("<a  href=\""+url+"\">"+name+"</a>");
        sendMessage.setParseMode("html");
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
