package com.li.bot.handle.message;

import com.li.bot.entity.database.Convoys;
import com.li.bot.service.impl.BotServiceImpl;
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
public class UpdateConvoysTimeMessage implements IMessage{

    @Override
    public String getMessageName() {
        return "updateConvoysTime";
    }

    private  final FleetService fleetService ;

    public UpdateConvoysTimeMessage(@Lazy FleetService fleetService) {
        this.fleetService = fleetService;
    }


    @Autowired
    private UpdateConvoysSessionList updateConvoysSessionList ;


    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {

        String text = message.getText();

        String regex = "^-?\\d+$";

        // 创建 Pattern 对象
        Pattern pattern = Pattern.compile(regex);


        // 创建Matcher对象
        Matcher matcher = pattern.matcher(text);



        // 检查是否找到匹配项
        if (matcher.matches()) {
            // 提取匹配的内容
            Integer name = Integer.valueOf(text.trim());
            UpdateConvoysSession userSession = updateConvoysSessionList.getUserSession(message.getFrom().getId());
            if(userSession != null){
                Convoys convoys = userSession.getBusiness();
                convoys.setIntervalMinutes(name);
                fleetService.updateFleetInterval(convoys);
                bot.execute(SendMessage.builder()
                        .chatId(message.getChatId())
                        .text(convoys.getName()+"修改成功\n"+"更新间隔为"+convoys.getIntervalMinutes()+"分钟")
                        .build());
            }
        }else {
            bot.execute(SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("输入有误")
                    .build());
        }
        updateConvoysSessionList.removeUserSession(message.getFrom().getId());
        }
}
