package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Takeout;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.TakeoutMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.UserTakeoutSessionList;
import com.li.bot.utils.BotSendMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class AdminReviewTakeoutCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminReviewTakeout";
    }

    @Autowired
    private TakeoutMapper takeoutMapper;

    @Autowired
    private UserMapper userMapper;

    private InlineKeyboardMarkup createButton(String name) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text(name).callbackData("null").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private void adminReview(BotServiceImpl bot, CallbackQuery callbackQuery, Long takeoutId, Integer type) {
        Takeout takeout = takeoutMapper.selectOne(new LambdaQueryWrapper<Takeout>().eq(Takeout::getTakeoutId, takeoutId).eq(Takeout::getStatus, 0));
        if (Objects.isNull(takeout)) {
            try {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("提现申请已处理").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, takeout.getTgId()));

        //更新状态
        String name = "";
        SendMessage sendMessage = null;
        if (type == 0) {
            takeout.setStatus(1);
            name = "\uD83D\uDFE2已同意";
            sendMessage = SendMessage.builder().chatId(user.getTgId()).text("您的提现积分:<b>" + takeout.getMoney() + "</b>\n已同意\n请等待客服处理").parseMode("html").build();
        } else if (type == 1) {
            takeout.setStatus(-1);
            user.setMoney(user.getMoney().add(takeout.getMoney()));
            userMapper.updateById(user);
            name = "\uD83D\uDD34已拒绝";
            sendMessage = SendMessage.builder().chatId(user.getTgId()).text("您的提现积分:<b>" + takeout.getMoney() + "</b>\n已拒绝").parseMode("html").build();

        }
        takeout.setReviewTgId(callbackQuery.getFrom().getId());
        takeout.setUpdateTime(LocalDateTime.now());
        takeoutMapper.updateById(takeout);


        EditMessageText messageText = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(BotSendMessageUtils.getAdminReviewMessage(user, takeout))
                .parseMode("html")
                .replyMarkup(createButton(name))
                .build();
        try {
            bot.execute(messageText);
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("提现错误:" + e);
            System.out.println("error");
        }
    }

    @Override
    @Transactional
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, callbackQuery.getFrom().getId()));
        if (!user.getIsAdmin()) {
            return;
        }

        //正则解析
        String regex = "admin:review:takeout:type:(\\d+):takeoutId:(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);

        if (matcher.matches()) {
            //获取type和takeoutId
            Integer type = Integer.valueOf(matcher.group(1));
            String takeoutId = matcher.group(2);
            adminReview(bot, callbackQuery, Long.valueOf(takeoutId), type);
        }
    }
}
