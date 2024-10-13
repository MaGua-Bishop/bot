package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.*;
import com.li.bot.mapper.*;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.utils.BotMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
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
    private BotConfig botConfig ;

    @Autowired
    private ConvoysMapper convoysMapper;

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
    @Transactional
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
        convoysInviteMapper.updateById(convoysInvite);

        String name = "已同意";
        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton(name)).build();
        bot.execute(editMessageReplyMarkup);

        Long inviteId = convoysInvite.getInviteId();

        Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getInviteId, inviteId));
        invite.setIsReview(true);
        inviteMapper.updateById(invite);
        if (invite != null) {
            String text = "频道名: <a href=\"" + invite.getLink() + "\">" + invite.getName() + "</a>\n"
                    + "申请人: <a href=\"tg://user?id=" + invite.getTgId() + "\">" + invite.getUserName() + "</a>\n" +
                    "申请通过";
            SendMessage sendMessage = SendMessage.builder().chatId(invite.getTgId()).text(text).parseMode("html").build();
            bot.execute(sendMessage);


            //车队加入后 频道互发消息
            Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, convoysInvite.getConvoysId()));

            List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, convoysInvite.getConvoysId()).eq(ConvoysInvite::getIsReview,true));
            List<Invite> inviteList = inviteMapper.getInviteListByIds(convoysInviteList.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList()));

            inviteList.forEach(in -> {
                StringBuilder builder = new StringBuilder();
                builder.append("<a href=\"https://"+botConfig.getBotname()+"\">" +"\uD83D\uDE80"+convoys.getName()+convoys.getCopywriter()+"\uD83D\uDE80\n</a>" );
                builder.append(fileService.getText() + "\n" );
                builder.append(BotMessageUtils.getConvoysMemberInfoList(inviteList));
                builder.append("\n"+fileService.getButtonText());
                SendMessage send = SendMessage.builder().chatId(in.getChatId()).text(String.valueOf(builder)).parseMode("html").replyMarkup(createInlineKeyboardButton()).build();
                Message execute = null;
                try {
                    execute = bot.execute(send);

                    Integer messageId = in.getMessageId();
                    if (messageId == null) {
                        in.setMessageId(execute.getMessageId());
                        inviteMapper.updateById(in);
                    } else {
                        try {
                            bot.execute(DeleteMessage.builder()
                                    .chatId(in.getChatId())
                                    .messageId(in.getMessageId())
                                    .build());
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                        in.setMessageId(execute.getMessageId());
                        inviteMapper.updateById(in);
                    }
                } catch (Exception e) {
                    // 更新messageId为空
                    inviteMapper.updateMessageIdById(execute.getMessageId(), in.getInviteId());
                }
            });


        }


    }

}
