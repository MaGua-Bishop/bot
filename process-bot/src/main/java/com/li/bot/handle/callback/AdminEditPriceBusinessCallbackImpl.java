package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Business;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.AddOrderSessionList;
import com.li.bot.sessions.AdminEditSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class AdminEditPriceBusinessCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminEditPrice";
    }

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private AddOrderSessionList addOrderSessionList;

    @Autowired
    private AdminEditSessionList adminEditSessionList ;
    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {

        addOrderSessionList.removeUserSession(callbackQuery.getFrom().getId());

        String data = callbackQuery.getData();
        String businessId = data.substring(data.lastIndexOf(":") + 1);

        Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, Long.parseLong(businessId)));


        if (business != null){
            SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(business.getName()+"\n当前价格:"+business.getMoney()+"\n请输入新的价格").parseMode("html").build();
            bot.execute(sendMessage);
            adminEditSessionList.addUserSession(callbackQuery.getFrom().getId(),business,1);
        }




    }

}
