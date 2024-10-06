package com.li.bot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.RechargeDTO;
import com.li.bot.entity.database.Recharge;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.RechargeMapper;
import com.li.bot.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

/**
 * @Author: li
 * @CreateTime: 2024-10-05
 */
@Service
@Slf4j
public class TelegramBotServiceImpl {

    @Autowired
    private BotServiceImpl botService;
    @Autowired
    private UserMapper userMapper ;
    @Autowired
    private RechargeMapper rechargeMapper;

    public void userRecharge(RechargeDTO rechargeDTO){
        Recharge recharge = rechargeMapper.selectWithinTenMinutesRecharge(rechargeDTO.getAmount());
        if(recharge == null){
            log.info("金额:{}.没找到对应的用户",rechargeDTO.getAmount());
            return ;
        }
        Long tgId = recharge.getTgId();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));
        log.info("用户id:{}.当前余额:{}",tgId,user.getMoney());
        user.setMoney(user.getMoney().add(rechargeDTO.getAmount()));
        log.info("用户id:{}.充值后余额:{}",tgId,user.getMoney());
        userMapper.updateById(user);

        recharge.setStatus(1);
        recharge.setUpdateTime(LocalDateTime.now());
        rechargeMapper.updateById(recharge);

        SendMessage message = SendMessage.builder().chatId(tgId).text("充值成功\n充值金额:"+rechargeDTO.getAmount() + "\n用户当前余额:" + user.getMoney()).build();
        try {
            botService.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }


}
