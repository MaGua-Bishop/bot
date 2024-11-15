package com.li.bot.handle.menu;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.Workgroup;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.UserMoney;
import com.li.bot.mapper.UserMapper;
import com.li.bot.mapper.UserMoneyMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ReduceUserMoneyServiceMenuImpl implements IBotMenu {

    @Override
    public String getMenuName() {
        return "减少余额";
    }


    @Autowired
    private FileService fileService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserMoneyMapper userMoneyMapper;


    private Long getUserId(String text) {
        // 正则表达式模式，用于匹配固定十位数的ID
        Pattern idPattern = Pattern.compile("^\\d{5,15}$");
        Matcher idMatcher = idPattern.matcher(text);

        // 检查整个文本是否完全匹配十位数字
        if (idMatcher.matches()) {
            // 如果匹配成功，将文本转换为 Long 并返回
            return Long.parseLong(text);
        }
        // 如果不匹配，返回 null
        return null;
    }

    private BigDecimal isMoney(String money) {
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
        if (!user.getIsAdmin()) {
            System.out.println("非管理员");
            return;
        }
        //判断是否是工作群
        String string = fileService.readFileContent();
        Workgroup workgroup = JSONObject.parseObject(string, Workgroup.class);
        boolean b = workgroup.getGroupList().contains(message.getChatId().toString());

        String string02 = fileService.readFileContent02();
        Workgroup workgroup02 = JSONObject.parseObject(string02, Workgroup.class);
        boolean b02 = workgroup02.getGroupList().contains(message.getChatId().toString());

        String string03 = fileService.readFileContent03();
        Workgroup workgroup03 = JSONObject.parseObject(string03, Workgroup.class);
        boolean b03 = workgroup03.getGroupList().contains(message.getChatId().toString());
        if (!b && !b02 && !b03) {
            System.out.println("非工作群");
            return;
        }

        String text = message.getText();
        text = text.replace("#减少余额 ", "");

        String[] split = text.split(" ");
        if (split.length != 2) {
            return;
        }
        Long userId = getUserId(split[0]);
        if (userId == null) {
            return;
        }
        String money = split[1];
        BigDecimal amount;
        try {
            amount = new BigDecimal(money);
            // 如果能成功转换为 BigDecimal，则返回 true
        } catch (NumberFormatException e) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("请输入正确的金额").build());
            } catch (TelegramApiException t) {
                throw new RuntimeException(t);
            }
            return;
        }
        User selectUserId = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, userId));
        BigDecimal money1 = selectUserId.getMoney();
        if (selectUserId == null) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("用户不存在").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        selectUserId.setMoney(selectUserId.getMoney().subtract(amount));
        userMapper.updateById(selectUserId);
        try {
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("修改用户金额成功\n用户名:<a href=\"tg://user?id=" + selectUserId.getTgId() + "\">" + selectUserId.getTgName() + "</a>\n用户id:" + selectUserId.getTgId() + "\n用户余额:" + selectUserId.getMoney()).parseMode("html").build());
            log.info("用户id:{},减少前余额:{},减少余额:{},减少后余额:{}", selectUserId.getTgId(), money1, amount, selectUserId.getMoney());
            UserMoney userMoney = new UserMoney();
            userMoney.setTgId(selectUserId.getTgId());
            userMoney.setUserMoney(money1);
            userMoney.setMoney(amount);
            userMoney.setType(3);
            userMoney.setAfterMoney(selectUserId.getMoney());
            userMoneyMapper.insert(userMoney);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
