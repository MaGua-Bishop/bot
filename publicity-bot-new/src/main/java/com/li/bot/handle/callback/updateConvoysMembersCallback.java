package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Button;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.enums.ConvoysInviteStatus;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.UpdateConvoysSessionList;
import com.li.bot.utils.UnitConversionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class updateConvoysMembersCallback implements ICallback {

    @Override
    public String getCallbackName() {
        return "updateConvoysMembers";
    }


    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;



    @Autowired
    private InviteMapper inviteMapper ;


    private InlineKeyboardMarkup createInlineKeyboardButton(Long convoysId){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, convoysId).eq(ConvoysInvite::getStatus, ConvoysInviteStatus.BOARDED.getCode()));
        if(convoysInviteList.isEmpty()){
            buttonList.add(InlineKeyboardButton.builder().text("暂无成员").callbackData("null").build());
        }else {
            List<Invite> inviteList = inviteMapper.getInviteListByIds(convoysInviteList.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList()));
            for (Invite invite : inviteList) {
                buttonList.add(InlineKeyboardButton.builder().text(invite.getName()+"|"+ UnitConversionUtils.toThousands(invite.getMemberCount())).callbackData("updateConvoysMembersUrl:"+invite.getInviteId()).build());
            }
        }
        buttonList.add(InlineKeyboardButton.builder().text("返回").callbackData("selectConvoysInfo:"+convoysId).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    @Override
    @Transactional
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String id = data.substring(data.lastIndexOf(":") + 1);
        Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, Long.valueOf(id)));
        EditMessageText messageText = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(convoys.getName() + "请选择要修改的车队成员").replyMarkup(createInlineKeyboardButton(convoys.getConvoysId())).build();
        bot.execute(messageText);






    }

}
