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
        int i = 1 ;
        String code = "\uD83E\uDDE7";
        if(lotteryInfoList.isEmpty()){
            str.append("no one's involved yet\n");
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
                String t = code+"\t₦" +lotteryInfo.getMoney()+ "\t<b>" + lotteryInfo.getTgName() + "</b>\n";
                str.append(t);
                i++ ;
            }
        }
        return "Thank you all for your enthusiastic participation in this event! After an exciting draw, we are pleased to announce the list of lucky winners below. Winners, please take note:\n" +
                "\n" +
                "Contact our online customer service within the designated time to claim your prize.\n" +
                "Provide the required verification, and your winnings will be credited to your account promptly.\n" +
                "If you have any questions, feel free to reach out to our customer service team.\n" +
                "Once again, thank you for your participation! We will be hosting more exciting events in the future, and we look forward to your continued support and involvement!\n" +
                "\n" +
                "\n" +
                "\n" +
                "\uD83C\uDF81"+lottery.getLotteryId()+"\uD83C\uDF81\n" +
                "\n" +
                "\uD83D\uDCCCWinning user:\n" +
                "description:rankings money username\n" +
                "\n" +
                str;
    }

}
