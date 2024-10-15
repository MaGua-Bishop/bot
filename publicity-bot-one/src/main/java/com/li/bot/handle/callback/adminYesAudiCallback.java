package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.*;
import com.li.bot.enums.ConvoysInviteStatus;
import com.li.bot.mapper.*;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.utils.BotMessageUtils;
import com.li.bot.utils.UnitConversionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
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
public class adminYesAudiCallback implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminYesAudi";
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper;

    @Autowired
    private InviteMapper inviteMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private ButtonMapper buttonMapper ;

    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private BotConfig botConfig ;

    private Long currentConvoysCapacity = 0L;


    private void getConvoysCapacity(Long convoysId){
        Long countById = convoysInviteMapper.getCountById(convoysId);
        if(countById == null){
            currentConvoysCapacity = 0L;
        }else {
            currentConvoysCapacity = countById ;
        }
    }

    private InlineKeyboardMarkup createButton(String name) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).callbackData("无").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        List<Button> buttons = buttonMapper.selectList(null);

        buttons.forEach(button -> {
            String name = button.getName();
            String url = button.getUrl();
            buttonList.add(InlineKeyboardButton.builder().text(name).url(url).build());
        });

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {

        String data = callbackQuery.getData();
        String id = data.substring(data.lastIndexOf(":") + 1);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, callbackQuery.getFrom().getId()));
        if (!user.getIsAdmin()) {
            return;
        }

        ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getId, Long.valueOf(id)).eq(ConvoysInvite::getIsReview, false));
        if (convoysInvite == null) {
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("该申请不存在或已处理了").build());
            return;
        }
        convoysInvite.setIsReview(true);
        convoysInvite.setReviewTgId(callbackQuery.getFrom().getId());
        convoysInvite.setStatus(ConvoysInviteStatus.BOARDED.getCode());
        convoysInviteMapper.updateById(convoysInvite);

        Long inviteId = convoysInvite.getInviteId();

        Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getInviteId, inviteId));
        invite.setIsReview(true);
        inviteMapper.updateById(invite);
        if (invite != null) {
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
                msg = "被禁用";
            }
            String x = "申请车队名: " + convoys.getName() + "\n"
                    + "车队类型: 频道\n"
                    + "车队介绍: " + convoys.getCopywriter() + "\n"
                    + "当前/最大(成员): " + currentConvoysCapacity + "/" + convoys.getCapacity() + "\n"
                    + "最低订阅: " + UnitConversionUtils.tensOfThousands(convoys.getSubscription())  + "\n"+
                    "最低阅读: " + convoys.getRead() + "\n\n"+
                    "申请频道id:" + invite.getChatId() + "\n" +
                    "申请频道: <a href=\""+invite.getLink()+"\">"+"" + invite.getName() + "</a>\n" +
                    "订阅人数: " + invite.getMemberCount() + "\n" +
                    "申请人ID: " + invite.getTgId() + "\n" +
                    "申请人名: " + "<a href=\"tg://user?id="+invite.getTgId()+"\">@"+invite.getUserName()+"</a>"+"\n"+
                    "申请状态:"+ code+msg;
            SendMessage sendMessage = SendMessage.builder().chatId(invite.getTgId()).text(x).parseMode("html").build();
            bot.execute(sendMessage);

            EditMessageText editMessageText = EditMessageText.builder().messageId(callbackQuery.getMessage().getMessageId()).chatId(callbackQuery.getMessage().getChatId().toString()).text(x).replyMarkup(createButton("已同意")).parseMode("html").build();
            bot.execute(editMessageText);

            //加入后车队频道互发消息 找出当前车队已上车成员

            List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, convoysInvite.getConvoysId()).eq(ConvoysInvite::getStatus, ConvoysInviteStatus.BOARDED.getCode()));
            List<Invite> inviteList = inviteMapper.getInviteListByIds(convoysInviteList.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList()));

            inviteList.forEach(in -> {
                StringBuilder builder = new StringBuilder();
                builder.append("<a href=\"https://"+botConfig.getBotname()+"\">" +"\uD83D\uDE80来自"+convoys.getName()+"\uD83D\uDE80\n</a>" );
                builder.append("<b>"+fileService.getText() + "</b>\n" );
                builder.append(BotMessageUtils.getConvoysMemberInfoList(inviteList));
                builder.append("\n<b>"+fileService.getButtonText()+"</b>");
                SendMessage send = SendMessage.builder().chatId(in.getChatId()).text(String.valueOf(builder)).parseMode("html").replyMarkup(createInlineKeyboardButton()).disableWebPagePreview(true).build();
                Message execute = null;
                Long cId = convoysInvite.getConvoysId() ;
                try {
                    execute = bot.execute(send);
                    ConvoysInvite convoysInvite1 = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, cId).eq(ConvoysInvite::getInviteId, in.getInviteId()));
                    Integer messageId = convoysInvite1.getMessageId();
                    if (messageId == null) {
                        convoysInviteMapper.updateMessageIdById(execute.getMessageId(), in.getInviteId(),convoysInvite1.getConvoysId());
                    } else {
                        try {
                            bot.execute(DeleteMessage.builder()
                                    .chatId(in.getChatId())
                                    .messageId(convoysInvite1.getMessageId())
                                    .build());
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                        convoysInviteMapper.updateMessageIdById(execute.getMessageId(), in.getInviteId(),convoysInvite1.getConvoysId());
                    }
                } catch (Exception e) {
                    // 更新messageId为空
                    convoysInviteMapper.updateMessageIdById(execute.getMessageId(), in.getInviteId(),cId);
                }
            });


        }


    }

}
