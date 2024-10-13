package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Convoys;
import com.li.bot.mapper.ConvoysMapper;
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
public class updateConvoysNameCallback implements ICallback {

    @Override
    public String getCallbackName() {
        return "updateConvoysName";
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

        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("当前车队标题:"+convoys.getName()+"\n"+"请输入新的车队标题").build();
        bot.execute(sendMessage);

        updateConvoysSessionList.addConvoysSession(callbackQuery.getFrom().getId(), convoys,1);






    }

}
