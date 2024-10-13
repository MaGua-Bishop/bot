package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.config.BotConfig;
import com.li.bot.handle.message.IMessage;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.utils.BotMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class StartCallback implements ICallback {

    @Override
    public String getCallbackName() {
        return "/start";
    }

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private BotConfig botConfig ;

    @Autowired
    private FileService fileService ;
    private InlineKeyboardMarkup createInlineKeyboardButton(com.li.bot.entity.database.User user) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        // 添加前两个按钮
        buttonList.add(InlineKeyboardButton.builder()
                .text("\uD83C\uDF38手机邀请进频道")
                .url("https://" + botConfig.getBotname() + "?startgroup")
                .build());
        buttonList.add(InlineKeyboardButton.builder()
                .text("\uD83C\uDF1E电脑邀请进频道")
                .url("https://" + botConfig.getBotname() + "?startchannel=true")
                .build());

        // 添加第三个按钮，单独一行
        buttonList.add(InlineKeyboardButton.builder()
                .text("\uD83C\uDF1E频道互推\uD83C\uDF1E")
                .callbackData("selectConvoysList")
                .build());

        // 获取并添加第四个按钮，单独一行
        Map<String, String> map = fileService.getAdminChannelList();
        String url = map.get("url");
        buttonList.add(InlineKeyboardButton.builder()
                .text("❔车队教程")
                .url(url)
                .build());

        // 如果用户是管理员，添加额外的按钮
        if (user.getIsAdmin()) {
            buttonList.add(InlineKeyboardButton.builder()
                    .text("\uD83D\uDE97创建车队")
                    .callbackData("adminAddConvoys")
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("创建互推导航按钮")
                    .callbackData("adminAddButton")
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("\uD83D\uDDD1删除互推导航按钮")
                    .callbackData("adminDeleteButton")
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("\uD83D\uDDB9创建顶部文案")
                    .callbackData("adminAddText")
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("\uD83D\uDDB9创建底部文案")
                    .callbackData("adminAddBottomText")
                    .build());
        }

        // 创建行列表，手动指定每行的按钮
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        // 前两个按钮在同一行
        rowList.add(buttonList.subList(0, 2));
        // 第三个按钮单独一行
        rowList.add(Arrays.asList(buttonList.get(2)));
        // 第四个按钮单独一行
        rowList.add(Arrays.asList(buttonList.get(3)));

        // 如果有管理员按钮，继续添加它们，每两个一组
        for (int i = 4; i < buttonList.size(); i += 2) {
            if (i + 1 < buttonList.size()) {
                // 两个按钮在同一行
                rowList.add(Arrays.asList(buttonList.get(i), buttonList.get(i + 1)));
            } else {
                // 只有一个按钮，单独一行
                rowList.add(Arrays.asList(buttonList.get(i)));
            }
        }

        // 构建并返回 InlineKeyboardMarkup
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                .keyboard(rowList)
                .build();
        return inlineKeyboardMarkup;
    }


    private com.li.bot.entity.database.User addUser(Long tgId,String userName){
        com.li.bot.entity.database.User user = userMapper.selectOne(new LambdaQueryWrapper<com.li.bot.entity.database.User>().eq(com.li.bot.entity.database.User::getTgId, tgId));
        if (user != null ){
            return user;
        }else {
            com.li.bot.entity.database.User u = new com.li.bot.entity.database.User();
            u.setTgId(tgId);
            u.setTgName(userName);
            u.setIsAdmin(false);
            userMapper.insert(u);
            return u ;
        }
    }


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        User user = callbackQuery.getFrom();
        Long tgId = user.getId();
        String userName =  user.getFirstName() + user.getLastName() ;


        com.li.bot.entity.database.User u = addUser(tgId, userName);

        bot.execute(EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .parseMode("html")
                .text(BotMessageUtils.getStartMessage(tgId,userName,botConfig.getBotname()))
                .replyMarkup(createInlineKeyboardButton(u))
                .build());

//        SendMessage send = SendMessage.builder().text().chatId(callbackQuery.getMessage().getChatId()).replyMarkup(createInlineKeyboardButton(u)).parseMode("html").build();
//
//        bot.execute(send);
    }
}
