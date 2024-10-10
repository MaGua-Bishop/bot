package com.li.bot.handle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.li.bot.entity.database.Invite;
import com.li.bot.enums.InviteType;
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-29
 * @Description: 处理文本消息
 */
public class ChatMemberUpdatedHandle {


    private BotServiceImpl bot ;

    private ChatMemberUpdated myChatMember ;

    private InviteMapper inviteMapper ;


    public ChatMemberUpdatedHandle(BotServiceImpl bot, ChatMemberUpdated myChatMember, InviteMapper inviteMapper) {
        this.bot = bot;
        this.myChatMember = myChatMember;
        this.inviteMapper = inviteMapper;
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

    public void handle() throws TelegramApiException {
        // 获取群组信息
        Chat chat = myChatMember.getChat();
        String title = chat.getTitle();
        Long id = chat.getId();
        String type = chat.getType();

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
                sendMessage.setText(title+"频道添加机器人成功\n\n权限检测正常");
                bot.execute(sendMessage);
            }else{
                invite.setIsPermissions(false);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(myChatMember.getFrom().getId());
                sendMessage.setText(title+"频道添加机器人成功\n\n权限检测不正常\n\n管理权限：发布消息/编辑其他人的消息/删除其他人的消息/邀请其他人权限，缺失权限机器人不能正常工作");
                bot.execute(sendMessage);
            }
            addInviteOrUpdate(invite);
        } else if (myChatMember.getNewChatMember() instanceof ChatMemberLeft) {
            // 处理机器人离开群聊的情况
            Invite selectOne = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, myChatMember.getChat().getId()));

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(selectOne.getTgId());
            sendMessage.setText("机器人离开了"+title+"频道");
            bot.execute(sendMessage);
            inviteMapper.deleteById(selectOne);
        }
    }
}

