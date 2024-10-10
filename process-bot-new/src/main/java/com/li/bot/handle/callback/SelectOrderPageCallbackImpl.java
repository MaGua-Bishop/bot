package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.li.bot.entity.database.vo.OrderAndBusinessVo;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import com.li.bot.utils.OrderPageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
@Slf4j
public class SelectOrderPageCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "page:select:order";
    }

    @Autowired
    private OrderMapper orderMapper ;




    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();

        Pattern pattern = null ;
        if(data.indexOf("next:select:order:") == 0){
            pattern = Pattern.compile("next:select:order:(\\d+):businessId:(\\d+)");
        }else if(data.indexOf("prev:select:order:") == 0){
            pattern = Pattern.compile("prev:select:order:(\\d+):businessId:(\\d+)");
        }
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            int pageCount = Integer.parseInt(matcher.group(1));
            Long businessId = Long.parseLong(matcher.group(2));
            Page<OrderAndBusinessVo> page = new Page<>(pageCount, OrderPageUtils.PAGESIZE);
            IPage<OrderAndBusinessVo> pageData = orderMapper.getOrderByBusinessIdAndStatus(page, businessId, OrderStatus.PENDING.getCode());
            if(!pageData.getRecords().isEmpty()){
                try {
                    bot.execute(EditMessageText.builder()
                            .chatId(callbackQuery.getMessage().getChatId())
                            .messageId(callbackQuery.getMessage().getMessageId())
                            .text(BotMessageUtils.getOrderInfoMessage(pageData.getRecords().get(0).getBusinessName(), pageData))
                            .replyMarkup(OrderPageUtils.createInlineKeyboardButton(pageData))
                            .build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }





    }
}
