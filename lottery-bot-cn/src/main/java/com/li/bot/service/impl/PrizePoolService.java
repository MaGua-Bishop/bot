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
    private LotteryMapper lotteryMapper;

    @Autowired
    @Lazy
    private BotServiceImpl bot;
    @Autowired
    @Lazy
    private PrizePoolMapper prizePoolMapper;


    @Transactional
    public void add(String lotteryId, BigDecimal money, Integer number) {
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
        log.info("抽奖id:{},奖金池:{}", lotteryId, moneyList);
    }


    private InlineKeyboardMarkup createInlineKeyboardButton() {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("已结束").callbackData("null").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }


    /**
     * 随机获取一个奖金 没有了就返回空
     *
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
            SendMessage sendMessage = SendMessage.builder().chatId(lottery.getChatId()).text("抽奖id:<code>" + lottery.getLotteryId() + "</code>\n抽奖已结束\n请发送\n<b><code>/view " + lottery.getLotteryId() + "</code></b>\n命令查看中奖者名单").parseMode("html").build();
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

    public List<BigDecimal> divideRedPacket(BigDecimal totalAmount, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("红包个数必须大于 0");
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("总金额必须大于 0");
        }

        // 转换为分，避免精度问题
        int totalCents = totalAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.DOWN).intValue();
        if (count > totalCents) {
            throw new IllegalArgumentException("红包个数不能大于总金额的分单位数");
        }

        // 初始化红包列表
        List<Integer> redPacketsInCents = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            // 确保每个红包金额在合理范围内，例如 0.5x 到 1.5x 的平均值
            int avgCents = totalCents / (count - i);
            int minCents = Math.max(1, (int) (0.5 * avgCents));
            int maxCents = Math.min(totalCents - (count - i - 1), (int) (1.5 * avgCents));
            int amount = random.nextInt(maxCents - minCents + 1) + minCents;

            redPacketsInCents.add(amount);
            totalCents -= amount;
        }

        // 转换为 BigDecimal 并确保总金额一致
        List<BigDecimal> redPackets = new ArrayList<>();
        for (int cents : redPacketsInCents) {
            redPackets.add(BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        // 调整误差
        BigDecimal total = redPackets.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal difference = totalAmount.subtract(total);
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            redPackets.set(0, redPackets.get(0).add(difference));
        }

        return redPackets;
    }
}
