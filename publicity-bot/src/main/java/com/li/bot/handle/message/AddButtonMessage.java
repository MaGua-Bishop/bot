package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Button;
import com.li.bot.mapper.ButtonMapper;
import com.li.bot.service.impl.BotServiceImpl;
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
public class AddButtonMessage implements IMessage{

    @Override
    public String getMessageName() {
        return "addButton";
    }


    @Autowired
    private ButtonMapper buttonMapper ;

    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        String text = message.getText();

        // 定义正则表达式模式
        Pattern pattern = Pattern.compile(
                "#按钮名\\s+(.*?)\\n" +
                        "#链接地址\\s+(.*?)(?=\\n|$)"
        );

        // 创建Matcher对象
        Matcher matcher = pattern.matcher(text);

        // 检查是否找到匹配项
        if (matcher.find()) {
            // 提取匹配的内容
            String name = matcher.group(1).trim();
            String url = matcher.group(2).trim();
            if(url.startsWith("@")){
                url = url.replace("@", "https://t.me/");
            }

            Button one = buttonMapper.selectOne(new LambdaQueryWrapper<Button>().eq(Button::getName, name));
            if(one != null){
                bot.execute(SendMessage.builder()
                        .chatId(message.getChatId().toString())
                        .text("按钮名存在,请重新添加")
                        .build());
                return;
            }

            Button button = new Button();
            button.setName(name);
            button.setUrl(url);
            button.setTgId(message.getFrom().getId());
            buttonMapper.insert(button);

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
