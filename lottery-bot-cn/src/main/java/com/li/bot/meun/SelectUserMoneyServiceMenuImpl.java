package com.li.bot.meun;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
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


    private Long getUserId(String text){
        // 正则表达式模式，用于匹配固定十位数的ID
        Pattern idPattern = Pattern.compile("^\\d{5,15}$");
        Matcher idMatcher = idPattern.matcher(text);

        // 查找并打印所有匹配的ID
        if (idMatcher.find()) {
            String id = idMatcher.group(0);  // 提取ID
            return Long.parseLong(id);
        }
        return null;
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
        List<String> groupIdList = fileService.getGroupIdList();
        boolean b = groupIdList.contains(message.getChatId().toString());
        if (!b) {
            System.out.println("非工作群");
            return;
        }

        String text = message.getText();
        Long userId = getUserId(text);
        if(userId == 0L){
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("请输入正确的用户id").build());
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
