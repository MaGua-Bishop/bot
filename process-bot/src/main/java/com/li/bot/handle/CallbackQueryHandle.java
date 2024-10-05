package com.li.bot.handle;

import com.google.common.collect.Lists;
import com.li.bot.handle.callback.CallbackFactory;
import com.li.bot.handle.callback.ICallback;
import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
public class CallbackQueryHandle {

    private CallbackQuery callbackQuery;

    private BotServiceImpl bot;

    private CallbackFactory callbackFactory ;

    public CallbackQueryHandle(BotServiceImpl bot , CallbackQuery callbackQuery,CallbackFactory callbackFactory){
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
        if(data.indexOf("receive:order:") == 0){
            callbackFactory.getCallback("receiveOrder").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("waiver:order:") == 0){
            callbackFactory.getCallback("waiverOrder").execute(bot,callbackQuery);
            return;
        }

        if(data.indexOf("select:businessId:") == 0){
            callbackFactory.getCallback("selectBusiness").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("businessId:") == 0){
            callbackFactory.getCallback("allBusiness").execute(bot,callbackQuery);
            return;
        }
        if(data.indexOf("user:send:businessId:") == 0){
            callbackFactory.getCallback("sendOrder").execute(bot,callbackQuery);
            return ;
        }
        if(data.indexOf("order:yes:") ==0){
            callbackFactory.getCallback("order:yes").execute(bot,callbackQuery);
            return ;
        }
        if(data.indexOf("order:no:") ==0){
            callbackFactory.getCallback("order:no").execute(bot,callbackQuery);
            return ;
        }
        if(data.indexOf("send:order:") ==0){
            callbackFactory.getCallback("send:order").execute(bot,callbackQuery);
            return ;
        }
        if(data.indexOf("next:select:order:") ==0 || data.indexOf("prev:select:order:")==0){
            callbackFactory.getCallback("page:select:order").execute(bot,callbackQuery);
            return ;
        }
        if(data.indexOf("group:select:order:") == 0){
            callbackFactory.getCallback("group:select:order").execute(bot,callbackQuery);
            return ;
        }
        //select:order:records:
        if(data.indexOf("select:reply:records:") == 0){
            callbackFactory.getCallback("select:reply:records").execute(bot,callbackQuery);
            return ;
        }
        if(data.indexOf("select:reply:record:") == 0){
            callbackFactory.getCallback("select:reply:record").execute(bot,callbackQuery);

        }

//        bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("未找到对应按钮回调").build());



    }

}
