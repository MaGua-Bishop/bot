package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.OrderVo;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.entity.database.User;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.*;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.AddBusinessSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class AdminSelectListCallbackImpl implements ICallback{

    @Override
    public String getCallbackName() {
        return "adminSelectListBusiness";
    }


    @Autowired
    private OrderMapper orderMapper ;

    @Autowired
    private BusinessMapper businessMapper ;

    @Autowired
    private ReplyMapper replyMapper;

    private String getChatInfo(long userId, BotServiceImpl bot) {
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

    private String getOrderInfo(String orderId,Long name,BotServiceImpl bot){
        Reply reply = replyMapper.selectOne(new LambdaQueryWrapper<Reply>().eq(Reply::getOrderId,orderId));
        Business business = businessMapper.selectOne(new LambdaQueryWrapper<Business>().eq(Business::getBusinessId, name));
        String url = "<a href=\"tg://user?id="+reply.getTgId()+"\">"+getChatInfo(reply.getTgId(),bot)+"</a>" ;
        return url + " "+ business.getName() ;
    }





    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {

        String data = callbackQuery.getData();
        Long businessId = Long.parseLong(data.substring(data.lastIndexOf(":") + 1));

        String t = "" ;
        Integer status = 0 ;
        if(businessId == 0){
            t = "未领取订单" ;
            status = OrderStatus.PENDING.getCode();
        }else {
            t = "未回复订单" ;
            status = OrderStatus.IN_PROGRESS.getCode();
        }


        List<Order> orderList = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getStatus, status));
        if(orderList.isEmpty()){
            SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text("没有"+t).build();
            bot.execute(sendMessage);
            return;
        }

        // 获取所有业务ID
        Set<Long> businessIdList = orderList.stream()
                .map(Order::getBusinessId)
                .collect(Collectors.toSet());

        // 获取所有业务信息
        List<Business> businessList = businessMapper.selectAllBusiness(new ArrayList<>(businessIdList));

        // 创建一个 Map 用于存储业务 ID 和名称的映射
        Map<Long, String> businessIdToNameMap = businessList.stream()
                .collect(Collectors.toMap(Business::getBusinessId, Business::getName));

        // 根据业务名称对订单进行分组，并统计每个业务的订单数量
        Map<String, Integer> businessOrderCountMap = orderList.stream()
                .collect(Collectors.groupingBy(order -> businessIdToNameMap.get(order.getBusinessId()), Collectors.summingInt(o -> 1)));

        // 将结果转换为 List<OrderVo>
       if(businessId == 0){
           List<OrderVo> orderVos = businessOrderCountMap.entrySet().stream()
                   .map(entry -> new OrderVo(entry.getKey(), entry.getValue()))
                   .collect(Collectors.toList());

           // 打印结果
           StringBuilder text = new StringBuilder();
           text.append("未领取订单列表:\n");
           text.append("-----------------------------------\n");
           text.append("未领取订单总数: "+ orderList.size()+"\n");
           for (OrderVo orderVo : orderVos) {
               text.append(orderVo.getName()).append(": ").append(orderVo.getNumber()).append("个\n");
           }
           SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text(String.valueOf(text)).build();
           bot.execute(sendMessage);
       }else {
           // 打印结果
           StringBuilder text = new StringBuilder();
           text.append("未回复订单列表:\n");
           text.append("-----------------------------------\n");
           text.append("未回复订单总数: "+ orderList.size()+"\n");
           orderList.forEach(order -> {
               String orderInfo = getOrderInfo(order.getOrderId(), order.getBusinessId(),bot);
               text.append(orderInfo+"\n");
           });
           SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text(String.valueOf(text)).parseMode("html").build();
           bot.execute(sendMessage);
       }









    }

}
