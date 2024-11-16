package com.li.bot.handle;

import com.li.bot.handle.callback.CallbackFactory;
import com.li.bot.handle.callback.ICallback;
import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Objects;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
public class CallbackQueryHandle {

    private CallbackQuery callbackQuery;

    private BotServiceImpl bot;

    private CallbackFactory callbackFactory;

    public CallbackQueryHandle(BotServiceImpl bot, CallbackQuery callbackQuery, CallbackFactory callbackFactory) {
        this.callbackQuery = callbackQuery;
        this.bot = bot;
        this.callbackFactory = callbackFactory;
    }

    public void executeCallbackQuery() throws TelegramApiException {
        String data = callbackQuery.getData();
        System.out.println("按钮参数：" + data);
        ICallback callback = callbackFactory.getCallback(data);
        if (Objects.nonNull(callback)) {
            callback.execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminShelving:") == 0) {
            callbackFactory.getCallback("adminShelving").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminEditBusiness:") == 0) {
            callbackFactory.getCallback("adminEditBusiness").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminEditPrice:") == 0) {
            callbackFactory.getCallback("adminEditPrice").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminEditType:") == 0) {
            callbackFactory.getCallback("adminEditType").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("user:recharge:") == 0) {
            callbackFactory.getCallback("userRecharge").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("user:code:recharge") == 0) {
            callbackFactory.getCallback("userCodeRecharge").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("user:cancel:") == 0) {
            callbackFactory.getCallback("userCancel").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("receive:order:") == 0) {
            callbackFactory.getCallback("receiveOrder").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("list:order:receive:") == 0) {
            callbackFactory.getCallback("receiveOrderList").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("waiver:order:") == 0) {
            callbackFactory.getCallback("waiverOrder").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("admin:cancel:order:") == 0) {
            callbackFactory.getCallback("adminCancelOrder").execute(bot, callbackQuery);
            return;
        }

        if (data.indexOf("select:businessId:") == 0) {
            callbackFactory.getCallback("selectBusiness").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("businessId:") == 0) {
            callbackFactory.getCallback("allBusiness").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("user:send:businessId:") == 0) {
            callbackFactory.getCallback("sendOrder").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("order:yes:") == 0) {
            callbackFactory.getCallback("order:yes").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("order:no:") == 0) {
            callbackFactory.getCallback("order:no").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("send:order:") == 0) {
            callbackFactory.getCallback("send:order").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("next:select:order:") == 0 || data.indexOf("prev:select:order:") == 0) {
            callbackFactory.getCallback("page:select:order").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("group:select:order:") == 0) {
            callbackFactory.getCallback("group:select:order").execute(bot, callbackQuery);
            return;
        }
        //select:order:records:
        if (data.indexOf("select:reply:records:") == 0) {
            callbackFactory.getCallback("select:reply:records").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("select:reply:record:") == 0) {
            callbackFactory.getCallback("select:reply:record").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("select:business:type:") == 0) {
            callbackFactory.getCallback("select:business:type").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("business:type:") == 0) {
            callbackFactory.getCallback("business:type").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminDeleteBusiness:") == 0) {
            callbackFactory.getCallback("adminDeleteBusiness").execute(bot, callbackQuery);
            return;
        }
        if (data.startsWith("adminSelectListBusiness:")) {
            callbackFactory.getCallback("adminSelectListBusiness").execute(bot, callbackQuery);
            return;
        }
        if (data.startsWith("adminSelectUserInfo:")) {
            callbackFactory.getCallback("adminSelectUserInfo").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminSelectBilling:") == 0) {
            callbackFactory.getCallback("adminSelectBilling").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminSelectToday:") == 0) {
            callbackFactory.getCallback("adminSelectToday").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminUpdateCode") == 0) {
            callbackFactory.getCallback("adminUpdateCode").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminDeleteCode") == 0) {
            callbackFactory.getCallback("adminDeleteCode").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("adminUpdateRechargeCopy") == 0) {
            callbackFactory.getCallback("adminUpdateRechargeCopy").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("admin:confirm:money:") == 0) {
            callbackFactory.getCallback("adminConfirmMoney").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("admin:cancel:money:") == 0) {
            callbackFactory.getCallback("adminCancelMoney").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("user:select:type:") == 0) {
            callbackFactory.getCallback("userRechargeType").execute(bot, callbackQuery);
            return;
        }
        if (data.indexOf("admin:deletecode:") == 0) {
            callbackFactory.getCallback("admindeletecode").execute(bot, callbackQuery);
            return;
        }


//        bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("未找到对应按钮回调").build());


    }

}
