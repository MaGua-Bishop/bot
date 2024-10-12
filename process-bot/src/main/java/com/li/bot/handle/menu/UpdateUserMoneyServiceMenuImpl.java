package com.li.bot.handle.menu;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.Workgroup;
import com.li.bot.entity.database.User;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class UpdateUserMoneyServiceMenuImpl implements IBotMenu {

    @Override
    public String getMenuName() {
        return "修改用户金额";
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

        private BigDecimal isMoney(String money){
        // 使用正则表达式验证是否是数字且最多保留两位小数
        Pattern pattern = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
        Matcher matcher = pattern.matcher(money);

        if (matcher.matches()) {
            // 使用 DecimalFormat 保留两位小数
            DecimalFormat df = new DecimalFormat("0.00");
            String format = df.format(Double.parseDouble(money));
            return new BigDecimal(format);
        } else {
            // 如果不是有效的数字或格式不正确，返回 null
            return null;
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

        // 示例字符串

        // 定义正则表达式
        String regex = "#修改用户金额\\s+用户id\\s+(\\d+)\\s+金额\\s+([\\d]+(\\.\\d{2}))";

        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);

        // 创建匹配器
        Matcher matcher = pattern.matcher(text);

        // 查找匹配
        if (matcher.find()) {
            // 获取用户 ID 和金额
            String userId = matcher.group(1);
            String amount = matcher.group(2);
            BigDecimal money = isMoney(amount);
            if(money == null){
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("请输入正确的金额").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            User selectUserId = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, Long.valueOf(userId)));
            if(selectUserId == null){
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("用户不存在").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            selectUserId.setMoney(money);
            userMapper.updateById(selectUserId);
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("用户名:<a href=\"tg://user?id="+selectUserId.getTgId()+"\">"+selectUserId.getTgName()+"</a>\n用户id:"+selectUserId.getTgId()+"\n用户余额:"+selectUserId.getMoney() ).parseMode("html").build());

            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("请输入正确的格式：#修改用户金额 用户id 用户的id 金额 修改的金额(必须保留两位小数)").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
