package com.li.bot.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.vo.OrderAndBusinessVo;
import com.li.bot.entity.database.vo.UserAndOrderVo;
import com.li.bot.enums.OrderStatus;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class BotMessageUtils {


    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    private static String formatDateTime(LocalDateTime dateTime){
        return formatter.format(dateTime);
    }

    private static String formatDate(Date dateTime){
        String nowTime = format.format(dateTime);
        return nowTime;
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




    public static String getUserInfoMessage(User user) {
        return "```用户信息\n" +
                "TGID："+user.getTgId()+"\n" +
                "用户名："+user.getTgName()+"\n" +
                "用户余额："+user.getMoney()+"\n" +
                "```\n点击下方按钮查看报单状态";
    }

    //       "序号  订单编号  业务价格 业务状态\n" +
    public static String getOrderInfoMessage(String businessName , IPage<OrderAndBusinessVo> page) {
        return businessName+"待处理订单信息\n" +
                "序号 价格 订单状态\n" +
                getOrderInfoList(page) +
                "\n" +
                getPage(page) ;
    }

    public static String getUserReplyMessage(Reply reply,String text,String name) {
        return name+"\n" +
                "<strong>订单内容:</strong>\n"+text+"\n" +
                "<strong>接单时间:</strong>"+formatDateTime(reply.getCreateTime())+"\n"+
                " 订单id:\n" +
                "<code>"+reply.getOrderId()+"</code>";
    }

    public static String getOrderInfoMessage(Date dateTime, String text, String name, String orderId) {
        return name+"\n" +
                "<strong>订单内容:</strong>\n"+text+"\n" +
                "<strong>报单时间:</strong>"+formatDate(dateTime)+"\n"+
                " 订单id:\n" +
                "<code>"+orderId+"</code>";
    }

    private static String removeEmpty(String text){
        return text.replaceAll("null","");
    }

    public static String getAdminQueryUserInfo(Page<User> userPage){
        StringBuilder text = new StringBuilder();
        text.append("序号\tid\t用户名\t用户余额\n");
        int index = 1 ;
        for (User user : userPage.getRecords()) {
            text.append(index).append("\t").append("<code>"+user.getTgId()+"</code>").append("\t").append("<a href=\"tg://user?id="+user.getTgId()+"\">"+removeEmpty(user.getTgName())+"</a>").append("\t").append("<b>"+user.getMoney()+"</b>").append("\n").append("\n");
            index++ ;
        }
        text.append("共"+userPage.getCurrent()+"/"+userPage.getPages()+"页");
        return  String.valueOf(text);
    }

}
