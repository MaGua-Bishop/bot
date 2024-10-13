package com.li.bot.handle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.Button;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.mapper.ButtonMapper;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.utils.BotMessageUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-11
 */
public class ChannelPostHandle {

    private final BotServiceImpl bot;

    private final Message message ;

    private final ConvoysInviteMapper convoysInviteMapper ;

    private final InviteMapper inviteMapper ;

    private final FileService fileService  ;

    private final ButtonMapper buttonMapper ;

    private BotConfig botConfig ;

    private final ConvoysMapper convoysMapper ;

    public ChannelPostHandle(BotServiceImpl bot, Message message, ConvoysInviteMapper convoysInviteMapper, InviteMapper inviteMapper, FileService fileService, ButtonMapper buttonMapper,BotConfig botConfig, ConvoysMapper convoysMapper) {
        this.bot = bot;
        this.message = message;
        this.convoysInviteMapper = convoysInviteMapper;
        this.inviteMapper = inviteMapper;
        this.fileService = fileService;
        this.buttonMapper = buttonMapper;
        this.botConfig = botConfig ;
        this.convoysMapper = convoysMapper ;
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


    public void handle() {
        Long chatId = message.getChatId();

        Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, chatId));
        if(invite != null){

            ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, invite.getInviteId()));
            Long convoysId = convoysInvite.getConvoysId();

            List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, convoysId).eq(ConvoysInvite::getIsReview, true));
            List<Invite> inviteList = inviteMapper.getInviteListByIds(convoysInviteList.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList()));
            Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, convoysInvite.getConvoysId()));
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
