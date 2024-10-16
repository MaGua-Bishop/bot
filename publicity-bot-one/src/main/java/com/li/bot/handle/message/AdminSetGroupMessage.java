package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.AddAdminGroup;
import com.li.bot.entity.TextAndLink;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ExportChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class AdminSetGroupMessage implements IMessage{

    @Override
    public String getMessageName() {
        return "adminSetGroup";
    }


    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private FileService fileService ;

    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        String text = message.getText();
        Long chatId = message.getChatId();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, message.getFrom().getId()));
        if(user.getIsAdmin()){
            ExportChatInviteLink exportChatInviteLink = new ExportChatInviteLink(String.valueOf(chatId));
            String link = bot.execute(exportChatInviteLink);
            AddAdminGroup addAdminGroup = new AddAdminGroup();
            addAdminGroup.setId(chatId);
            addAdminGroup.setLink(link);

            fileService.addAdminGroup(addAdminGroup);

            SendMessage sendMessage = SendMessage.builder().chatId(chatId).text("设置成功").build();
            bot.execute(sendMessage);

        }


    }
}
