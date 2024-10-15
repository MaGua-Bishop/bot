package com.li.bot.controller;

import com.li.bot.entity.RechargeDTO;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.TelegramBotServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final TelegramBotServiceImpl telegramBotService ;

    public TelegramBotController(BotServiceImpl botService, TelegramBotServiceImpl telegramBotService)
    {
        this.botService = botService;
        this.telegramBotService = telegramBotService;
    }

    @PostMapping("/process_bot")
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

    @PostMapping("/recharge")
    public ResponseEntity<?> recharge(@RequestBody RechargeDTO rechargeDTO){
        log.info("服务端接收到的参数");
        log.info("转出账户:{}", rechargeDTO.getFrom());
        log.info("转入账户:{}", rechargeDTO.getTo());
        log.info("转入金额:{}", rechargeDTO.getAmount());
        log.info("交易时间:{}", rechargeDTO.getTransactionTime());
        log.info("订单号:{}", rechargeDTO.getTxId());
        telegramBotService.userRecharge(rechargeDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
