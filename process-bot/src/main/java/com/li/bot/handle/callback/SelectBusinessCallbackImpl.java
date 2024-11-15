package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.li.bot.entity.database.vo.OrderAndBusinessVo;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import com.li.bot.utils.OrderPageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class SelectBusinessCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "selectBusiness";
    }


    @Autowired
    private OrderMapper orderMapper ;


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery){


        //解析业务id
        String data = callbackQuery.getData();
        Long businessId = Long.parseLong(data.substring(data.lastIndexOf(":") + 1));


        //获取该业务所有待完成的订单
        //分页
        Page<OrderAndBusinessVo> page = new Page<>(1, OrderPageUtils.PAGESIZE);
        IPage<OrderAndBusinessVo> result = orderMapper.getOrderByBusinessIdAndStatus(page, businessId, OrderStatus.PENDING.getCode());

        //发送消息给群聊用户-4576426080

        Long chatId = callbackQuery.getMessage().getChatId() ;
        String userName = callbackQuery.getFrom().getLastName() + callbackQuery.getFrom().getFirstName();
        if(result.getRecords().isEmpty()){
            String a = "<a href=\"tg://user?id=" + chatId + "\">@" + userName + "</a>";
            SendMessage msg = SendMessage.builder().chatId(chatId).text(a+
                    "该业务类型订单已全部完成").parseMode("html").build();
            try {
                bot.execute(msg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        String businessName = result.getRecords().get(0).getBusinessName();
        SendMessage msg = SendMessage.builder().chatId(chatId).text(BotMessageUtils.getOrderInfoMessage(businessName,result)).replyMarkup(OrderPageUtils.createInlineKeyboardButton(result)).build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

}
