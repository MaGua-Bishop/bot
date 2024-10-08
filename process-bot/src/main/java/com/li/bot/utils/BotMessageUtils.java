package com.li.bot.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.vo.OrderAndBusinessVo;
import com.li.bot.entity.database.vo.UserAndOrderVo;
import com.li.bot.enums.OrderStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BotMessageUtils {


    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private static String formatDateTime(LocalDateTime dateTime){
        return formatter.format(dateTime);
    }


    private static String getUserOrderList(List<UserAndOrderVo> list){
        String text = "";
        if(!list.isEmpty()){
            for (UserAndOrderVo userAndOrderVo : list) {
                text +=  userAndOrderVo.getBusinessName()+"\t"+userAndOrderVo.getBusinessMoney()+"\t"+ OrderStatus.getMessageByCode(userAndOrderVo.getOrderStatus()) +"\t"+formatDateTime(userAndOrderVo.getCreateTime())+"\n";
            }
        }
        return text;
    }

    private static String getUserReplyList(List<Reply> list){
        String text = "";
        if(!list.isEmpty()){
            for (Reply vo : list) {
                text +=  vo.getOrderId()+"\t"+formatDateTime(vo.getCreateTime())+"\n";
            }
        }
        return text;
    }

    private static String getOrderInfoList(IPage<OrderAndBusinessVo> page){
        String text = "";
        List<OrderAndBusinessVo> list = page.getRecords();
        int index = 1 ;
        if(!list.isEmpty()){
            for (OrderAndBusinessVo vo : list) {
                text +=index+"\t"+vo.getBusinessMoney()+"\t"+OrderStatus.getMessageByCode(vo.getOrderStatus()) +"\n";
                index++ ;
            }
        }
        return text;
    }

    private static String getPage(IPage<OrderAndBusinessVo> page){
        String text = "第"+page.getCurrent()+"页/共"+page.getPages()+"页";
        return text;
    }




    public static String getUserInfoMessage(User user, List<UserAndOrderVo> userOrderList, List<UserAndOrderVo> surplusOrder, List<UserAndOrderVo> replyOrderList) {
        String userOrderListText = getUserOrderList(userOrderList);
        String surplusOrderText = getUserOrderList(surplusOrder);
        String replyOrderListText = getUserOrderList(replyOrderList);
        return "```用户信息\n" +
                "TGID："+user.getTgId()+"\n" +
                "用户名："+user.getTgName()+"\n" +
                "用户余额："+user.getMoney()+"\n" +
                "用户报单：\n" +
                "业务类型 金额 业务状态 发单时间\n" +
                userOrderListText +
                "\n" +
                "用户回单：\n" +
                "业务类型 金额 业务状态 发单时间\n" +
                replyOrderListText +
                "\n" +
                "剩余未回单：\n" +
                "业务类型 金额 业务状态 发单时间\n" +
                surplusOrderText+
                "```";
    }

    //       "序号  订单编号  业务价格 业务状态\n" +
    public static String getOrderInfoMessage(String businessName , IPage<OrderAndBusinessVo> page) {
        return businessName+"待处理订单信息\n" +
                "序号 价格 订单状态\n" +
                getOrderInfoList(page) +
                "\n" +
                getPage(page) ;
    }

    public static String getUserReplyMessage(String  type, Reply reply) {
        return type+"订单信息\n" +
                "接单时间:"+formatDateTime(reply.getCreateTime())+"\n"+
                " 订单id:\n" +
                "<code>"+reply.getOrderId()+"</code>";
    }

}
