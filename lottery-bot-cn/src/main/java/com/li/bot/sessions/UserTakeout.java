package com.li.bot.sessions;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Takeout;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.TakeoutMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.meun.BotMenuFactory;
import com.li.bot.meun.IBotMenu;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.sessions.enums.UserTakeoutSessionState;
import com.li.bot.utils.BotSendMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Slf4j
public class UserTakeout {

    private BotServiceImpl bot;

    private Message message;

    private UserTakeoutSessionList userTakeoutSessionList;


    private UserMapper userMapper;

    private TakeoutMapper takeoutMapper;

    private FileService fileService;


    public UserTakeout(BotServiceImpl bot, Message message, UserTakeoutSessionList userTakeoutSessionList, UserMapper userMapper, TakeoutMapper takeoutMapper, FileService fileService) {
        this.bot = bot;
        this.message = message;
        this.userTakeoutSessionList = userTakeoutSessionList;
        this.userMapper = userMapper;
        this.takeoutMapper = takeoutMapper;
        this.fileService = fileService;
    }

    private InlineKeyboardMarkup createButton(Long takeoutId) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("同意").callbackData("admin:review:takeout:type:" + 0 + ":takeoutId:" + takeoutId).build());
        buttonList.add(InlineKeyboardButton.builder().text("拒绝").callbackData("admin:review:takeout:type:" + 1 + ":takeoutId:" + takeoutId).build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    public void execute(BotMenuFactory botMenuFactory) {
        IBotMenu menu = botMenuFactory.getMenu(message.getText());
        if (menu != null) {
            userTakeoutSessionList.removeUserSession(message.getChatId());
            menu.execute(bot, message);
            return;
        }
        UserTakeoutSession userSession = userTakeoutSessionList.getUserTakeoutSession(message.getFrom().getId());
        UserTakeoutSessionState state = userSession.getState();
        switch (state) {
            case WAITING_FOR_USER_MESSAGE:
                handleUserMessageInput(message, userSession);
                break;
            default:
                break;
        }
    }

    private BigDecimal isMoney(String money) {
        // 使用正则表达式验证是否是数字且最多保留两位小数
        Pattern pattern = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
        Matcher matcher = pattern.matcher(money);

        if (matcher.matches()) {
            // 使用 DecimalFormat 保留两位小数
            DecimalFormat df = new DecimalFormat("0.00");
            String format = df.format(Double.parseDouble(money));
            return new BigDecimal(format);
        } else {
            // 如果不是有效的数字或格式不正确，返回 null
            return null;
        }
    }

    private void handleUserMessageInput(Message message, UserTakeoutSession userSession) {
        String text = message.getText();

        BigDecimal money = isMoney(text);
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text("输入的积分不正确").build();
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            userTakeoutSessionList.removeUserSession(message.getChatId());
            return;
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, message.getFrom().getId()));
        if (Objects.isNull(user)) {
            return;
        }
        if (user.getMoney().compareTo(money) < 0) {
            SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text("您的积分不足").build();
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            userTakeoutSessionList.removeUserSession(message.getChatId());
            return;
        }

        user.setMoney(user.getMoney().subtract(money));
        userMapper.updateById(user);

        Takeout takeout = new Takeout();
        takeout.setTgId(message.getFrom().getId());
        takeout.setMoney(money);
        takeout.setStatus(0);
        int insert = takeoutMapper.insert(takeout);
        if (insert > 0) {
            SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text("提现申请成功\n请等待提现审核").build();
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            List<String> groupIdList = fileService.getGroupIdList();
            for (String groupId : groupIdList) {
                SendMessage groupMessage = SendMessage.builder().chatId(groupId).text(BotSendMessageUtils.getAdminReviewMessage(user, takeout)).replyMarkup(createButton(takeout.getTakeoutId())).parseMode("html").build();
                try {
                    bot.execute(groupMessage);
                } catch (TelegramApiException e) {
                    System.out.println("提现错误:" + e);
                    continue;
                }
            }
        }
        userTakeoutSessionList.removeUserSession(message.getChatId());
    }
}
