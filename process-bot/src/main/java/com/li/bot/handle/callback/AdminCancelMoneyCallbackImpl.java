package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.UserMoney;
import com.li.bot.mapper.UserMapper;
import com.li.bot.mapper.UserMoneyMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class AdminCancelMoneyCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminCancelMoney";
    }

    @Autowired
    private UserMoneyMapper userMoneyMapper;

    @Autowired
    private UserMapper userMapper;

    private InlineKeyboardMarkup createButton(String name) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).callbackData("无").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        Long moneyId = Long.parseLong(data.substring(data.lastIndexOf(":") + 1));
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, callbackQuery.getFrom().getId()));
        if (!user.getIsAdmin()) {
            System.out.println("非管理员");
            return;
        }
        UserMoney userMoney = userMoneyMapper.selectOne(new LambdaQueryWrapper<UserMoney>().eq(UserMoney::getMoneyId, moneyId));
        SendMessage sendMessage = SendMessage.builder().chatId(userMoney.getTgId()).text("您的扫码充值失败\n充值金额:" + userMoney.getMoney()).parseMode("html").build();
        bot.execute(sendMessage);
        userMoneyMapper.deleteById(moneyId);
        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createButton("已取消转账")).build();
        bot.execute(editMessageReplyMarkup);
    }
}
