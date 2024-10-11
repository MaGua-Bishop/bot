package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.*;
import com.li.bot.mapper.*;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.service.impl.FleetService;
import com.li.bot.utils.BotMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.File;
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
@Slf4j
public class StartMessageMessage implements IMessage{

    @Override
    public String getMessageName() {
        return "开始推送";
    }

    @Autowired
    private UserMapper userMapper;


    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;

    @Autowired
    private InviteMapper inviteMapper ;

    @Autowired
    private ButtonMapper buttonMapper ;

    @Autowired
    private FileService fileService ;

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

    private void groupSendMessage(BotServiceImpl bot, Message message) throws TelegramApiException {
        //有哪些车队
        List<Convoys> convoysList = convoysMapper.selectList(null);
        log.info("当前有{}个车队",convoysList.size());
        for (Convoys convoys : convoysList) {
            Long count = convoysInviteMapper.getCountByConvoysId(convoys.getConvoysId());
            if(convoys.getCapacity().equals(count)){
                SendMessage send = SendMessage.builder().chatId(message.getChatId()).text(""+convoys.getName()+"车队有"+count+"个成员,满员！！！推送").build();
                bot.execute(send);
                log.info("{}车队有{}个成员,满员！！！",convoys.getName(),count);
                List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, convoys.getConvoysId()));
                List<Invite> inviteList = inviteMapper.getInviteListByIds(convoysInviteList.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList()));

                inviteList.forEach(invite -> {
                    String text = fileService.getText()+"\n\n";
                    text += BotMessageUtils.getConvoysMemberInfoList(inviteList);
                    SendMessage sendMessage = SendMessage.builder().chatId(invite.getChatId()).text(text).parseMode("html").replyMarkup(createInlineKeyboardButton()).build();
                    Message execute = null;
                    try {
                        execute = bot.execute(sendMessage);

                        Integer messageId = invite.getMessageId();
                        if(messageId == null){
                            invite.setMessageId(execute.getMessageId());
                            inviteMapper.updateById(invite);
                        }else {
                            try {
                                bot.execute(DeleteMessage.builder()
                                        .chatId(invite.getChatId())
                                        .messageId(invite.getMessageId())
                                        .build());
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                            invite.setMessageId(execute.getMessageId());
                            inviteMapper.updateById(invite);
                        }
                    } catch (Exception e) {
                        // 更新messageId为空
                        inviteMapper.updateMessageIdById(execute.getMessageId(), invite.getInviteId());
                    }
                });
            }else {
                log.info("{}车队有{}个成员,未满员 不推送",convoys.getName(),count);
            }
        }
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        if(message.getChat().getType().equals("private")){
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, message.getFrom().getId()));
            if(user.getIsAdmin()){
                groupSendMessage(bot,message);
                SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text("推送完成").build();
                bot.execute(sendMessage);
            }
        }
    }
}
