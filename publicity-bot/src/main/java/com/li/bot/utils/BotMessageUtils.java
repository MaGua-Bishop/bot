package com.li.bot.utils;

import com.li.bot.entity.database.Invite;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
public class BotMessageUtils {

    public static String getStartMessage(Long tgId,String userName){
        return "用户id:"+tgId+"用户名:"+userName+"\n\n"+
                "使用说明：请将本机器人拉入你的频道或点击下方邀请到频道按钮，并赋予管理员权限\n\n"+
                "管理权限：发布消息/编辑其他人的消息/删除其他人的消息/邀请其他人权限，缺失权限机器人不能正常工作";

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
