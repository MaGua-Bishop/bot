package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Convoys;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FleetService;
import com.li.bot.utils.BotMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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
public class AddConvoysMessage implements IMessage{

    @Override
    public String getMessageName() {
        return "addConvoys";
    }


    private  final FleetService fleetService ;

    public AddConvoysMessage(@Lazy FleetService fleetService) {
        this.fleetService = fleetService;
    }




    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {
        String text = message.getText();

        // 定义正则表达式模式
        Pattern pattern = Pattern.compile(
                "(?s)" +              // 单行模式，使.匹配包括换行符在内的所有字符
                        "#车队标题\\s+(.*?)\\n" +  // 匹配 #车队名 后面跟着任意非空白字符，直到下一个换行符
                        "#车队介绍\\s+(.*?)\\n" +  // 匹配 #车队文案 后面跟着任意非空白字符，直到下一个换行符
                        "#车队容量\\s+(\\d+)\\n" +  // 匹配 #车队容量 后面跟着一个或多个数字，直到下一个换行符
                        "#最低订阅数量\\s+(\\d+)\\n"+  // 匹配 #频道最低订阅数量 后面跟着一个或多个数字
                        "#最低阅读数量\\s+(\\d+)"
        );

        // 创建Matcher对象
        Matcher matcher = pattern.matcher(text);

        // 检查是否找到匹配项
        if (matcher.find()) {
            // 提取匹配的内容
            String fleetName = matcher.group(1).trim();  // 车队标题
            String fleetText = matcher.group(2).trim();  // 车队介绍
            Long fleetCapacity = Long.valueOf(matcher.group(3));  // 车队容量
            Long minSubscribers = Long.valueOf(matcher.group(4));  // 频道最低订阅数量
            Long read = Long.valueOf(matcher.group(5));  // 阅读

            Convoys convoys = new Convoys();
            convoys.setName(fleetName);
            convoys.setCopywriter(fleetText);
            convoys.setCapacity(fleetCapacity);
            convoys.setSubscription(minSubscribers);
            convoys.setRead(read);
            convoys.setIntervalMinutes(15);
            convoys.setTgId(message.getFrom().getId());

            fleetService.addNewConvoy(convoys);

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
