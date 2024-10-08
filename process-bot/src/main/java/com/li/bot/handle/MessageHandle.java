package com.li.bot.handle;

import com.li.bot.handle.key.BotKeyFactory;
import com.li.bot.handle.key.IKeyboard;
import com.li.bot.handle.menu.BotMenuFactory;
import com.li.bot.handle.menu.IBotMenu;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.UserStartKeyUtils;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Objects;

/**
 * @Author: li
 * @CreateTime: 2024-09-29
 * @Description: 处理文本消息
 */
public class MessageHandle {

    private BotKeyFactory botKeyFactory ;

    private BotMenuFactory botMenuFactory ;

    private BotServiceImpl bot ;

    private Message message ;

    public MessageHandle(BotKeyFactory botKeyFactory,BotMenuFactory botMenuFactory, BotServiceImpl bot, Message message) {
        this.botKeyFactory = botKeyFactory;
        this.botMenuFactory = botMenuFactory;
        this.bot = bot;
        this.message = message;
    }

    public void executeMessage() throws TelegramApiException {
        String type = message.getChat().getType();
        if (type.equals("private")) {
            if(UserStartKeyUtils.userStartKeyList.contains(message.getText())){
                IBotMenu menu = botMenuFactory.getMenu(message.getText());
                if (Objects.nonNull(menu)) {
                    menu.execute(bot, message);
                }
            }
        }else if(type.equals("group") && message.getText().equals("设置工作群")){
            botMenuFactory.getMenu("设置工作群").execute(bot, message);
        }
    }

    public void handle() throws TelegramApiException {
        String text = message.getText();

        if(text.indexOf("/start") == 0){
            text = "/start";
        }
        IKeyboard key = botKeyFactory.getKey(text);
        if(key != null){
            key.execute(bot,message);
        }else {
            executeMessage();
        }
        }
    }

