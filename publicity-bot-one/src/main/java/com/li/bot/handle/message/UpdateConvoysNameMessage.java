package com.li.bot.handle.message;

import com.li.bot.entity.database.Convoys;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FleetService;
import com.li.bot.sessions.UpdateConvoysSession;
import com.li.bot.sessions.UpdateConvoysSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class UpdateConvoysNameMessage implements IMessage{

    @Override
    public String getMessageName() {
        return "updateConvoysName";
    }

    private  final FleetService fleetService ;

    public UpdateConvoysNameMessage(@Lazy FleetService fleetService) {
        this.fleetService = fleetService;
    }


    @Autowired
    private UpdateConvoysSessionList updateConvoysSessionList ;


    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        String text = message.getText();
        UpdateConvoysSession userSession = updateConvoysSessionList.getUserSession(message.getFrom().getId());
        if(userSession != null){
            Convoys convoys = userSession.getBusiness();
            convoys.setName(text);
            fleetService.updateFleetInterval(convoys);
            bot.execute(SendMessage.builder()
                    .chatId(message.getChatId())
                    .text(convoys.getName()+"修改成功\n"+"更新为"+convoys.getName())
                    .build());
        }
        updateConvoysSessionList.removeUserSession(message.getFrom().getId());
        }
}
