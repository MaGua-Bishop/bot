package com.li.bot.handle;

import com.li.bot.handle.callback.CallbackFactory;
import com.li.bot.handle.callback.ICallback;
import com.li.bot.handle.message.StartMessage;
import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Objects;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
public class CallbackQueryHandle {

    private CallbackQuery callbackQuery;

    private BotServiceImpl bot;

    private CallbackFactory callbackFactory;


    public CallbackQueryHandle(BotServiceImpl bot , CallbackQuery callbackQuery, CallbackFactory callbackFactory){
        this.callbackQuery = callbackQuery;
        this.bot = bot;
        this.callbackFactory = callbackFactory;
    }

    public void executeCallbackQuery() throws TelegramApiException {
        String data = callbackQuery.getData();
        System.out.println("按钮参数："+data);
        ICallback callback = callbackFactory.getCallback(data);
        if(Objects.nonNull(callback)){
            callback.execute(bot,callbackQuery);
            return ;
        }
        if(data.indexOf("/start") == 0){
            callbackFactory.getCallback("/start").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("updateConvoysTime:") == 0){
            callbackFactory.getCallback("updateConvoysTime").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("deleteConvoysTime:") == 0){
            callbackFactory.getCallback("deleteConvoysTime").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("selectConvoysInfo:") == 0){
            callbackFactory.getCallback("selectConvoysInfo").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("channelRequest:") == 0){
            callbackFactory.getCallback("channelRequest").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("adminYesAudi:") == 0){
            callbackFactory.getCallback("adminYesAudi").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("adminNoAudi:") == 0){
            callbackFactory.getCallback("adminNoAudi").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("deleteButton:") == 0){
            callbackFactory.getCallback("deleteButton").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("next:select:convoys:") ==0 || data.indexOf("prev:select:convoys:")==0){
            callbackFactory.getCallback("page:select:convoys").execute(bot,callbackQuery);
            return ;
        }
        if(data.indexOf("applyToJoin:") ==0){
            callbackFactory.getCallback("applyToJoin").execute(bot,callbackQuery);
            return ;
        }



    }

}
