package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Button;
import com.li.bot.mapper.ButtonMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.service.impl.FleetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class AddTextMessage implements IMessage{

    @Override
    public String getMessageName() {
        return "addText";
    }


    @Autowired
    private FileService fileService ;

    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        String text = message.getText();

        // 定义正则表达式模式
        String regex = "#顶部文案\\s*(.*)";
        Pattern pattern = Pattern.compile(regex);

        // 创建Matcher对象
        Matcher matcher = pattern.matcher(text);

        // 检查是否找到匹配项
        if (matcher.find()) {
            // 提取匹配的内容
            String t = matcher.group(1).trim();

            fileService.addText(t);

            bot.execute(SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("添加成功")
                    .build());
        } else {
            bot.execute(SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("添加失败 请按指定格式填写")
                    .build());
        }


    }
}
