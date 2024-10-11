package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Business;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class AdminDeleteBusinessCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminDeleteBusiness";
    }

    @Autowired
    private BusinessMapper businessMapper;

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {

        String data = callbackQuery.getData();
        String businessId = data.substring(data.lastIndexOf(":") + 1);

        int delete = businessMapper.delete(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, Long.valueOf(businessId)));
        if (delete > 0) {
            SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("删除成功").build();
            bot.execute(sendMessage);

            bot.execute(DeleteMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .build());

        }


    }

}
