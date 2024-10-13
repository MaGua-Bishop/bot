package com.li.bot.handle;

import com.li.bot.handle.message.IMessage;
import com.li.bot.handle.message.MessageFactory;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.UpdateConvoysSession;
import com.li.bot.sessions.UpdateConvoysSessionList;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-29
 * @Description: 处理文本消息
 */
public class MessageHandle {


    private BotServiceImpl bot ;

    private Message message ;
    private MessageFactory messageFactory ;

    private UpdateConvoysSessionList updateConvoysSessionList ;

    public MessageHandle(BotServiceImpl bot, Message message, MessageFactory messageFactory, UpdateConvoysSessionList updateConvoysSessionList) {
        this.bot = bot;
        this.message = message;
        this.messageFactory = messageFactory;
        this.updateConvoysSessionList = updateConvoysSessionList;
    }

    public void handle() throws TelegramApiException {
        String text = message.getText();

        UpdateConvoysSession userSession = updateConvoysSessionList.getUserSession(message.getFrom().getId());
        if(userSession != null && userSession.getType() == 0){
            messageFactory.getMessage("updateConvoysTime").execute(bot,message);
            return ;
        }
        if(userSession != null && userSession.getType() == 1){
            messageFactory.getMessage("updateConvoysName").execute(bot,message);
            return ;
        }
        if(text.startsWith("#车队标题")){
            messageFactory.getMessage("addConvoys").execute(bot,message);
        }
        if(text.startsWith("#按钮名")){
            messageFactory.getMessage("addButton").execute(bot,message);
        }
        if(text.startsWith("#顶部文案")){
            messageFactory.getMessage("addText").execute(bot,message);
        }
        if(text.startsWith("#底部文案")){
            messageFactory.getMessage("addBottomText").execute(bot,message);
        }

        IMessage m = messageFactory.getMessage(text);
        if(m !=null){
            m.execute(bot,message);
            return ;
        }
    }
}

