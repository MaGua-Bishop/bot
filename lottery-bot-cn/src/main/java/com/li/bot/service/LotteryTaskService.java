package com.li.bot.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.li.bot.entity.database.Luckydraw;
import com.li.bot.mapper.LuckydrawIdMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LotteryTaskService {

    @Autowired
    private LuckydrawIdMapper luckydrawIdMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private BotServiceImpl bot;

    @Autowired
    private UserMapper userMapper;


    private static final int WINNER_COUNT = 6;

    public static String formatDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now().withHour(22).withMinute(0);
        // 定义格式化器，使用 "年月日时分" 格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        // 格式化并返回
        return localDateTime.format(formatter);
    }


    //1分钟执行一次
//    @Scheduled(cron = "0 0/1 * * * ?")
    // 每天22:00执行
    @Scheduled(cron = "0 0 22 * * ?")
    public void executeLottery() {
        LocalDate currentDate = LocalDate.now();

        // 获取当天的所有记录，且状态为0（未开奖）
        List<Luckydraw> luckydrawList = luckydrawIdMapper.findLuckydrawsBeforeCutoff(currentDate);

        if (!luckydrawList.isEmpty()) {
            // 批量更新所有参与者的状态为1
            luckydrawIdMapper.updateStatusToOne(luckydrawList.stream().map(Luckydraw::getLuckydrawId).collect(Collectors.toList()));
            log.info("开始进行抽奖... 参与用户数:{}", luckydrawList.size());
            BigDecimal totalPool = BigDecimal.valueOf(luckydrawList.size()).multiply(BigDecimal.TEN); // 每个用户10积分
            // 检查奖池积分是否达到100
            if (totalPool.compareTo(BigDecimal.valueOf(100)) >= 0) {
                //totalPool的70%
                BigDecimal Integral = totalPool.multiply(BigDecimal.valueOf(0.7)).setScale(0, RoundingMode.HALF_UP);
                log.info("奖池总积分:{},中奖积分:{}", totalPool.toString(), Integral.toString());
                // 进行抽奖，随机选择6位中奖者
                Collections.shuffle(luckydrawList);
                List<Luckydraw> winnerList = luckydrawList.stream().limit(WINNER_COUNT).collect(Collectors.toList());
                //获取每位中奖者的金额
                List<BigDecimal> moneyList = divideRedPacket(Integral, WINNER_COUNT);
                // 批量更新中奖者信息
                List<Luckydraw> info = new ArrayList<>();
                for (int i = 0; i < winnerList.size(); i++) {
                    Luckydraw luckydraw = winnerList.get(i);
                    luckydraw.setMoney(moneyList.get(i));
                    userMapper.addMoney(luckydraw.getTgId(), luckydraw.getMoney());
                    info.add(luckydraw);
                }
                luckydrawIdMapper.batchUpdate(winnerList, LocalDateTime.now());

                //构建中奖者信息
                String participants = info.stream()
                        .map(luckydraw -> "<code>" + luckydraw.getTgId() + "</code>\t中奖积分:<b>" + luckydraw.getMoney() + "</b>")
                        .collect(Collectors.joining("\n"));
                String text = "<b>" + formatDateTime() + "</b>开奖信息\n参与用户数:" + luckydrawList.size() + "\n奖池总积分:<b>" + Integral.toString() + "</b>" + "\n中奖用户: \n" + participants;

                //发送信息到工作群
                List<String> groupIdList = fileService.getGroupIdList();
                for (String groupId : groupIdList) {
                    try {
                        SendMessage sendMessage = SendMessage.builder().chatId(groupId).text(text).parseMode("html").build();
                        bot.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        continue;
                    }
                }
                //发送给每位用户
                for (Luckydraw luckydraw : luckydrawList) {
                    try {
                        SendMessage sendMessage = SendMessage.builder().chatId(luckydraw.getTgId()).text(text).parseMode("html").build();
                        bot.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        continue;
                    }
                }
            } else {
                log.info("没有达到100积分，今天则不开奖，参与用户数:{}", luckydrawList.size());
                LocalDateTime dateTime = LocalDateTime.now().plusDays(1).withHour(22).withMinute(0).withSecond(0).withNano(0);
                //发送信息到工作群
                List<String> groupIdList = fileService.getGroupIdList();
                for (String groupId : groupIdList) {
                    try {
                        // 构建参与用户的链接
                        String participants = luckydrawList.stream()
                                .map(luckydraw -> "<code>" + luckydraw.getTgId() + "</code>")
                                .collect(Collectors.joining("\n"));

                        String text = "<b>" + formatDateTime() + "</b>开奖信息:\n当前奖池积分未达到100，今天未进行开奖。\n下次开奖时间:" + dateTime + "\n参与用户数:" + luckydrawList.size() + "\n参与用户: \n" + participants;
                        SendMessage sendMessage = SendMessage.builder().chatId(groupId).text(text).parseMode("html").build();
                        bot.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        continue;
                    }
                }
                //发送给每位用户
                for (Luckydraw luckydraw : luckydrawList) {
                    luckydraw.setLuckydrawTime(dateTime);
                    luckydraw.setStatus(0);
                    luckydrawIdMapper.update(luckydraw, new UpdateWrapper<Luckydraw>().eq("luckydraw_id", luckydraw.getLuckydrawId()));
                    try {
                        SendMessage sendMessage = SendMessage.builder().chatId(luckydraw.getTgId()).text("<b>" + formatDateTime() + "</b>开奖信息:\n当前奖池积分未达到100，今天未进行开奖。\n下次开奖时间:" + dateTime + "\n参与用户数:" + luckydrawList.size() + "\n").parseMode("html").build();
                        bot.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        continue;
                    }
                }

            }
        } else {
            log.info("没有抽奖的用户");
            List<String> groupIdList = fileService.getGroupIdList();
            for (String groupId : groupIdList) {
                try {
                    String text = "<b>" + formatDateTime() + "</b>开奖信息:\n今日无用户参与\n参与用户数:0";
                    SendMessage sendMessage = SendMessage.builder().chatId(groupId).text(text).parseMode("html").build();
                    bot.execute(sendMessage);
                } catch (TelegramApiException e) {
                    continue;
                }
            }
        }
    }

    private BigDecimal getPositiveRandomAmount(NormalDistribution normalDist, BigDecimal max) {
        double randomValue;
        do {
            randomValue = normalDist.sample();
        } while (randomValue < 0 || randomValue > max.doubleValue());

        return new BigDecimal(randomValue).setScale(2, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> divideRedPacket(BigDecimal totalAmount, Integer count) {
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
