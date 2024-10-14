package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.Invite;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
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
public class updateConvoysMembersUrlCallback implements ICallback {

    @Override
    public String getCallbackName() {
        return "updateConvoysMembersUrl";
    }


    @Autowired
    private InviteMapper inviteMapper ;


    @Autowired
    private UpdateConvoysSessionList updateConvoysSessionList ;

    @Override
    @Transactional
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String id = data.substring(data.lastIndexOf(":") + 1);

        Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getInviteId, Long.valueOf(id)));


        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(invite.getName() +"\n当前链接:"+invite.getLink()+"\n请输入新的链接地址").disableWebPagePreview(true).build();
        bot.execute(sendMessage);

        Convoys convoys = new Convoys();
        convoys.setConvoysId(invite.getInviteId());

        updateConvoysSessionList.addConvoysSession(callbackQuery.getFrom().getId(), convoys,1);






    }

}
