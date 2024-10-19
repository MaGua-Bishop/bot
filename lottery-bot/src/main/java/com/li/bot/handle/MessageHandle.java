package com.li.bot.handle;

import com.li.bot.handle.message.IMessage;
import com.li.bot.handle.message.MessageFactory;
import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
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

    public MessageHandle( BotServiceImpl bot, Message message, MessageFactory messageFactory) {
        this.bot = bot;
        this.message = message;
        this.messageFactory = messageFactory;
    }

    public void handle() throws TelegramApiException {
        String text = message.getText();
        System.out.println("用户输入的:"+text);
//        IMessage m = messageFactory.getMessage(text);
        if(text.startsWith("/start")&& message.getChat().getType().equals("private")){
            messageFactory.getMessage("start").execute(bot,message);
            return;
        }
        if(text.startsWith("/help")&& message.getChat().getType().equals("private")){
            messageFactory.getMessage("/help").execute(bot,message);
            return;
        }

//        if(m !=null){
//            m.execute(bot,message);
//            return ;
//        }
        if(text.startsWith("/view")&& message.getChat().getType().equals("private")){
            messageFactory.getMessage("viewWinningUser").execute(bot,message);
            return;
        }
        if(text.startsWith("/exchange")&& message.getChat().getType().equals("private")){
            messageFactory.getMessage("adminExchange").execute(bot,message);
            return;
        }
        if((message.getChat().getType().equals("group") || message.getChat().getType().equals("supergroup")) &&message.getText().startsWith("gift")){
            messageFactory.getMessage("GroupAdminLotteryMessage").execute(bot,message);
            return;
        }
    }
}

