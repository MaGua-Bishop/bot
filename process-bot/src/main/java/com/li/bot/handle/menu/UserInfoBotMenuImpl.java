package com.li.bot.handle.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.vo.UserAndOrderVo;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.utils.BotMessageUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-09-29
 * @Description: 用户信息菜单实现类
 */
@Component
public class UserInfoBotMenuImpl implements IBotMenu {

    @Override
    public String getMenuName() {
        return "用户信息";
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 根据订单状态分组
     *
     * @param userOrderList
     * @return
     */
    public Map<Integer, List<UserAndOrderVo>> groupOrdersByStatus(List<UserAndOrderVo> userOrderList) {
        return userOrderList.stream()
                .collect(Collectors.groupingBy(UserAndOrderVo::getOrderStatus));
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(User user) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        if (user.getIsAdmin()) {
            buttonList.add(InlineKeyboardButton.builder().text("查看用户信息").callbackData("adminSelectUserInfo:" + 1).build());
            buttonList.add(InlineKeyboardButton.builder().text("查看今日账单").callbackData("adminSelectBilling").build());
            buttonList.add(InlineKeyboardButton.builder().text("导出消费记录").callbackData("adminExport").build());
        }
        buttonList.add(InlineKeyboardButton.builder().text("查看报单").callbackData("userOrderType:0").build());
        buttonList.add(InlineKeyboardButton.builder().text("查看回单").callbackData("userOrderType:1").build());
        buttonList.add(InlineKeyboardButton.builder().text("查看剩余未回单").callbackData("userOrderType:2").build());

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);


        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();

        return inlineKeyboardMarkup;
    }


    private void startMessage(BotServiceImpl bot, Message message) {

        //获取用户信息
        Long tgId = message.getFrom().getId();
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getTgId, tgId);
        User user = userMapper.selectOne(userWrapper);

//        List<UserAndOrderVo> userOrderList = new ArrayList<>();
//        if (user == null) {
//            user = new User();
//            user.setTgId(tgId);
//            user.setTgName(message.getFrom().getFirstName() + message.getFrom().getLastName());
//            userMapper.insert(user);
//        } else {
//            userOrderList = orderMapper.getUserAndOrderVoByTgId(tgId);
//        }


//        List<UserAndOrderVo> surplusOrder = new ArrayList<>();
//        List<UserAndOrderVo> replyOrder = new ArrayList<>();
//        if (!userOrderList.isEmpty()) {
//            // 根据订单状态分组
//            Map<Integer, List<UserAndOrderVo>> groupOrders = groupOrdersByStatus(userOrderList);
//            // 遍历订单状态
//            Set<Integer> orderStatus = groupOrders.keySet();
//            for (Integer status : orderStatus) {
//                List<UserAndOrderVo> orders = groupOrders.get(status);
//                if (status == OrderStatus.IN_PROGRESS.getCode() || status == OrderStatus.PENDING.getCode() || status == OrderStatus.REVIEW.getCode()) {
//                    surplusOrder.addAll(orders);
//                } else if (status == OrderStatus.COMPLETED.getCode()) {
//                    replyOrder.addAll(orders);
//                }
//            }
//        }


        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(BotMessageUtils.getUserInfoMessage(user));
        sendMessage.setReplyMarkup(createInlineKeyboardButton(user));
        sendMessage.enableMarkdownV2(true);
        try {
            bot.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(BotServiceImpl bot, Message message) {
        startMessage(bot, message);
    }
}
