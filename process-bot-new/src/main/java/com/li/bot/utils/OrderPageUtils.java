package com.li.bot.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.vo.OrderAndBusinessVo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-02
 */
public class OrderPageUtils {

    public  static final Long PAGESIZE = 10L;


    public static InlineKeyboardMarkup createInlineKeyboardButton(IPage<OrderAndBusinessVo> page) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        // 假设每个 OrderAndBusinessVo 有一个 orderId
        int index = 1 ;
        for (OrderAndBusinessVo vo : page.getRecords()) {
            buttonList.add(InlineKeyboardButton.builder()
                    .text(String.valueOf(index))
                    .callbackData("group:select:order:" + vo.getOrderId())
                    .build());
            index ++ ;
        }

        // 添加分页按钮
        Long businessId = page.getRecords().get(0).getBusinessId();
        if (page.getCurrent() == 1 && page.getCurrent() < page.getPages()) { // 第一页
            buttonList.add(InlineKeyboardButton.builder()
                    .text("下一页")
                    .callbackData("next:select:order:" + (page.getCurrent() + 1)+":businessId:"+businessId)
                    .build());
        } else if (page.getCurrent() < page.getPages()) { // 中间页
            buttonList.add(InlineKeyboardButton.builder()
                    .text("上一页")
                    .callbackData("prev:select:order:" + (page.getCurrent() - 1)+":businessId:"+businessId)
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("下一页")
                    .callbackData("next:select:order:" + (page.getCurrent() + 1)+":businessId:"+businessId)
                    .build());
        } else if(page.getPages() > 1){ // 最后一页
            buttonList.add(InlineKeyboardButton.builder()
                    .text("上一页")
                    .callbackData("prev:select:order:" + (page.getCurrent() - 1)+":businessId:"+businessId)
                    .build());
        }

        List<List<InlineKeyboardButton>> list = Lists.partition(buttonList, 5);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(list).build();


        return inlineKeyboardMarkup;
    }




}
