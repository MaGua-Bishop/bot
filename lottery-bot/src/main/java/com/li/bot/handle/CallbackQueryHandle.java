package com.li.bot.handle;

import com.li.bot.handle.callback.CallbackFactory;
import com.li.bot.handle.callback.ICallback;
import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
public class CallbackQueryHandle {

    private final Map<String, ICallback> callbackMap = new HashMap<>();

    public CallbackQueryHandle() {
        // 初始化映射
        callbackMap.put("updateConvoysTime:", callbackFactory.getCallback("updateConvoysTime"));
        // 可以在这里继续添加更多的前缀和回调
    }

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
        for (Map.Entry<String, ICallback> entry : callbackMap.entrySet()) {
            if (data.startsWith(entry.getKey())) {
                entry.getValue().execute(bot, callbackQuery);
                return;
            }
        }
    }

}
