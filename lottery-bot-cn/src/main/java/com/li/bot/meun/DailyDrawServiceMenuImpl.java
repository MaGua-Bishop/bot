package com.li.bot.meun;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class DailyDrawServiceMenuImpl implements IBotMenu {

    @Override
    public String getMenuName() {
        return "每日抽奖";
    }

    @Autowired
    private UserMapper userMapper;

    private InlineKeyboardMarkup createInlineKeyboardButton() {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("参与抽奖").callbackData("userDailyDraw").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) {
        String text = "<b>参与每日抽奖规则：</b>\n\n" +
                "1. 每天可使用 <b>10积分</b> 参与抽奖。\n" +
                "2. 当奖池达到 <b>100积分</b> 或以上时，将在每天晚上 <b>22:00</b> 公布中奖信息。\n" +
                "3. 每次抽奖将随机选出 <b>6位中奖者</b>，每位中奖者将随机获得奖池中的一部分积分。\n" +
                "4. 如果当天奖池未达到 <b>100积分</b>，则当天不开奖，所有参与用户会自动进入下次开奖。\n" +
                "5. <b>中奖积分自动发放</b>\n\n" +
                "点击下方<b>参与抽奖</b>按钮，即可参与";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(text);
        sendMessage.setParseMode("html");
        sendMessage.setReplyMarkup(createInlineKeyboardButton());
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
