package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class channelRequestCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "channelRequest";
    }

    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private InviteMapper inviteMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;

    @Autowired
    private FileService fileService ;



    private InlineKeyboardMarkup createInlineKeyboardButton(Long tgId,Long capacity){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        //根据tgId和订阅数是否等于或大于capacity，判断是否可以订阅
        List<Invite> inviteList = inviteMapper.getInviteListByChatIdAndMemberCount(tgId,capacity);
        if(inviteList.isEmpty()){
            buttonList.add(InlineKeyboardButton.builder().text("您暂无符合的频道").callbackData("null").build());
        }else {
            for (Invite invite : inviteList) {
                buttonList.add(InlineKeyboardButton.builder().text(invite.getName()).callbackData("channelRequest:"+invite.getInviteId()).build());
            }
        }
        buttonList.add(InlineKeyboardButton.builder().text("\uD83D\uDD19返回").callbackData("returnConvoysList").build());

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup createInlineKeyboardButton02(Long id){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        buttonList.add(InlineKeyboardButton.builder().text("同意").callbackData("adminYesAudi:"+id).build());
        buttonList.add(InlineKeyboardButton.builder().text("拒绝").callbackData("adminNoAudi:"+id).build());

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }



    @Override
    @Transactional
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();

        Pattern pattern = Pattern.compile(":(\\d+)");
        Matcher matcher = pattern.matcher(data);

        Long inviteId = -1L, convoysId = -1L; // 初始化为-1或其他默认值
        if (matcher.find()) {
            inviteId = Long.valueOf(matcher.group(1));
        }
        if (matcher.find()) {
            convoysId = Long.valueOf(matcher.group(1));
        }

        bot.execute(DeleteMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .build());

        Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getInviteId, inviteId));
        if(invite == null){
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请检查机器人是否离开了").build());
            return;
        }


        ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, convoysId).eq(ConvoysInvite::getInviteId, inviteId));

        if(convoysInvite != null){
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("您该频道已申请过该车队,请勿重复申请").build());
            return;
        }
        convoysInvite = new ConvoysInvite();
        convoysInvite.setConvoysId(convoysId);
        convoysInvite.setInviteId(inviteId);
        convoysInviteMapper.insert(convoysInvite);

        //发消息提示用户
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(invite.getName() + "已申请,请等待审核").build();
        bot.execute(sendMessage);

        //发送消息给频道管理员同意或拒绝加入
        Map<String,String> adminChannelList = fileService.getAdminChannelList();

        String string = adminChannelList.get("id");


        Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, convoysId));

        String text = "频道名: <a href=\"" + invite.getLink() + "\">" + invite.getName() + "</a>\n"
                + "申请人: <a href=\"tg://user?id=" + invite.getTgId() + "\">" + invite.getUserName() + "</a>\n"
                +"频道人数:"+ invite.getMemberCount() + "\n"
                +"申请车队: "+convoys.getName();

        SendMessage send = SendMessage.builder().chatId(string).text(text).parseMode("html").replyMarkup(createInlineKeyboardButton02(convoysInvite.getId())).build();
        bot.execute(send);

    }
}
