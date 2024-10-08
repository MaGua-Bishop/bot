package com.li.bot.handle.callback;

import com.li.bot.entity.database.Recharge;
import com.li.bot.mapper.RechargeMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
public class UserRechargeCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "userRecharge";
    }


    @Autowired
    private RechargeMapper rechargeMapper;


    @Override
    @Transactional
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery)  {
        String data = callbackQuery.getData();
        String amount = data.substring(data.lastIndexOf(":") + 1);
       amount =  amount.replace("U","");

        List<Recharge> rechargeList = rechargeMapper.selectWithinTenMinutesRechargeList();
        BigDecimal bigDecimal = new BigDecimal(amount);
        if(rechargeList.isEmpty()){
            bigDecimal = bigDecimal.add(BigDecimal.valueOf(0.01));
        }else {
            //list长度*0.01
            bigDecimal = bigDecimal.add(BigDecimal.valueOf((rechargeList.size() +1) * 0.01));
        }

        Recharge recharge = new Recharge();
        recharge.setTgId(callbackQuery.getFrom().getId());
        recharge.setMoney(bigDecimal);
        rechargeMapper.insert(recharge);

        EditMessageText messageText = EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text("此订单10分钟内有效，过期后请重新生成订单。\n"+
                        "转账地址: <code>TD8xFqeF5ni5PDTwmdwJUE8h3cqgbfMMMM</code> (TRC-20网络)\n" +
                        "转账金额: " + bigDecimal + " USDT\n" +
                        "请注意转账金额务必与上方的转账金额一致，否则无法自动到账\n" +
                        "支付完成后, 请等待1分钟左右查询，自动到账。")
                .parseMode("html")
                .build();

        try {
            bot.execute(messageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


    }
}
