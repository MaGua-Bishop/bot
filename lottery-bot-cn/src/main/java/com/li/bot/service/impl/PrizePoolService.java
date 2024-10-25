package com.li.bot.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.PrizePool;
import com.li.bot.entity.vo.PrizePoolVO;
import com.li.bot.enums.LotteryStatus;
import com.li.bot.mapper.LotteryMapper;
import com.li.bot.mapper.PrizePoolMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
@Component
@Slf4j
public class PrizePoolService {

    @Autowired
    @Lazy
    private  LotteryMapper lotteryMapper;

    @Autowired
    @Lazy
    private BotServiceImpl bot ;
    @Autowired
    @Lazy
    private PrizePoolMapper prizePoolMapper ;


    @Transactional
    public void add(String lotteryId,BigDecimal money,Integer number) {
        List<BigDecimal> moneyList = divideRedPacket(money, number);
        List<PrizePool> prizePoolList = new ArrayList<>();
        moneyList.forEach(amount -> {
            PrizePool prizePool = new PrizePool();
            prizePool.setPrizePoolId(IdUtil.randomUUID());
            prizePool.setLotteryId(lotteryId);
            prizePool.setMoney(amount);
            prizePoolList.add(prizePool);
        });
        prizePoolMapper.batchSavePrizePool(prizePoolList);
        log.info("抽奖id:{},奖金池:{}",lotteryId,moneyList);
    }


    private InlineKeyboardMarkup createInlineKeyboardButton(){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("已结束").callbackData("null").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    /**
     * 随机获取一个奖金 没有了就返回空
     * @param lotteryId
     * @return
     */
    public String getRandomMoney(String lotteryId) {
        List<PrizePoolVO> moneyList = prizePoolMapper.getRandomMoney(lotteryId);
        //没有金额了更新状态
        if (moneyList.isEmpty()) {
            //更新状态
            Lottery lottery = lotteryMapper.selectOne(new LambdaQueryWrapper<Lottery>().eq(Lottery::getLotteryId, lotteryId));
            lottery.setStatus(LotteryStatus.END.getCode());
            lottery.setUpdateTime(LocalDateTime.now());
            lotteryMapper.updateById(lottery);
            EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(lottery.getChatId()).messageId(Integer.valueOf(String.valueOf(lottery.getMessageId()))).replyMarkup(createInlineKeyboardButton()).build();
            SendMessage sendMessage = SendMessage.builder().chatId(lottery.getChatId()).text("抽奖id:<code>" + lottery.getLotteryId() + "</code>\n抽奖已结束\n请发送\n<b><code>/view "+lottery.getLotteryId()+"</code></b>\n命令查看中奖者名单").parseMode("html").build();
            try {
                bot.execute(editMessageReplyMarkup);
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
        List<String> prizePoolIds = moneyList.stream().map(PrizePoolVO::getPrizePoolId).collect(Collectors.toList());
        int index = new Random().nextInt(prizePoolIds.size());

        return prizePoolIds.get(index);
    }
    private BigDecimal getPositiveRandomAmount(NormalDistribution normalDist, BigDecimal max) {
        double randomValue;
        do {
            randomValue = normalDist.sample();
        } while (randomValue < 0 || randomValue > max.doubleValue());

        return new BigDecimal(randomValue).setScale(2, RoundingMode.HALF_UP);
    }

    public List<BigDecimal> divideRedPacket(BigDecimal totalAmount, Integer count) {
        NormalDistribution normalDist = new NormalDistribution(totalAmount.divide(new BigDecimal(count), 2, RoundingMode.DOWN).doubleValue(), 10.0);

        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal remainingAmount = totalAmount;

        for (int i = 0; i < count - 1; i++) {
            BigDecimal randomAmount = getPositiveRandomAmount(normalDist, remainingAmount);
            amounts.add(randomAmount);
            remainingAmount = remainingAmount.subtract(randomAmount).setScale(2, RoundingMode.DOWN);
        }

        amounts.add(remainingAmount);
        return amounts;
    }

}
