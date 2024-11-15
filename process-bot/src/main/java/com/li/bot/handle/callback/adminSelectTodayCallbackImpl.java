package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.UserMoney;
import com.li.bot.entity.database.vo.OrderBusinessVo;
import com.li.bot.enums.OrderStatus;
import com.li.bot.enums.UserMoneyStatus;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.mapper.UserMoneyMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class adminSelectTodayCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminSelectToday";
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserMoneyMapper userMoneyMapper;
    @Autowired
    private OrderMapper orderMapper;


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        Integer type = Integer.parseInt(data.substring(data.lastIndexOf(":") + 1));
//     type=0 今日充值情况，type=1 今日报单情况
        StringBuilder sb = new StringBuilder();
        if (type == 0) {
            List<UserMoney> userMonies = userMoneyMapper.selectTodayUserMoney();
            sb.append("今日充值情况：\n");
            sb.append("序号\t用户id\t类型\t金额\n");
            sb.append("-----------------------------------\n");
            if (userMonies.isEmpty()) {
                sb.append("今日暂无充值记录");
            } else {
                int i = 1;
                for (UserMoney userMony : userMonies) {
                    sb.append(i + ".\t").append(userMony.getTgId() + "\t").append("<b>" + UserMoneyStatus.getMessageByCode(userMony.getType()) + "</b>\t").append("<b>" + userMony.getMoney() + "</b>\n").append("用户余额："+userMony.getAfterMoney() + "\n\n");
                    i++;
                }
            }
        } else if (type == 1) {
            sb.append("今日报单情况：\n");
            sb.append("-----------------------------------\n");
            sb.append("序号\t订单id\t报单类型\t报单价格\t报单用户id\n");
            sb.append("-----------------------------------\n");
            List<OrderBusinessVo> orderBusinessVos = orderMapper.selectTodayOrder();
            if (orderBusinessVos.isEmpty()) {
                sb.append("今日暂无报单记录");
            } else {
                int i = 1;
                for (OrderBusinessVo orderBusinessVo : orderBusinessVos) {
                    String a = "<a href=\"tg://user?id=" + orderBusinessVo.getReplyTgId() + "\">@" + orderBusinessVo.getUserName() + "</a>";
                    sb.append(i + ".\t").append("<code>" + orderBusinessVo.getOrderId() + "</code>\t").append("<b>" + orderBusinessVo.getBusinessName() + "</b>\t").append("<b>" + orderBusinessVo.getBusinessMoney() + "</b>\t").append("<b>" + orderBusinessVo.getTgId() + "</b>\n接单人id：").append(orderBusinessVo.getReplyTgId() + "\t接单人：").append(a + "\t处理状态：").append(OrderStatus.getMessageByCode(orderBusinessVo.getStatus()) + "\n\n");
                    i++;
                }
            }
        }
        bot.execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text(sb.toString()).parseMode("html").build());
    }

}
