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

    private InlineKeyboardMarkup createInlineKeyboardButton(Long businessId,User user,Business businessInfo) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        if(businessInfo.getIsShelving()){
            buttonList.add(InlineKeyboardButton.builder().text("下一步").callbackData("nextstep").build());
        }
        if (user != null && user.getIsAdmin()) {
            buttonList.add(InlineKeyboardButton.builder().text("修改业务文案").callbackData("adminEditBusiness:"+businessId).build());
            buttonList.add(InlineKeyboardButton.builder().text("修改业务价格").callbackData("adminEditPrice:"+businessId).build());
            if(businessInfo.getIsShelving()){
                buttonList.add(InlineKeyboardButton.builder().text("下架该业务(点击按钮直接下架)").callbackData("adminShelving:"+businessId).build());
            }else {
                buttonList.add(InlineKeyboardButton.builder().text("上架该业务(点击按钮直接上架)").callbackData("adminShelving:"+businessId).build());
            }
            buttonList.add(InlineKeyboardButton.builder().text("删除该业务").callbackData("adminDeleteBusiness:"+businessId).build());

        }


        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);


        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup createInlineKeyboardButton02(Long businessId) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        buttonList.add(InlineKeyboardButton.builder().text("删除该业务").callbackData("adminDeleteBusiness:"+businessId).build());
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
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, callbackQuery.getFrom().getId()));
        copyMessage.setReplyMarkup(createInlineKeyboardButton(businessId,user,businessInfo));

//        String text = "```" + businessInfo.getName() + "业务信息\n" +
//                "业务描述：" + businessInfo.getDescription() + "\n" +
//                "业务价格：" + businessInfo.getMoney() + "\n" +
//                "```";
        try{
            bot.execute(copyMessage);
        }catch (TelegramApiException e){
            if (user.getIsAdmin()){
                SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text("<b>"+businessInfo.getName()+"</b>异常,请删除重新添加").replyMarkup(createInlineKeyboardButton02(businessId)).parseMode("html").build();
                bot.execute(sendMessage);
            }
            System.out.println("异常");
            return;
        }

//        bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(text).parseMode("MarkdownV2").build());
        addOrderSessionList.addUserSession(callbackQuery.getFrom().getId(),businessInfo);


    }

}
