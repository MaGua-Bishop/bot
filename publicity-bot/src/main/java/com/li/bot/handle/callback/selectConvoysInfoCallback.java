package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import com.li.bot.utils.UnitConversionUtils;
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
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class selectConvoysInfoCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "selectConvoysInfo";
    }

    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private InviteMapper inviteMapper ;

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;

    private int currentConvoysCapacity = 0 ;


    private void getConvoysCapacity(Long ConvoysId){
        List<ConvoysInvite> list = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, ConvoysId).eq(ConvoysInvite::getIsReview, true));
       if(list.isEmpty()){
           currentConvoysCapacity = 0;
       }else {
           currentConvoysCapacity = list.size();
       }
    }

    private List<Invite> getConvoysMemberList(Long ConvoysId){
        List<ConvoysInvite> list = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, ConvoysId).eq(ConvoysInvite::getIsReview, true));
        List<Invite> inviteList = new ArrayList<>();
        if(!list.isEmpty()){
            //根据inviteId查出所有的
            List<Long> inviteIds = list.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList());
            inviteList = inviteMapper.getInviteListByIds(inviteIds);
            //把link字段信息提取
        }
       return inviteList;
    }




    private Convoys selectConvoysInfo(Long convoysId){
        Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, convoysId));
        return convoys;
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(Long tgId,Long subscription,Long convoysId,Long capacity){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));
        if(capacity == currentConvoysCapacity ){
            buttonList.add(InlineKeyboardButton.builder().text("车队已满").callbackData("null").build());
        }else if(user.getIsAdmin()){
                List<Invite> invites = inviteMapper.selectList(new LambdaQueryWrapper<Invite>().eq(Invite::getTgId, tgId));
                for (Invite invite : invites) {
                    buttonList.add(InlineKeyboardButton.builder().text(invite.getName()).callbackData("channelRequest:"+invite.getInviteId()+":convoysId:"+convoysId).build());
                }
                buttonList.add(InlineKeyboardButton.builder().text("修改车队推送间隔").callbackData("updateConvoysTime:"+convoysId).build());
        } else {
            //根据tgId和订阅数是否等于或大于capacity，判断是否可以订阅
            List<Invite> inviteList = inviteMapper.getInviteListByChatIdAndMemberCount(tgId,subscription);
            if(inviteList.isEmpty()){
                buttonList.add(InlineKeyboardButton.builder().text("您暂无符合的频道").callbackData("null").build());
            }else {
                for (Invite invite : inviteList) {
                    buttonList.add(InlineKeyboardButton.builder().text(invite.getName()).callbackData("channelRequest:"+invite.getInviteId()+":convoysId:"+convoysId).build());
                }
            }
        }


        buttonList.add(InlineKeyboardButton.builder().text("\uD83D\uDD19返回").callbackData("returnConvoysList").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup createInlineKeyboardButton02(Long tgId,Long convoysId){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        buttonList.add(InlineKeyboardButton.builder().text("➕申请加入").callbackData("applyToJoin:"+convoysId).build());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));
        if(user.getIsAdmin()){
            buttonList.add(InlineKeyboardButton.builder().text("修改车队推送间隔").callbackData("updateConvoysTime:"+convoysId).build());
        }
        buttonList.add(InlineKeyboardButton.builder().text("\uD83D\uDD19返回").callbackData("returnConvoysList").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }





    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String convoysId = data.substring(data.lastIndexOf(":") + 1);
        Long id = Long.valueOf(convoysId);

        //获取当前车队的成员数
        getConvoysCapacity(id);

        Convoys convoys = selectConvoysInfo(id);

        List<Invite> InviteList = getConvoysMemberList(id);

        String message = "车队标题：" + convoys.getName() + "\n" +
                "车队介绍：" + convoys.getCopywriter() + "\n" +
                "当前/最大(成员)："+currentConvoysCapacity+"/" + convoys.getCapacity() + "\n" +
                "车队类型：频道" +"\n" +
                "最低订阅：" + UnitConversionUtils.tensOfThousands(convoys.getSubscription()) +"\n" +
                "最低阅读：" + convoys.getRead()+"\n" +
                "\n\n"+
                "车队成员列表:"+"\n"+
                BotMessageUtils.getConvoysMemberList(InviteList);
        EditMessageText editMessageText = EditMessageText.builder().messageId(callbackQuery.getMessage().getMessageId()).chatId(callbackQuery.getMessage().getChatId().toString()).text(message).replyMarkup(createInlineKeyboardButton02(callbackQuery.getFrom().getId(),id)).parseMode("html").build();
        bot.execute(editMessageText);
    }
}
