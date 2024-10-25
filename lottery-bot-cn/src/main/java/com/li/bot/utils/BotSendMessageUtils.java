package com.li.bot.utils;

import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.LotteryInfo;
import com.li.bot.entity.database.Takeout;
import com.li.bot.entity.database.User;
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
        return "\uD83C\uDF89 茶社抽奖开始了！ \uD83C\uDF89\n" +
                "点击下面的链接参与抽奖并领取现金奖！\uD83D\uDCB0\n" +
                "\n\uD83C\uDF81<b>积分:" +amount+"</b>\uD83C\uDF81\n\n" +
                "不要错过机会，现在就加入，赢取大奖！\n" +
                "\n" +
                "\uD83D\uDD14 关注茶社频道获取独家奖励!\n" +
                "敬请关注随机赠品和特殊奖金-不要错过这些令人兴奋的福利! \uD83C\uDF81\n"+
                "<code>"+uuid+"</code>";
    }

    public static String adminQueryMessage(Lottery lottery, List<LotteryInfo> lotteryInfoList,BigDecimal bigDecimal,BotServiceImpl bot){
        StringBuilder str = new StringBuilder();
        int i = 1 ;
        String code = "\uD83E\uDDE7";
        if(lotteryInfoList.isEmpty()){
            str.append("还没有人参与\n");
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
                String t = code+"\t" +lotteryInfo.getMoney()+ "\t<b>" + lotteryInfo.getTgName() + "</b>\n";
                str.append(t);
                i++ ;
            }
        }
        return "感谢大家对本次活动的热情参与!在激动人心的抽奖之后，我们很高兴地宣布下面的幸运获奖者名单。获奖者，请注意:\n" +
                "\n" +
                "在指定时间内联系<a href=\"tg://user?id="+lottery.getTgId()+"\">@"+lottery.getTgName()+"</a>领取您的奖品。\n" +
                "提供所需的验证(中奖id)，您的奖金将立即记入您的帐户。\n" +
                "如果您有任何问题，请随时联系我们的客户服务团队。\n" +
                "再次感谢您的参与!我们将在未来举办更精彩的活动，我们期待您的继续支持和参与!\n" +
                "\n" +
                "\n" +
                "\uD83C\uDF81"+lottery.getLotteryId()+"\uD83C\uDF81\n" +
                "\n" +
                "\uD83D\uDCCC获奖用户:\n" +
                "描述: 排名|积分|用户名\n" +
                "\n" +
                str;
    }

    public static String getAdminReviewMessage(User user , Takeout takeout){
        String t = "";
        if(takeout.getStatus() == 0){
            t = "待审核";
        }else if(takeout.getStatus() == 1){
            t = "\uD83D\uDFE2已同意";
        }else if(takeout.getStatus() == -1){
            t = "\uD83D\uDD34已拒绝";
        }
        return "用户名:<a href=\"tg://user?id="+user.getTgId()+"\">"+user.getTgName()+"</a>\nID:<code>"+user.getTgId()+"</code>\n"
                +"\n用户提现积分:<b>"+takeout.getMoney()+"</b>\n提现状态:<b>"+t+"</b>\n";
    }

}
