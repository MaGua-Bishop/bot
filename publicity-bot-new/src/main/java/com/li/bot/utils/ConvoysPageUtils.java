package com.li.bot.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.mapper.ConvoysInviteMapper;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-02
 */
public class ConvoysPageUtils {

    public  static final Long PAGESIZE = 20L;


    public static InlineKeyboardMarkup createInlineKeyboardButton(IPage<Convoys> page, ConvoysInviteMapper convoysInviteMapper) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        for (Convoys vo : page.getRecords()) {
            List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, vo.getConvoysId()).eq(ConvoysInvite::getIsReview,true));
            buttonList.add(InlineKeyboardButton.builder()
                    .text(vo.getName()+"|"+convoysInviteList.size()+"-"+vo.getCapacity()+"|"+ UnitConversionUtils.toThousands(vo.getSubscription())).callbackData("selectConvoysInfo:"+vo.getConvoysId())
                    .callbackData("selectConvoysInfo:" + vo.getConvoysId())
                    .build());
        }
        // 添加分页按钮
        Long businessId = page.getRecords().get(0).getConvoysId();
        if (page.getCurrent() == 1 && page.getCurrent() < page.getPages()) { // 第一页
            buttonList.add(InlineKeyboardButton.builder()
                    .text("下一页")
                    .callbackData("next:select:convoys:" + (page.getCurrent() + 1)+":convoysId:"+businessId)
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("首页")
                    .callbackData("/start")
                    .build());
        } else if (page.getCurrent() < page.getPages()) { // 中间页
            buttonList.add(InlineKeyboardButton.builder()
                    .text("上一页")
                    .callbackData("prev:select:convoys:" + (page.getCurrent() - 1)+":convoysId:"+businessId)
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("下一页")
                    .callbackData("next:select:convoys:" + (page.getCurrent() + 1)+":convoysId:"+businessId)
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("首页")
                    .callbackData("/start")
                    .build());
        } else if(page.getPages() > 1){ // 最后一页
            buttonList.add(InlineKeyboardButton.builder()
                    .text("上一页")
                    .callbackData("prev:select:convoys:" + (page.getCurrent() - 1)+":convoysId:"+businessId)
                    .build());
            buttonList.add(InlineKeyboardButton.builder()
                    .text("首页")
                    .callbackData("/start")
                    .build());
        }

        List<List<InlineKeyboardButton>> list = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(list).build();


        return inlineKeyboardMarkup;
    }




}