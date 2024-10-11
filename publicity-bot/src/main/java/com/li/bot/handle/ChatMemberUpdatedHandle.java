package com.li.bot.handle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.enums.InviteType;
import com.li.bot.handle.callback.SelectConvoysListCallback;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.BeanUtils;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ExportChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-29
 * @Description: 处理文本消息
 */
public class ChatMemberUpdatedHandle {


    private BotServiceImpl bot ;

    private ChatMemberUpdated myChatMember ;

    private InviteMapper inviteMapper ;
    private ConvoysMapper convoysMapper ;
    private ConvoysInviteMapper convoysInviteMapper ;

    public ChatMemberUpdatedHandle(BotServiceImpl bot, ChatMemberUpdated myChatMember, InviteMapper inviteMapper, ConvoysMapper convoysMapper, ConvoysInviteMapper convoysInviteMapper) {
        this.bot = bot;
        this.myChatMember = myChatMember;
        this.inviteMapper = inviteMapper;
        this.convoysMapper = convoysMapper;
        this.convoysInviteMapper = convoysInviteMapper;
    }

    private boolean permissions(ChatMemberAdministrator admin){
        Boolean canPostMessages = admin.getCanPostMessages();
        Boolean canEditMessages = admin.getCanEditMessages();
        Boolean canDeleteMessages = admin.getCanDeleteMessages();
        Boolean canInviteUsers = admin.getCanInviteUsers();
        if(canPostMessages && canEditMessages && canDeleteMessages && canInviteUsers){
            return true;
        }else {
            return false;
        }
    }

