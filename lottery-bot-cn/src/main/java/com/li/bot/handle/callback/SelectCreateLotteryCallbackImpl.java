package com.li.bot.handle.callback;

import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class SelectCreateLotteryCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "selectCreateLottery";
    }



    @Override
    @Transactional
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery)  {
        //用户创建红包记录
        System.out.println("用户创建红包记录");
    }
}
