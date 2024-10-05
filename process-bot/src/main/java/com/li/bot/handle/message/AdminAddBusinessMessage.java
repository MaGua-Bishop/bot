//package com.li.bot.handle.message;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.li.bot.entity.database.Business;
//import com.li.bot.entity.database.User;
//import com.li.bot.mapper.BusinessMapper;
//import com.li.bot.mapper.UserMapper;
//import com.li.bot.service.impl.BotServiceImpl;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Message;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//
//import java.math.BigDecimal;
//import java.text.DecimalFormat;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * @Author: li
// * @CreateTime: 2024-10-01
// */
//public class AdminAddBusinessMessage {
//
//    private BotServiceImpl bot;
//    private UserMapper userMapper;
//    private BusinessMapper businessMapper;
//
//    private Message message;
//
//    public AdminAddBusinessMessage(BotServiceImpl bot,Message message ,UserMapper userMapper , BusinessMapper businessMapper) {
//        this.userMapper = userMapper;
//        this.businessMapper = businessMapper;
//        this.bot = bot;
//        this.message = message;
//    }
//
//
//
//    public static String parseBusinessName(String input,String type) {
//        // 查找 标记的位置
//        int startIndex = input.indexOf(type);
//
//        if (startIndex == -1) {
//            // 如果没有找到 返回空字符串或抛出异常
//            return "";
//        }
//
//        int nameStartIndex = input.indexOf(' ', startIndex) + 1;
//        if (nameStartIndex == -1 || nameStartIndex >= input.length()) {
//            return "";
//        }
//
//        // 查找下一个换行符或结束位置
//        int nameEndIndex = input.indexOf('\n', nameStartIndex);
//        if (nameEndIndex == -1) {
//            nameEndIndex = input.length();
//        }
//
//        // 提取业务名称
//        return input.substring(nameStartIndex, nameEndIndex).trim();
//    }
//
//    private BigDecimal isMoney(String money){
//        // 使用正则表达式验证是否是数字且最多保留两位小数
//        Pattern pattern = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
//        Matcher matcher = pattern.matcher(money);
//
//        if (matcher.matches()) {
//            // 使用 DecimalFormat 保留两位小数
//            DecimalFormat df = new DecimalFormat("0.00");
//            String format = df.format(Double.parseDouble(money));
//            return new BigDecimal(format);
//        } else {
//            // 如果不是有效的数字或格式不正确，返回 null
//            return null;
//        }
//    }
//
//
//    public void execute() throws TelegramApiException {
//        //判断是否是管理员
//        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
//        userWrapper.eq(User::getTgId, message.getFrom().getId());
//        User user = userMapper.selectOne(userWrapper);
//        if(user == null){
//            return;
//        }
//        if(!user.getIsAdmin() ){
//            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("无效命令").build());
//            return;
//        }
//        String text = message.getText();
//        String businessName = parseBusinessName(text,"业务名称");
//        String businessCopywriting = parseBusinessName(text,"业务文案");
//        String businessMoney = parseBusinessName(text,"业务价格");
//        System.out.println(businessName);
//        System.out.println(businessCopywriting);
//        System.out.println(businessMoney);
//        BigDecimal money = isMoney(businessMoney);
//        if(money ==null){
//            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("价格格式错误").build());
//            return;
//        }
//        if(businessCopywriting.isEmpty() || money.compareTo(BigDecimal.ZERO) <= 0){
//            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("价格格式错误").build());
//        }else {
//            Business business = new Business();
//            business.setName(businessName);
//            business.setDescription(businessCopywriting);
//            business.setMoney(money);
//            business.setTgId(user.getTgId());
//            int insert = businessMapper.insert(business);
//            if(insert <= 0){
//                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("添加失败").build());
//                return;
//            }
//            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("添加成功").build());
//        }
//
//
//
//    }
//
//}
