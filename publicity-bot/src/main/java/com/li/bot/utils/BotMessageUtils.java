package com.li.bot.utils;

import com.li.bot.entity.database.Invite;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
public class BotMessageUtils {

    public static String getStartMessage(Long tgId, String userName, String botName) {
        String name = botName.replace("t.me/","");
        return "<b>\uD83D\uDE80Telegram互推: </b>" + "<a href=\"https://" + botName + "\">@" + name + "</a>\n" +
                "\n" +
                "<b>UID " + tgId + "</b> 尊敬的 " + userName + " 你好:\n" +
                "\n" +
                "<b>自动互推机器人</b>\n" +
                "\n" +
                "<b>使用说明：</b>请将本机器人拉入你的群组|频道，并赋予管理员权限🔶\n" +
                "<a href=\"https://" + botName + "?startgroup=true\">机器人进群组</a>\n" +
                "<a href=\"https://" + botName + "?startchannel=true\">机器人进频道</a>\n" +
                "\n" +
                "<b>管理权限：</b>发布消息/编辑其他人的消息/删除其他人的消息/邀请其他人权限，缺失权限机器人不能正常工作\n" +
                "\n" +
                "👇点击添加机器人到群组/频道请点击机器人标题进详情页添加";
    }

    public static String getConvoysMemberList(List<Invite> list){
        StringBuilder text = new StringBuilder();
       if(list.isEmpty()){
           text.append("该车队暂无成员,快来加入吧!!!");
       }else {
           AtomicInteger i = new AtomicInteger(1);
           list.forEach(item->{
               text.append(i.get()).append(". ").append("<a href=\"").append(item.getLink()).append("\">")
                      .append(item.getName()).append("</a>\n");
               i.getAndIncrement();
           });
       }
       return text.toString();
    }


    public static String getConvoysMemberInfoList(List<Invite> list){
        StringBuilder text = new StringBuilder();
        AtomicInteger i = new AtomicInteger(1);
        list.forEach(item->{
            text.append(i.get()).append(". ").append("<a href=\"").append(item.getLink()).append("\">")
                    .append(item.getName()).append("</a>\n");
            i.getAndIncrement();
        });
        return text.toString();
    }




}
