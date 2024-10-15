package com.li.bot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @Author: li
 * @CreateTime: 2024-10-15
 */
@Component
public class ChannelMembersServiceImpl {



    //创建一个线程安全的set集合
    private static final Set<Long> channelMembers = new ConcurrentSkipListSet<>();

    public void addChannelMember(Long tgId) {
        channelMembers.add(tgId);
    }

    public boolean isChannelMember(Long tgId) {
        return channelMembers.contains(tgId);
    }

    public void removeChannelMember(Long tgId) {
        channelMembers.remove(tgId);
    }

    public Set<Long> getChannelMembers() {
        return channelMembers;
    }

    public void clearChannelMembers() {
        channelMembers.clear();
    }

}
