package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInfoListVo;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import com.li.bot.utils.ConvoysPageUtils;
import com.li.bot.utils.UnitConversionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class ReturnConvoysListCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "returnConvoysList";
    }

    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;



    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        Page<Convoys> page = new Page<>(1, ConvoysPageUtils.PAGESIZE);
        IPage<ConvoysInfoListVo> convoysList = convoysMapper.selectConvoysList(page);
        Long number = convoysList.getRecords().stream().map(ConvoysInfoListVo::getCurrentCapacity).reduce(Long::sum).get();

        EditMessageText sendMessage = EditMessageText.builder().chatId(callbackQuery.getMessage().getChatId()).text(BotMessageUtils.getConvoysHall(convoysList.getRecords().size(),number)).replyMarkup(ConvoysPageUtils.createInlineKeyboardButton(convoysList)).messageId(callbackQuery.getMessage().getMessageId())
                .parseMode("html").build();
        bot.execute(sendMessage);
    }
}
