package com.li.bot.handle;

import com.li.bot.handle.message.IMessage;
import com.li.bot.handle.message.MessageFactory;
import com.li.bot.meun.BotMenuFactory;
import com.li.bot.meun.IBotMenu;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.UserStartKeyUtils;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Objects;

/**
 * @Author: li
 * @CreateTime: 2024-09-29
 * @Description: 处理文本消息
 */
public class MessageHandle {


    private BotServiceImpl bot ;

    private Message message ;

    private MessageFactory messageFactory ;

    private BotMenuFactory botMenuFactory ;

    public MessageHandle( BotServiceImpl bot, Message message, MessageFactory messageFactory, BotMenuFactory botMenuFactory) {
        this.bot = bot;
        this.message = message;
        this.messageFactory = messageFactory;
        this.botMenuFactory = botMenuFactory;
    }

    public void handle() throws TelegramApiException {
        String text = message.getText();
        System.out.println("用户输入的:"+text);
        String type = message.getChat().getType();
        if (type.equals("private")) {
            if(UserStartKeyUtils.userStartKeyList.contains(message.getText())){
                IBotMenu menu = botMenuFactory.getMenu(message.getText());
                if (Objects.nonNull(menu)) {
                    menu.execute(bot, message);
                    return;
                }
            }
        }
        if(text.startsWith("/start")&& message.getChat().getType().equals("private")){
            messageFactory.getMessage("start").execute(bot,message);
            return;
        }
        if(text.startsWith("/help")&& message.getChat().getType().equals("private")){
            messageFactory.getMessage("/help").execute(bot,message);
            return;
        }
        if(text.startsWith("/view")&& message.getChat().getType().equals("private")){
            messageFactory.getMessage("viewWinningUser").execute(bot,message);
            return;
        }
        if(text.startsWith("/exchange")&& message.getChat().getType().equals("private")){
            messageFactory.getMessage("exchange").execute(bot,message);
            return;
        }
        if((message.getText().startsWith("gift")) && message.getChat().getType().equals("private")){
            messageFactory.getMessage("CreateLotteryMessage").execute(bot,message);
            return;
        }
        if((message.getText().startsWith("adminGift")) && message.getChat().getType().equals("private")){
            messageFactory.getMessage("adminCreateLotteryMessage").execute(bot,message);
            return;
        }
        if((type.equals("group") || type.equals("supergroup")) && message.getText().equals("设置工作群")){
            messageFactory.getMessage("setReviewGroup").execute(bot,message);
            return;
        }
        if((type.equals("group") || type.equals("supergroup")) && message.getText().startsWith("#减少余额")){
            botMenuFactory.getMenu("减少余额").execute(bot,message);
            return;
        }

    }
}

