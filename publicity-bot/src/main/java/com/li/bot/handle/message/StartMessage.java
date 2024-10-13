package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.utils.BotMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class StartMessage implements IMessage{

    @Override
    public String getMessageName() {
        return "/start";
    }

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private BotConfig botConfig ;

    @Autowired
    private FileService fileService ;


    private InlineKeyboardMarkup createInlineKeyboardButton(com.li.bot.entity.database.User user){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        buttonList.add(InlineKeyboardButton.builder().text("➕邀请到频道").url("https://"+botConfig.getBotname()+"?startchannel=true").build());
        buttonList.add(InlineKeyboardButton.builder().text("➕邀请到群聊").url("https://"+botConfig.getBotname()+"?startgroup").build());
//        buttonList.add(InlineKeyboardButton.builder().text("车队列表").url("https://t.me/lidemobot?startchannel=true").build());
        buttonList.add(InlineKeyboardButton.builder().text("\uD83D\uDE97查看车队").callbackData("selectConvoysList").build());
        Map<String, String> map = fileService.getAdminChannelList();
        String url = map.get("url");
        buttonList.add(InlineKeyboardButton.builder().text("审批频道").url(url).build());
        if(user.getIsAdmin()){
            buttonList.add(InlineKeyboardButton.builder().text("\uD83D\uDE97创建车队").callbackData("adminAddConvoys").build());
            buttonList.add(InlineKeyboardButton.builder().text("创建互推导航按钮").callbackData("adminAddButton").build());
            buttonList.add(InlineKeyboardButton.builder().text("\uD83D\uDDD1删除互推导航按钮").callbackData("adminDeleteButton").build());
            buttonList.add(InlineKeyboardButton.builder().text("\uD83D\uDDB9创建顶部文案").callbackData("adminAddText").build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("\uD83D\uDDB9创建底部文案")
                    .callbackData("adminAddBottomText")
                    .build());

        }

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
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
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {

        User user = message.getFrom();
        Long tgId = user.getId();
        String userName = user.getUserName();

        com.li.bot.entity.database.User u = addUser(tgId, userName);

        SendMessage send = SendMessage.builder().text(BotMessageUtils.getStartMessage(tgId,userName)).chatId(message.getChatId()).replyMarkup(createInlineKeyboardButton(u)).build();

        bot.execute(send);
    }
}
