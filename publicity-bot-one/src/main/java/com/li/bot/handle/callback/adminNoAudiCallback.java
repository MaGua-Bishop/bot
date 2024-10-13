package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.entity.database.User;
import com.li.bot.enums.ConvoysInviteStatus;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
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

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class adminNoAudiCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminNoAudi";
    }
    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;

    @Autowired
    private InviteMapper inviteMapper ;

    @Autowired
    private ConvoysMapper convoysMapper ;

    private Long currentConvoysCapacity = 0L;


    private void getConvoysCapacity(Long convoysId){
        Long countById = convoysInviteMapper.getCountById(convoysId);
        if(countById == null){
            currentConvoysCapacity = 0L;
        }else {
            currentConvoysCapacity = countById ;
        }
    }

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
        convoysInvite.setIsReview(false);
        convoysInvite.setReviewTgId(callbackQuery.getFrom().getId());
        convoysInvite.setStatus(ConvoysInviteStatus.DISABLED.getCode());
        convoysInviteMapper.updateById(convoysInvite);


        Long inviteId = convoysInvite.getInviteId();

        Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getInviteId, inviteId));
        if(invite != null){
            Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, convoysInvite.getConvoysId()));
            getConvoysCapacity(convoys.getConvoysId());
            Integer status = convoysInvite.getStatus();
            String msg = "";
            String code ="";
            if(status.equals(ConvoysInviteStatus.IDLE.getCode())){
                code = "\uD83D\uDFE2";
                msg = "空闲";
            }else if(status.equals(ConvoysInviteStatus.REVIEW.getCode())){
                code = "\uD83D\uDFE1";
                msg = "待审核";
            }else if(status.equals(ConvoysInviteStatus.BOARDED.getCode())){
                code = "\uD83D\uDFE2";
                msg = "审核成功";
            }else if(status.equals(ConvoysInviteStatus.DISABLED.getCode())){
                code = "\uD83D\uDD34";
                msg = "审核成功(拒绝)";
            }
            String x =
//                    "📣系统通知📣\n"
                     "申请车队名: " + convoys.getName() + "\n"
                    + "车队类型: 频道\n"
                    + "车队介绍: " + convoys.getCopywriter() + "\n"
                    + "当前/最大(成员): " + currentConvoysCapacity + "/" + convoys.getCapacity() + "\n"
                    + "最低订阅: " + UnitConversionUtils.tensOfThousands(convoys.getSubscription()) + "\n"+
                    "最低阅读: " + convoys.getRead() + "\n\n"+
                    "申请频道id:" + invite.getChatId() + "\n" +
                    "申请频道: <a href=\""+invite.getLink()+"\">"+"" + invite.getName() + "</a>\n" +
                    "订阅人数: " + invite.getMemberCount() + "\n" +
                    "申请人ID: " + invite.getTgId() + "\n" +
                    "申请人名: " + "<a href=\"tg://user?id="+invite.getTgId()+"\">@"+invite.getUserName()+"</a>"+"\n"+
                    "申请状态:"+ code+msg;
            SendMessage sendMessage = SendMessage.builder().chatId(invite.getTgId()).text(x).parseMode("html").disableWebPagePreview(true).build();
            bot.execute(sendMessage);

            EditMessageText editMessageText = EditMessageText.builder().messageId(callbackQuery.getMessage().getMessageId()).chatId(callbackQuery.getMessage().getChatId().toString()).text(x).replyMarkup(createButton("未通过")).parseMode("html").build();
            bot.execute(editMessageText);

        }


    }
}
