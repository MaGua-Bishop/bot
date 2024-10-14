package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class AdminDeleteConvoysMembersCallback implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminDeleteConvoysMembers";
    }

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;

    @Autowired
    private InviteMapper inviteMapper ;

    private InlineKeyboardMarkup createInlineKeyboardButton(Long convoysId){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        buttonList.add(InlineKeyboardButton.builder().text("返回").callbackData("deleteConvoysMembers:" + convoysId).build());


        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }
    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();

        Pattern pattern = Pattern.compile("adminDeleteConvoysMembers:(\\d+)convoysId:(\\d+)");

        // 创建Matcher对象
        Matcher matcher = pattern.matcher(data);

        if (matcher.find()) {
            // 提取匹配的ID
            String inviteId = matcher.group(1);
            String convoysId = matcher.group(2);
            List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, Long.valueOf(convoysId)).eq(ConvoysInvite::getInviteId, Long.valueOf(inviteId)));
            for (ConvoysInvite convoysInvite : convoysInviteList) {
                convoysInviteMapper.deleteById(convoysInvite);
            }
            Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getInviteId, Long.valueOf(inviteId)));
            SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("《" + invite.getName() + "》\n机器人在频道状态异常或权限不足无法发送消息已退出").disableWebPagePreview(true).build();
            bot.execute(sendMessage);
            inviteMapper.deleteById(Long.valueOf(inviteId));

            EditMessageText messageText = EditMessageText.builder()
                    .chatId(callbackQuery.getMessage().getChatId().toString())
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text("《" + invite.getName() + "》删除成功")
                    .replyMarkup(createInlineKeyboardButton(Long.valueOf(convoysId)))
                    .build();
            bot.execute(messageText);
        }
    }

}
