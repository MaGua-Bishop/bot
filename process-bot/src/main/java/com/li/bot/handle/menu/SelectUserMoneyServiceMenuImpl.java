package com.li.bot.handle.menu;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.Workgroup;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.Reply;
import com.li.bot.entity.database.User;
import com.li.bot.enums.OrderStatus;
import com.li.bot.enums.ReplyStatus;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.ReplyMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class SelectUserMoneyServiceMenuImpl implements IBotMenu {

    @Override
    public String getMenuName() {
        return "查询用户余额";
    }


    @Autowired
    private FileService fileService;

    @Autowired
    private UserMapper userMapper ;


    private String getUserId(String input) {
        String regex = "#用户id\\s*(\\d+)";

        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);

        // 创建匹配器
        Matcher matcher = pattern.matcher(input);

        // 查找匹配
        if (matcher.find()) {
            // 获取用户 ID
            String userId = matcher.group(1);
            return userId;
        } else {
            return null ;
        }
    }

    @Override
    public void execute(BotServiceImpl bot, Message message) {

        Long id = message.getFrom().getId();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, id));
        if(!user.getIsAdmin()){
            System.out.println("非管理员");
            return;
        }
        //判断是否是工作群
        String string = fileService.readFileContent();
        Workgroup workgroup = JSONObject.parseObject(string, Workgroup.class);
        boolean b = workgroup.getGroupList().contains(message.getChatId().toString());
        if(!b){
            System.out.println("非工作群");
            return;
        }

        String text = message.getText();
        String userId = getUserId(text);
        if(userId == null){
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("请输入正确的格式：#用户id 用户id").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, Long.valueOf(userId)));
        if(u == null){
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("用户不存在").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        try {
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("用户名:<a href=\"tg://user?id="+u.getTgId()+"\">"+u.getTgName()+"</a>\n用户id:"+u.getTgId()+"\n用户余额:"+u.getMoney() ).parseMode("html").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


    }
}