    private void addInviteOrUpdate(Invite invite){
        Invite selectOne = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getTgId, invite.getTgId()).eq(Invite::getChatId, invite.getChatId()));
        if(selectOne == null){
            inviteMapper.insert(invite);
        }else {
            selectOne.setIsPermissions(invite.getIsPermissions());
            inviteMapper.updateById(selectOne);
        }
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(List<Convoys> convoys){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        if(convoys.isEmpty()){
            buttonList.add(InlineKeyboardButton.builder().text("暂无车队").callbackData("null").build());
        }else {
            for (Convoys convoy : convoys) {
                List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, convoy.getConvoysId()).eq(ConvoysInvite::getIsReview,true));
                String code = "";
                if(convoy.getCapacity()==convoysInviteList.size()){
                    code = "\uD83D\uDD34";
                }else {
                    code = "\uD83D\uDFE2";
                }
                buttonList.add(InlineKeyboardButton.builder().text(convoy.getCopywriter()+"("+convoysInviteList.size()+")"+code).callbackData("selectConvoysInfo:"+convoy.getConvoysId()).build());
            }
        }
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    public void handle() throws TelegramApiException {
        // 获取群组信息
        Chat chat = myChatMember.getChat();
        String title = chat.getTitle();
        Long id = chat.getId();
        String type = chat.getType();

        if(type.equals("supergroup")||type.equals("group")){

            String newStatus = myChatMember.getNewChatMember().getStatus();

            if ("kicked".equals(newStatus) || "left".equals(newStatus)) {
                // 处理机器人离开群聊的情况
                Invite selectOne = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, myChatMember.getChat().getId()));

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(selectOne.getTgId());
                sendMessage.setText("机器人离开了"+"<b>《"+title+"》</b>");
                sendMessage.setParseMode("html");
                bot.execute(sendMessage);
                inviteMapper.deleteById(selectOne);

            }

            // 获取群成员数
            GetChatMemberCount count = new GetChatMemberCount(String.valueOf(id));
            //获取群邀请连接
            ExportChatInviteLink exportChatInviteLink = new ExportChatInviteLink(String.valueOf(id));
            String link = bot.execute(exportChatInviteLink);
            Integer memberCount  = bot.execute(count);

            Invite invite = new Invite();
            invite.setTgId(myChatMember.getFrom().getId());
            invite.setChatId(id);
            invite.setType(InviteType.getCodeByMessage(type));
            invite.setMemberCount(Long.valueOf(memberCount));
            invite.setName(title);
            invite.setLink(link);
            invite.setUserName(myChatMember.getFrom().getUserName());

            invite.setIsPermissions(true);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(myChatMember.getFrom().getId());
            sendMessage.setText("<b>《"+title+"》</b>"+"添加机器人成功\n\n请检查权限是否正常,否则请重新添加\n\n管理权限：发布消息/编辑其他人的消息/删除其他人的消息/邀请其他人权限，缺失权限机器人不能正常工作");
            sendMessage.setParseMode("html");
            bot.execute(sendMessage);

            addInviteOrUpdate(invite);


            List<Convoys> convoys = convoysMapper.selectList(null);
            String text = "" ;
            for (Convoys convoy : convoys) {
                text += "\uD83C\uDFCE\uFE0F" ;
            }
            SendMessage send = SendMessage.builder().chatId(myChatMember.getFrom().getId()).text("请选择需要申请的车队\n\n车队数量:"+convoys.size()+"\n"+text+"\n\n快来加入吧!!!").replyMarkup(createInlineKeyboardButton(convoys))
                    .parseMode("html").build();
            bot.execute(send);

            return;
        }

        // 获取管理员信息
        if (myChatMember.getNewChatMember() instanceof ChatMemberAdministrator) {
            ChatMemberAdministrator admin = (ChatMemberAdministrator) myChatMember.getNewChatMember();
            //权限判断
            boolean permissions = permissions(admin);
            // 获取群成员数
            GetChatMemberCount count = new GetChatMemberCount(String.valueOf(id));
            //获取群邀请连接
            ExportChatInviteLink exportChatInviteLink = new ExportChatInviteLink(String.valueOf(id));
            String link = bot.execute(exportChatInviteLink);
            Integer memberCount  = bot.execute(count);

            Invite invite = new Invite();
            invite.setTgId(myChatMember.getFrom().getId());
            invite.setChatId(id);
            invite.setType(InviteType.getCodeByMessage(type));
            invite.setMemberCount(Long.valueOf(memberCount));
            invite.setName(title);
            invite.setLink(link);
            invite.setUserName(myChatMember.getFrom().getUserName());

            if(permissions){
                invite.setIsPermissions(true);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(myChatMember.getFrom().getId());
                sendMessage.setText("<b>《"+title+"》</b>"+"添加机器人成功\n\n权限检测正常");
                sendMessage.setParseMode("html");
                bot.execute(sendMessage);
            }else{
                invite.setIsPermissions(false);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(myChatMember.getFrom().getId());
                sendMessage.setText("<b>《"+title+"》</b>"+"添加机器人成功\n\n权限检测不正常\n\n管理权限：发布消息/编辑其他人的消息/删除其他人的消息/邀请其他人权限，缺失权限机器人不能正常工作");
                sendMessage.setParseMode("html");
                bot.execute(sendMessage);
            }
            addInviteOrUpdate(invite);

            List<Convoys> convoys = convoysMapper.selectList(null);
            String text = "" ;
            for (Convoys convoy : convoys) {
                text += "\uD83C\uDFCE\uFE0F" ;
            }
            SendMessage sendMessage = SendMessage.builder().chatId(myChatMember.getFrom().getId()).text("请选择需要申请的车队\n\n车队数量:"+convoys.size()+"\n"+text+"\n\n快来加入吧!!!").replyMarkup(createInlineKeyboardButton(convoys))
                    .parseMode("html").build();
            bot.execute(sendMessage);



        } else if (myChatMember.getNewChatMember() instanceof ChatMemberLeft) {
            // 处理机器人离开群聊的情况
            Invite selectOne = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, myChatMember.getChat().getId()));

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(selectOne.getTgId());
            sendMessage.setText("机器人离开了"+"<b>《"+title+"》</b>");
            sendMessage.setParseMode("html");
            bot.execute(sendMessage);
            inviteMapper.deleteById(selectOne);
        }
    }
}

