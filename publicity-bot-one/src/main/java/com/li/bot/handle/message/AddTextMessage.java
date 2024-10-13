package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.TextAndLink;
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

import java.util.ArrayList;
import java.util.List;
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

        Pattern adPattern = Pattern.compile("#AD\\s+(.*?)\\s+(.*?)(?=\\n#|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher adMatcher = adPattern.matcher(text);
        List<TextAndLink> list = new ArrayList<>();
        while (adMatcher.find()) {
            String link = adMatcher.group(1).trim();  // 提取链接
            String t = adMatcher.group(2).trim();  // 提取文字
            list.add(new TextAndLink(t, link));
        }

        Pattern copyPattern = Pattern.compile("#文案\\s+(.*?)(?=\\n)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher copyMatcher = copyPattern.matcher(text);

        // 查找并设置文案
        String copyText = "";
        if (copyMatcher.find()) {
            copyText = copyMatcher.group(1).trim();  // 提取文案
        }

        if(copyText.equals("")&&list.isEmpty()){
            bot.execute(SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("添加失败 请按指定格式填写")
                    .build());
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(copyText+"\n");
        for (TextAndLink textAndLink : list) {
            String link = textAndLink.getLink();
            String t = textAndLink.getText();
            sb.append("AD: <a href=\""+link+"\">"+t+"</a>\n");
        }

            fileService.addText(String.valueOf(sb));

            bot.execute(SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("添加成功")
                    .build());
    }
}
