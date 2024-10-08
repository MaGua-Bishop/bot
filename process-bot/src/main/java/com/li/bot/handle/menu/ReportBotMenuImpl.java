//package com.li.bot.handle.menu;
//
//import com.google.common.collect.Lists;
//import com.li.bot.utils.BotMessageUtils;
//import com.li.bot.service.impl.BotServiceImpl;
//import com.li.bot.utils.entity.StartMessage;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Message;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * @Author: li
// * @CreateTime: 2024-09-29
// * @Description: 报单菜单实现类
// */
//@Component
//public class ReportBotMenuImpl implements IBotMenu {
//
//    @Override
//    public String getMenuName() {
//        return "报单";
//    }
//
//    private InlineKeyboardMarkup createInlineKeyboardButton(){
//        List<InlineKeyboardButton> buttonList = new ArrayList<>();
//        buttonList.add(InlineKeyboardButton.builder().text("报修").callbackData("用户报修").build());
//        buttonList.add(InlineKeyboardButton.builder().text("其他").callbackData("用户其他").build());
//        buttonList.add(InlineKeyboardButton.builder().text("其他").callbackData("用户其他").build());
//        buttonList.add(InlineKeyboardButton.builder().text("其他").callbackData("用户其他").build());
//
//        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 5);
//
//
//        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
//
//        return inlineKeyboardMarkup;
//    }
//
//    private void startMessage(BotServiceImpl bot,Long chatId){
//        InlineKeyboardMarkup inlineKeyboardButton = createInlineKeyboardButton();
//
//        SendMessage sendMessage = new SendMessage();
//        sendMessage.setChatId(chatId);
//        sendMessage.setText("请选择下方按钮");
//        sendMessage.enableMarkdownV2(true);
//        sendMessage.setReplyMarkup(inlineKeyboardButton);
//        try {
//            bot.execute(sendMessage);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void execute(BotServiceImpl bot, Message message) {
//        startMessage(bot, message.getChatId());
//    }
//}
