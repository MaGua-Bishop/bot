package com.li.bot.controller;

import com.li.bot.service.impl.BotServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@RestController
@Slf4j
public class TelegramBotController {

    private final BotServiceImpl botService ;


    public TelegramBotController(BotServiceImpl botService)
    {
        this.botService = botService;
    }

    @PostMapping("/tgbot/publicity_bot_new")
    public ResponseEntity<?> handleUpdate(@RequestBody Update update) {
        try {
            // 调用你的Bot处理更新的方法
            botService.onWebhookUpdateReceived(update);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            // 处理异常情况
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

}
