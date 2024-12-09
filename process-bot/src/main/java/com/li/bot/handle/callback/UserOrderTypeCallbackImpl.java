package com.li.bot.handle.callback;

import com.google.common.collect.Lists;
import com.li.bot.entity.Code;
import com.li.bot.entity.Files;
import com.li.bot.entity.database.vo.UserAndOrderVo;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class UserOrderTypeCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "userOrderType";
    }

    @Autowired
    private OrderMapper orderMapper;

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private static String formatDateTime(LocalDateTime dateTime) {
        return formatter.format(dateTime);
    }


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        Integer type = Integer.parseInt(data.substring(data.lastIndexOf(":") + 1));

        Long tgId = callbackQuery.getFrom().getId();
        StringBuilder str = new StringBuilder();

        if (type == 0) {
            //用户报单
            List<UserAndOrderVo> userOrderList = orderMapper.getUserAndOrderVoByTgId(tgId);
            if (!userOrderList.isEmpty()) {
                str.append("用户报单\n");
                str.append("业务类型\t金额\t业务状态\t发单时间\n\n");
                for (UserAndOrderVo userAndOrderVo : userOrderList) {
                    str.append(userAndOrderVo.getBusinessName() + "\t" + userAndOrderVo.getBusinessMoney() + "\t" + OrderStatus.getMessageByCode(userAndOrderVo.getOrderStatus()) + "\t" + formatDateTime(userAndOrderVo.getCreateTime()) + "\n\n");
                }
            } else {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId().toString()).text("您暂无报单").build());
            }
        } else if (type == 1) {
            //用户回单
            List<UserAndOrderVo> userOrderList = orderMapper.getUserAndOrderVoByTgId(tgId);
            if (!userOrderList.isEmpty()) {
                str.append("用户回单\n");
                str.append("业务类型\t金额\t业务状态\t发单时间\n\n");
                for (UserAndOrderVo userAndOrderVo : userOrderList) {
                    Integer status = userAndOrderVo.getOrderStatus();
                    if (status == OrderStatus.COMPLETED.getCode()) {
                        str.append(userAndOrderVo.getBusinessName() + "\t" + userAndOrderVo.getBusinessMoney() + "\t" + OrderStatus.getMessageByCode(userAndOrderVo.getOrderStatus()) + "\t" + formatDateTime(userAndOrderVo.getCreateTime()) + "\n\n");
                    }
                }
            } else {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId().toString()).text("您暂无回单").build());
            }
        } else if (type == 2) {
            //剩余未回单
            List<UserAndOrderVo> userOrderList = orderMapper.getUserAndOrderVoByTgId(tgId);
            if (!userOrderList.isEmpty()) {
                str.append("剩余未回单\n");
                str.append("业务类型\t金额\t业务状态\t发单时间\n\n");
                for (UserAndOrderVo userAndOrderVo : userOrderList) {
                    Integer status = userAndOrderVo.getOrderStatus();
                    if (status == OrderStatus.IN_PROGRESS.getCode() || status == OrderStatus.PENDING.getCode() || status == OrderStatus.REVIEW.getCode()) {
                        str.append(userAndOrderVo.getBusinessName() + "\t" + userAndOrderVo.getBusinessMoney() + "\t" + OrderStatus.getMessageByCode(userAndOrderVo.getOrderStatus()) + "\t" + formatDateTime(userAndOrderVo.getCreateTime()) + "\n\n");
                    }
                }
            } else {
                bot.execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId().toString()).text("您暂无未回单").build());
            }
        }
        bot.execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text(str.toString()).parseMode("html").build());
    }

}
