package com.li.bot.utils;

import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.LotteryInfo;
import com.li.bot.service.impl.BotServiceImpl;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ZERO;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
public class BotSendMessageUtils {

    public static String createLotteryMessage(BigDecimal amount,Integer number,String uuid){
        return "\uD83C\uDF81Start Lottery\uD83C\uDF81\n" +
                "\n\uD83C\uDF81<b>Amount:"+ amount + "</b>\n" +
                "\uD83C\uDF81<b>Number:"+ number + "</b>\n\n"+
                "\uD83D\uDCCC<b>Lottery ID:</b>\n" +
                "<code>"+uuid+"</code>\n" +
                "\uD83D\uDC47Click button Lottery";
    }

    private static String getChatInfo(long userId, BotServiceImpl bot) {
        GetChat getChat = new GetChat();
        getChat.setChatId(String.valueOf(userId));

        try {
            Chat execute = bot.execute(getChat);
            return execute.getUserName();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String adminQueryMessage(Lottery lottery, List<LotteryInfo> lotteryInfoList,BigDecimal bigDecimal,BotServiceImpl bot){
        StringBuilder str = new StringBuilder();
        str.append("description:winningnumber\tuserid\tusername\tmoney\tstate\n\n");
        int i = 1 ;
        if(lotteryInfoList.isEmpty()){
            str.append("nobody\n");
        }else {
            for (LotteryInfo lotteryInfo : lotteryInfoList) {
                String url = "<a href=\"tg://user?id="+lotteryInfo.getTgId()+"\">"+getChatInfo(lotteryInfo.getTgId(),bot)+"</a>" ;
                String string = lotteryInfo.getStatus() == 0 ? "no" : "yes";
                String t = i+".<code>"+lotteryInfo.getLotteryInfoId() + "</code>\t<b>" + lotteryInfo.getTgId() + "</b>\t" +url+ "\t<b>" + lotteryInfo.getMoney() + "</b>\t<b>"+string+"</b>\n\n";
                str.append(t);
                i++ ;
            }
        }
        String money = "";
        if(bigDecimal.compareTo(ZERO)==0){
            money = "Draw Ended";
        }else {
            money =  "remaining:<b>"+bigDecimal+"</b>";
        }

        return "\uD83C\uDF81"+lottery.getLotteryId()+"\uD83C\uDF81\n" +
                "\n\uD83C\uDF81<b>Amount:"+ lottery.getMoney() + "</b>\n" +
                "\uD83C\uDF81<b>Number:"+ lottery.getNumber() + "</b>\n\n"+
                "\uD83D\uDCCC<b>Winning user:</b>\n"+
                str+"\n"+money;

    }

}
