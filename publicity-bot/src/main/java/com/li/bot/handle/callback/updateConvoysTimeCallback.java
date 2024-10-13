package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.*;
import com.li.bot.mapper.*;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.UpdateConvoysSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class updateConvoysTimeCallback implements ICallback {

    @Override
    public String getCallbackName() {
        return "updateConvoysTime";
    }


    @Autowired
    private ConvoysMapper convoysMapper ;


    @Autowired
    private UpdateConvoysSessionList updateConvoysSessionList ;

    @Override
    @Transactional
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String id = data.substring(data.lastIndexOf(":") + 1);

        Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, Long.valueOf(id)));

        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(convoys.getName()+"\n当前车队推送间隔:"+convoys.getIntervalMinutes()+"分钟\n"+"请输入新的推送时间(输入分钟数)").build();
        bot.execute(sendMessage);

        updateConvoysSessionList.addConvoysSession(callbackQuery.getFrom().getId(), convoys);






    }

}
