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

    private CallbackQuery callbackQuery;

    private BotServiceImpl bot;

    private CallbackFactory callbackFactory;


    public CallbackQueryHandle(BotServiceImpl bot , CallbackQuery callbackQuery, CallbackFactory callbackFactory){
        this.callbackQuery = callbackQuery;
        this.bot = bot;
        this.callbackFactory = callbackFactory;
        //充值
        callbackMap.put("user:recharge:", callbackFactory.getCallback("userRecharge"));
        //用户提现
        callbackMap.put("userTakeoutMoney", callbackFactory.getCallback("userTakeoutMoney"));
        //用户查看提现记录
        callbackMap.put("selectTakeoutMoney", callbackFactory.getCallback("selectTakeoutMoney"));
        //用户查看发布红包记录
        callbackMap.put("selectCreateLottery", callbackFactory.getCallback("selectCreateLottery"));
        //用户查看领取红包记录
        callbackMap.put("selectReceiveLottery", callbackFactory.getCallback("selectReceiveLottery"));
        //管理员审核提现
        callbackMap.put("admin:review:takeout:type:", callbackFactory.getCallback("adminReviewTakeout"));
        //红包条件
        callbackMap.put("set:lottery:condition:type:", callbackFactory.getCallback("setLotteryCondition"));
        //删除消息
        callbackMap.put("DeleteMessage", callbackFactory.getCallback("DeleteMessage"));
        //管理员发包
        callbackMap.put("admin:set:lottery:condition:type:", callbackFactory.getCallback("adminSetLotteryCondition"));
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
