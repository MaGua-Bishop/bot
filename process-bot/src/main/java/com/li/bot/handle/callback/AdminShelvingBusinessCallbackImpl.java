package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Business;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static net.sf.jsqlparser.parser.feature.Feature.delete;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class AdminShelvingBusinessCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminShelving";
    }

    @Autowired
    private BusinessMapper businessMapper;

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {

        String data = callbackQuery.getData();
        String businessId = data.substring(data.lastIndexOf(":") + 1);

        Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, Long.parseLong(businessId)));


        if (business != null){
            business.setIsShelving(!business.getIsShelving());

            String t = business.getIsShelving() ?  business.getName()+"已上架": business.getName()+"已下架";

            businessMapper.updateById(business);
            bot.execute(DeleteMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .build());
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(t).build());
        }


    }

}
