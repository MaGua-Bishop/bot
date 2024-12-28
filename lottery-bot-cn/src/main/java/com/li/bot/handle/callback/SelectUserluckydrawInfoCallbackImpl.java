package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Luckydraw;
import com.li.bot.entity.database.Takeout;
import com.li.bot.enums.TakeoutStatus;
import com.li.bot.mapper.LuckydrawIdMapper;
import com.li.bot.mapper.TakeoutMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class SelectUserluckydrawInfoCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "selectUserluckydrawInfo";
    }

    @Autowired
    private LuckydrawIdMapper luckydrawIdMapper;

    private InlineKeyboardMarkup createButton() {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("返回").callbackData("DeleteMessage").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    public static String formatDateTime(LocalDateTime localDateTime) {
        // 定义格式化器，使用 "年月日时分" 格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        // 格式化并返回
        return localDateTime.format(formatter);
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {

        List<Luckydraw> LuckydrawList = luckydrawIdMapper.selectList(new LambdaQueryWrapper<Luckydraw>().eq(Luckydraw::getTgId, callbackQuery.getFrom().getId()).orderByDesc(Luckydraw::getLuckydrawTime));
        StringBuilder text = new StringBuilder();
        if (LuckydrawList.isEmpty()) {
            text.append("暂无抽奖记录");
        } else {
            int i = 1;
            text.append("说明:序号|开奖状态|中奖金额|开奖时间\n");
            for (Luckydraw luckydraw : LuckydrawList) {
                String status = luckydraw.getStatus() == 0 ? "待开奖" : "已开奖";
                String money = "";
                if (luckydraw.getStatus() == 0) {
                    money = "<b>待开奖</b>\t";
                } else {
                    money = luckydraw.getMoney() == null ? "<b>未中奖</b>\t" : "<b>" + luckydraw.getMoney().toString() + "</b>\t";
                }
                text.append(i).append(". ").append("<b>" + status + "</b>\t").append(money).append(formatDateTime(luckydraw.getLuckydrawTime()) + "\n");
                i++;
            }
        }
        //用户提现记录
        SendMessage messageText = SendMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .text(String.valueOf(text))
                .parseMode("html")
                .replyMarkup(createButton())
                .build();
        try {
            bot.execute(messageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
