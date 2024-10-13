package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Button;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.enums.ConvoysInviteStatus;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
public class deleteConvoysMembersCallback implements ICallback {

    @Override
    public String getCallbackName() {
        return "deleteConvoysMembers";
    }



    @Autowired
    private InviteMapper inviteMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;

    private InlineKeyboardMarkup createInlineKeyboardButton(List<Invite> list, Long convoysId){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        list.forEach(button -> {
            buttonList.add(InlineKeyboardButton.builder().text(button.getName()).callbackData("adminDeleteConvoysMembers:" + button.getInviteId()+"convoysId:"+convoysId).build());
        });

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }
    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String id = data.substring(data.lastIndexOf(":") + 1);

        List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, Long.valueOf(id)).eq(ConvoysInvite::getStatus, ConvoysInviteStatus.BOARDED.getCode()));

        List<Invite> inviteListByIds = inviteMapper.getInviteListByIds(convoysInviteList.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList()));

        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请选择要删除的成员\n点击按钮直接删除").parseMode("html").replyMarkup(createInlineKeyboardButton(inviteListByIds,Long.valueOf(id))).build();
        bot.execute(sendMessage);


    }

}
