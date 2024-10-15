package com.li.bot.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.vo.OrderAndBusinessVo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-02
 */
public class UserInfoPageUtils {

    public  static final Long PAGESIZE = 10L;


    public static InlineKeyboardMarkup createInlineKeyboardButton(IPage<User> page) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        // 添加分页按钮
        if (page.getCurrent() == 1 && page.getCurrent() < page.getPages()) { // 第一页
            buttonList.add(InlineKeyboardButton.builder()
                    .text("下一页")
                    .callbackData("adminSelectUserInfo:" + (page.getCurrent() + 1))
                    .build());
        } else if (page.getCurrent() < page.getPages()) { // 中间页
            buttonList.add(InlineKeyboardButton.builder()
                    .text("上一页")
                    .callbackData("adminSelectUserInfo:" + (page.getCurrent() - 1))
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("下一页")
                    .callbackData("adminSelectUserInfo:" + (page.getCurrent() + 1))
                    .build());
        } else if(page.getPages() > 1){ // 最后一页
            buttonList.add(InlineKeyboardButton.builder()
                    .text("上一页")
                    .callbackData("adminSelectUserInfo:" + (page.getCurrent() - 1))
                    .build());
        }
        List<List<InlineKeyboardButton>> list = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(list).build();
        return inlineKeyboardMarkup;
    }




}
