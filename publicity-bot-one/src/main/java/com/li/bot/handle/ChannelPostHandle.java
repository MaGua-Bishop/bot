package com.li.bot.handle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.Button;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.enums.ConvoysInviteStatus;
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
import java.util.Map;
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

    private final ConvoysMapper convoysMapper ;

    private final BotConfig botConfig ;

    public ChannelPostHandle(BotServiceImpl bot, Message message, ConvoysInviteMapper convoysInviteMapper, InviteMapper inviteMapper, FileService fileService, ButtonMapper buttonMapper, ConvoysMapper convoysMapper, BotConfig botConfig) {
        this.bot = bot;
        this.message = message;
        this.convoysInviteMapper = convoysInviteMapper;
        this.inviteMapper = inviteMapper;
        this.fileService = fileService;
        this.buttonMapper = buttonMapper;
        this.convoysMapper = convoysMapper;
        this.botConfig = botConfig;
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

        //找出当前频道的邀请记录
        Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, chatId));
        if(invite != null){
            //找出当前频道所在的所有车队
            List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, invite.getInviteId()).eq(ConvoysInvite::getStatus, ConvoysInviteStatus.BOARDED.getCode()));
            Map<Long, List<ConvoysInvite>> collect = convoysInviteList.stream().collect(Collectors.groupingBy(ConvoysInvite::getConvoysId));

            collect.forEach((convoysId, invites) -> {
                List<Invite> inviteList = inviteMapper.getInviteListByIds(convoysInviteList.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList()));
                Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, convoysId));
                inviteList.forEach(in -> {
                    StringBuilder builder = new StringBuilder();
                    builder.append("<a href=\"https://"+botConfig.getBotname()+"\">" +"\uD83D\uDE80来自"+convoys.getName()+"\uD83D\uDE80\n</a>" );
                    builder.append(fileService.getText() + "\n" );
                    builder.append(BotMessageUtils.getConvoysMemberInfoList(inviteList));
                    builder.append("\n"+fileService.getButtonText());
                    SendMessage send = SendMessage.builder().chatId(in.getChatId()).text(String.valueOf(builder)).parseMode("html").replyMarkup(createInlineKeyboardButton()).disableWebPagePreview(true).build();
                    Message execute = null;
                    try {
                        execute = bot.execute(send);
                        ConvoysInvite c = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, in.getInviteId()).eq(ConvoysInvite::getConvoysId, convoysId));
                        Integer messageId = c.getMessageId();
                        if (messageId == null) {
                            convoysInviteMapper.updateMessageIdById(execute.getMessageId(), in.getInviteId(),convoysId);
                        } else {
                            try {
                                bot.execute(DeleteMessage.builder()
                                        .chatId(in.getChatId())
                                        .messageId(c.getMessageId())
                                        .build());
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                            convoysInviteMapper.updateMessageIdById(execute.getMessageId(), in.getInviteId(),convoysId);
                        }
                    } catch (Exception e) {
                        // 更新messageId为空
                        convoysInviteMapper.updateMessageIdById(execute.getMessageId(), in.getInviteId(),convoysId);
                    }
                });
            });
        }
    }
}
