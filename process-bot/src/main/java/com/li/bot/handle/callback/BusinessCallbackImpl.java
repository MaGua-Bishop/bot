package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.AddOrderSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
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
public class BusinessCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "allBusiness";
    }

    @Autowired
    private BusinessMapper businessMapper;
    @Autowired
    private AddOrderSessionList addOrderSessionList ;

    @Autowired
    private UserMapper userMapper ;

    private Business getBusinessInfo(Long businessId) {
        LambdaQueryWrapper<Business> wrapper = new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, businessId);
        return businessMapper.selectOne(wrapper);
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(Long businessId,Long tgId) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("下一步").callbackData("nextstep").build());

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));
        if (user != null && user.getIsAdmin()) {
            buttonList.add(InlineKeyboardButton.builder().text("删除该业务").callbackData("adminDeleteBusiness:"+businessId).build());
        }


        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);


        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();

        return inlineKeyboardMarkup;
    }

    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {

//        bot.execute(DeleteMessage.builder()
//                .chatId(callbackQuery.getMessage().getChatId())
//                .messageId(callbackQuery.getMessage().getMessageId())
//                .build());

        //解析业务id
        String data = callbackQuery.getData();
        Long businessId = Long.parseLong(data.substring(data.lastIndexOf(":") + 1));


        //获取业务信息
        Business businessInfo = getBusinessInfo(businessId);

        CopyMessage copyMessage = new CopyMessage();
        copyMessage.setChatId(callbackQuery.getFrom().getId());
        copyMessage.setMessageId(businessInfo.getMessageId());
        copyMessage.setFromChatId(businessInfo.getTgId());
        copyMessage.setReplyMarkup(createInlineKeyboardButton(businessId,callbackQuery.getFrom().getId()));

//        String text = "```" + businessInfo.getName() + "业务信息\n" +
//                "业务描述：" + businessInfo.getDescription() + "\n" +
//                "业务价格：" + businessInfo.getMoney() + "\n" +
//                "```";
        bot.execute(copyMessage);
//        bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(text).parseMode("MarkdownV2").build());
        addOrderSessionList.addUserSession(callbackQuery.getFrom().getId(),businessInfo);


    }

}
