package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class adminYesAudiCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminYesAudi";
    }

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;

    @Autowired
    private InviteMapper inviteMapper ;

    private InlineKeyboardMarkup createButton(String name){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).callbackData("无").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    @Transactional
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {

        String data = callbackQuery.getData();
        String id = data.substring(data.lastIndexOf(":") + 1);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, callbackQuery.getFrom().getId()));
        if(!user.getIsAdmin()){
            return;
        }

        ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getId, Long.valueOf(id)).eq(ConvoysInvite::getIsReview, false));
        if(convoysInvite == null){
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("该申请不存在或已处理了").build());
            return;
        }
        convoysInvite.setIsReview(true);
        convoysInvite.setReviewTgId(callbackQuery.getFrom().getId());
        convoysInviteMapper.updateById(convoysInvite);

        String name = "已同意";
        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton(name)).build();
        bot.execute(editMessageReplyMarkup);

        Long inviteId = convoysInvite.getInviteId();

        Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getInviteId, inviteId));
        invite.setIsReview(true);
        inviteMapper.updateById(invite);
        if(invite != null){
            String text = "频道名: <a href=\"" + invite.getLink() + "\">" + invite.getName() + "</a>\n"
                    + "申请人: <a href=\"tg://user?id=" + invite.getTgId() + "\">" + invite.getUserName() + "</a>\n"+
                    "申请通过";
            SendMessage sendMessage = SendMessage.builder().chatId(invite.getTgId()).text(text).parseMode("html").build();
            bot.execute(sendMessage);
        }


    }

}
