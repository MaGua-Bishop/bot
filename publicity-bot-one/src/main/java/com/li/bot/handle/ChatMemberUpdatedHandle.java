package com.li.bot.handle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.li.bot.entity.ConvoysExitMembers;
import com.li.bot.entity.database.*;
import com.li.bot.enums.InviteType;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import com.li.bot.utils.ConvoysPageUtils;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ExportChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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


    private UserMapper userMapper ;


    public ChatMemberUpdatedHandle(BotServiceImpl bot, ChatMemberUpdated myChatMember, InviteMapper inviteMapper, ConvoysMapper convoysMapper, ConvoysInviteMapper convoysInviteMapper,UserMapper userMapper) {
        this.bot = bot;
        this.myChatMember = myChatMember;
        this.inviteMapper = inviteMapper;
        this.convoysMapper = convoysMapper;
        this.convoysInviteMapper = convoysInviteMapper;
        this.userMapper = userMapper;
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

        if(type.equals("supergroup")||type.equals("group")){

            String newStatus = myChatMember.getNewChatMember().getStatus();

            if ("kicked".equals(newStatus) || "left".equals(newStatus)) {
                System.out.println("机器人离开群聊");
                // 处理机器人离开群聊的情况
                Invite selectOne = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, myChatMember.getChat().getId()));
                ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, selectOne.getInviteId()));
                if(convoysInvite != null){
                    convoysInviteMapper.deleteById(convoysInvite);
                }

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


            Page<Convoys> page = new Page<>(1, ConvoysPageUtils.PAGESIZE);
            IPage<ConvoysInfoListVo> convoysList = convoysMapper.selectConvoysList(page);
            if(convoysList.getRecords().isEmpty()){
                bot.execute(SendMessage.builder().chatId(myChatMember.getFrom().getId()).text("暂无互推").build());
                return;
            }
            Long number = convoysList.getRecords().stream().map(ConvoysInfoListVo::getCurrentCapacity).reduce(Long::sum).get();
            SendMessage send = SendMessage.builder().chatId(myChatMember.getFrom().getId()).text(BotMessageUtils.getConvoysHall(convoysList.getRecords().size(),number)).replyMarkup(ConvoysPageUtils.createInlineKeyboardButton(convoysList))
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


            Integer memberCount  = bot.execute(count);

            Invite invite = new Invite();
            invite.setTgId(myChatMember.getFrom().getId());
            invite.setChatId(id);
            invite.setType(InviteType.getCodeByMessage(type));
            invite.setMemberCount(Long.valueOf(memberCount));
            invite.setName(title);
            String link = "";
            try {
                link = bot.execute(exportChatInviteLink);
            }catch (TelegramApiException e){
                SendMessage send = SendMessage.builder().chatId(invite.getTgId()).text("《"+invite.getName()+"》\n检测到机器人发不了消息,机器人已自动退出").parseMode("html").build();
                bot.execute(send);
                Invite invite1 = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, id));
                ConvoysExitMembers convoysExitMembersByInviteId = inviteMapper.getConvoysExitMembersByInviteId(invite1.getInviteId());
                inviteMapper.delete(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, id));
                convoysInviteMapper.delete(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, invite1.getInviteId()));
                LeaveChat leaveChat = new LeaveChat(String.valueOf(id));
                bot.execute(leaveChat);
                List<User> adminList = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getIsAdmin, true));
                if(!adminList.isEmpty()){
                    adminList.forEach(user -> {
                        try {
                            bot.execute(SendMessage.builder().chatId(user.getTgId()).text("<b>车队:"+convoysExitMembersByInviteId.getConvoyName()+"</b>\n<b>频道:《"+convoysExitMembersByInviteId.getInviteName()+"》</b>\n检测到机器人发不了消息,机器人已自动退出车队").parseMode("html").build());
                        } catch (TelegramApiException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }
                return;
            }
            invite.setLink(link);
            invite.setUserName(myChatMember.getFrom().getUserName());
            if(permissions){
                invite.setIsPermissions(true);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(myChatMember.getFrom().getId());
                sendMessage.setText("<b>《"+title+"》</b>"+"添加机器人成功\n\n权限检测正常");
                sendMessage.setParseMode("html");
                bot.execute(sendMessage);
                addInviteOrUpdate(invite);
            }else{
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(myChatMember.getFrom().getId());
                sendMessage.setText("<b>《"+title+"》</b>"+"机器人失败\n\n权限检测不正常\n\n管理权限：发布消息/编辑其他人的消息/删除其他人的消息/邀请其他人权限，缺失权限机器人不能正常工作");
                sendMessage.setParseMode("html");
                bot.execute(sendMessage);
                SendMessage send = SendMessage.builder().chatId(invite.getTgId()).text("《"+invite.getName()+"》\n检测到机器人发不了消息,机器人已自动退出").parseMode("html").build();
                bot.execute(send);
                Invite invite1 = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, id));
                ConvoysExitMembers convoysExitMembersByInviteId = inviteMapper.getConvoysExitMembersByInviteId(invite1.getInviteId());
                inviteMapper.deleteById(invite1);
                convoysInviteMapper.delete(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, invite1.getInviteId()));
                LeaveChat leaveChat = new LeaveChat(String.valueOf(id));
                bot.execute(leaveChat);
                List<User> adminList = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getIsAdmin, true));
                if(!adminList.isEmpty()){
                    adminList.forEach(user -> {
                        try {
                            bot.execute(SendMessage.builder().chatId(user.getTgId()).text("<b>车队:"+convoysExitMembersByInviteId.getConvoyName()+"</b>\n<b>频道:《"+convoysExitMembersByInviteId.getInviteName()+"》</b>\n检测到机器人发不了消息,机器人已自动退出车队").parseMode("html").build());
                        } catch (TelegramApiException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }}
            if(permissions){
                Page<Convoys> page = new Page<>(1, ConvoysPageUtils.PAGESIZE);
                IPage<ConvoysInfoListVo> convoysList = convoysMapper.selectConvoysList(page);
                if(convoysList.getRecords().isEmpty()){
                    bot.execute(SendMessage.builder().chatId(myChatMember.getFrom().getId()).text("暂无互推").build());
                    return;
                }
                Long number = convoysList.getRecords().stream().map(ConvoysInfoListVo::getCurrentCapacity).reduce(Long::sum).get();
                SendMessage send = SendMessage.builder().chatId(myChatMember.getFrom().getId()).text(BotMessageUtils.getConvoysHall(convoysList.getRecords().size(),number)).replyMarkup(ConvoysPageUtils.createInlineKeyboardButton(convoysList))
                        .parseMode("html").build();
                bot.execute(send);
            }


        } else if (myChatMember.getNewChatMember() instanceof ChatMemberLeft) {
            // 处理机器人离开群聊的情况
            Invite selectOne = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, myChatMember.getChat().getId()));
            ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, selectOne.getInviteId()));
            if(convoysInvite != null){
                convoysInviteMapper.deleteById(convoysInvite);
            }
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(selectOne.getTgId());
            sendMessage.setText("机器人离开了"+"<b>《"+title+"》</b>");
            sendMessage.setParseMode("html");
            bot.execute(sendMessage);
            inviteMapper.deleteById(selectOne);
        }else if(myChatMember.getOldChatMember() instanceof ChatMemberAdministrator){
            Invite selectOne = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, myChatMember.getChat().getId()));
            ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, selectOne.getInviteId()));
            if(convoysInvite != null){
                convoysInviteMapper.deleteById(convoysInvite);
            }
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(selectOne.getTgId());
            sendMessage.setText("机器人离开了"+"<b>《"+title+"》</b>");
            sendMessage.setParseMode("html");
            bot.execute(sendMessage);
            inviteMapper.deleteById(selectOne);
        }else if(myChatMember.getNewChatMember() instanceof ChatMemberBanned){
            ChatMember newChatMember = myChatMember.getNewChatMember();
            ChatMemberBanned banned = (ChatMemberBanned) newChatMember;
            if(banned.getUntilDate() != null && banned.getUntilDate() > 0){
                Invite selectOne = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getChatId, myChatMember.getChat().getId()));
                ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, selectOne.getInviteId()));
                if(convoysInvite != null){
                    convoysInviteMapper.deleteById(convoysInvite);
                }
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(selectOne.getTgId());
                sendMessage.setText("机器人离开了"+"<b>《"+title+"》</b>");
                sendMessage.setParseMode("html");
                bot.execute(sendMessage);
                inviteMapper.deleteById(selectOne);
            }
        }
    }
}

