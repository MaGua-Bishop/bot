package com.li.bot.meun;

import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class PersonalCenterServiceMenuImpl implements IBotMenu{

    @Override
    public String getMenuName() {
        return "Personal Center";
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) {

        User from = message.getFrom();
        Long id = from.getId();
        String username = from.getUserName().isEmpty()? from.getFirstName()+ from.getLastName():from.getUserName();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        String balancePoints = "0";
        sendMessage.setText("Personal Center\nusername:<a href=\"tg://user?id="+id+"\">"+username+"</a>\nID:<code>"+id+"</code>\nBalance:"+balancePoints+"");
        sendMessage.setParseMode("html");
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
