package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import com.li.bot.utils.ConvoysPageUtils;
import com.li.bot.utils.UnitConversionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Component
@Slf4j
public class SelectConvoysPageCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "page:select:convoys";
    }

    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;







    @Override
    public synchronized void execute(BotServiceImpl bot, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();

        Pattern pattern = null ;
        if(data.indexOf("next:select:convoys:") == 0){
            pattern = Pattern.compile("next:select:convoys:(\\d+):convoysId:(\\d+)");
        }else if(data.indexOf("prev:select:convoys:") == 0){
            pattern = Pattern.compile("prev:select:convoys:(\\d+):convoysId:(\\d+)");
        }
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            int pageCount = Integer.parseInt(matcher.group(1));
            Page<Convoys> page = new Page<>(pageCount, ConvoysPageUtils.PAGESIZE);
            IPage<Convoys> pageData = convoysMapper.selectPage(page, null);
            List<Invite> list = convoysInviteMapper.getConvoysInviteListByConvoysIds(pageData.getRecords().stream().map(Convoys::getConvoysId).collect(Collectors.toList()));
            if(!pageData.getRecords().isEmpty()){
                try {
                    bot.execute(EditMessageText.builder()
                            .chatId(callbackQuery.getMessage().getChatId())
                            .messageId(callbackQuery.getMessage().getMessageId())
                            .text(BotMessageUtils.getConvoysHall(pageData.getRecords().size(),list.size()))
                            .replyMarkup(ConvoysPageUtils.createInlineKeyboardButton(pageData, convoysInviteMapper))
                            .build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }





    }
}
