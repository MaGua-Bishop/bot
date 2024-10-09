//package com.li.bot.task;
//
//import com.li.bot.service.impl.BotServiceImpl;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//
//@Component
//public class HelloWordTask {
//
//    @Autowired
//    private BotServiceImpl botService ;
//
//
//    @Scheduled(cron ="*/10 * * * * ?")
//    public void sayWord() {
//        try {
//            botService.execute(SendMessage.builder()
//                    .chatId(2142298091L)
//                    .text("您好，这是定时推送的消息").build());
//        } catch (TelegramApiException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//}
