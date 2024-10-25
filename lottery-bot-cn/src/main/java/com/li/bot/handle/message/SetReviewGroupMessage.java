package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Objects;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class SetReviewGroupMessage implements IMessage {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileService fileService ;


    @Override
    public String getMessageName() {
        return "setReviewGroup";
    }


    @Override
    public synchronized void execute(BotServiceImpl bot, Message message) {
        Long tgId = message.getFrom().getId();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId).eq(User::getIsAdmin, true));
        if(Objects.isNull(user)){
            return;
        }

        List<String> groupIdList = fileService.getGroupIdList();
        if(groupIdList.contains(String.valueOf(message.getChatId()))){
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText("该群已设置为审核群了");
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }else{
            fileService.addGroupId(String.valueOf(message.getChatId()));
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText("审核群设置成功");
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
