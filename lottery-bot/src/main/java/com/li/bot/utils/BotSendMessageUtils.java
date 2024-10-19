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
        return "\uD83C\uDF89 The 77NG Lottery Has Begun! \uD83C\uDF89\n" +
                "Click below to enter the draw and claim your cash prize! \uD83D\uDCB0\n" +
                "\n" +
                "Don’t miss out—join now for your chance to win big!\n" +
                "\n" +
                "\uD83D\uDD14 Follow the Official 77NG Channel for Exclusive Rewards!\n" +
                "Stay tuned for random giveaways and special bonuses—don’t miss out on these exciting perks! \uD83C\uDF81\n"+
                "<code>"+uuid+"</code>";
    }

    public static String adminQueryMessage(Lottery lottery, List<LotteryInfo> lotteryInfoList,BigDecimal bigDecimal,BotServiceImpl bot){
        StringBuilder str = new StringBuilder();
        str.append("description:rankings\tmoney\tusername\n\n");
        int i = 1 ;
        String code = "\uD83E\uDDE7";
        if(lotteryInfoList.isEmpty()){
            str.append("nobody\n");
        }else {
            for (LotteryInfo lotteryInfo : lotteryInfoList) {
                if(i == 1){
                    code = "🥇";
                }else if(i == 2){
                    code = "🥈";
                }else if(i == 3){
                    code = "🥉";
                }else {
                    code = "\uD83E\uDDE7";
                }
                String url = "<a href=\"tg://user?id="+lotteryInfo.getTgId()+"\">"+lotteryInfo.getTgName()+"</a>" ;
                String t = code+"\t" +lotteryInfo.getMoney()+ "\t<b>" + url + "</b>\n";
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
