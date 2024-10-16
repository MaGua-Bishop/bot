package com.li.bot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;

/**
 * @Author: li
 * @CreateTime: 2024-10-16
 */
@Component
public class AdminGroupServiceImpl {

    private final BotServiceImpl bot;

    private final FileService fileService;

    public AdminGroupServiceImpl(@Lazy BotServiceImpl bot, FileService fileService) {
        this.bot = bot;
        this.fileService = fileService;
    }
    private Long chatId =null;

    public synchronized Long getChatId() {
        if(chatId == null){
            String groupChatId = fileService.getGroupChatId();
            chatId = Long.valueOf(groupChatId);
        }
        return chatId;
    }


}
