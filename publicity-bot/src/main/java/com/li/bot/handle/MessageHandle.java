package com.li.bot.handle;

import com.li.bot.handle.message.IMessage;
import com.li.bot.handle.message.MessageFactory;
import com.li.bot.service.impl.BotServiceImpl;
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

    public MessageHandle(BotServiceImpl bot, Message message, MessageFactory messageFactory) {
        this.bot = bot;
        this.message = message;
        this.messageFactory = messageFactory;
    }

    public void handle() throws TelegramApiException {
        String text = message.getText();

        if(text.startsWith("#车队名")){
            messageFactory.getMessage("addConvoys").execute(bot,message);
        }
        if(text.startsWith("#按钮名")){
            messageFactory.getMessage("addButton").execute(bot,message);
        }

        IMessage m = messageFactory.getMessage(text);
        if(m !=null){
            m.execute(bot,message);
            return ;
        }
    }
}

