package com.li.bot.sessions;

import com.li.bot.entity.database.Business;
import com.li.bot.handle.key.BotKeyFactory;
import com.li.bot.handle.key.IKeyboard;
import com.li.bot.handle.menu.BotMenuFactory;
import com.li.bot.handle.menu.IBotMenu;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.sessions.enums.BusinessSessionState;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class AddBusiness {

    private BotServiceImpl bot ;

    private BusinessSession businessSession;

    private Message message ;
    private AddBusinessSessionList addBusinessSessionList;
    private BusinessMapper businessMapper;

    public AddBusiness(BotServiceImpl bot, BusinessSession businessSession, Message message, AddBusinessSessionList addBusinessSessionList, BusinessMapper businessMapper){
        this.bot = bot ;
        this.businessSession = businessSession;
        this.message = message ;
        this.addBusinessSessionList = addBusinessSessionList;
        this.businessMapper = businessMapper;
    }

    public void execute(BotMenuFactory botMenuFactory, BotKeyFactory botKeyFactory){

        IBotMenu menu = botMenuFactory.getMenu(message.getText());
        if(menu != null ){
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("操作已取消").build());
                addBusinessSessionList.removeUserSession(message.getFrom().getId());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            menu.execute(bot, message);
            return;
        }else {
            IKeyboard key = botKeyFactory.getKey(message.getText());
            if(key != null ){
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("操作已取消").build());
                    addBusinessSessionList.removeUserSession(message.getFrom().getId());
                } catch (TelegramApiException e) {
                     throw new RuntimeException(e);
                }
                key.execute(bot, message);
                return;
            }
        }

        BusinessSessionState state = businessSession.getState();
        switch (state) {
            case WAITING_FOR_BUSINESS_NAME:
                handleBusinessNameInput(message);
                break;
            case WAITING_FOR_COPYWRITING:
                handleBusinessCopywritingInput(message);
                break;
            case WAITING_FOR_PRICE:
                handleMoneyInput(message);
                break;
            default:
                // 处理其他状态
                break;
        }
    }

    private boolean nameCheck(Business business){
        String name = business.getName();

        Pattern pattern = Pattern.compile("#(热门|冷门)\\s+([\\u4e00-\\u9fa5a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(name);

        if (matcher.find()) {
            String tag = matcher.group(1);
            String businessName = matcher.group(2);

            if(tag.equals("热门")){
                business.setStatus(0);
            }else{
                business.setStatus(1);
            }
            business.setName(businessName);
            return true;
        } else {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("输入格式不正确，请确保输入格式如：#热门 业务名称").build());
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("操作已取消").build());
                addBusinessSessionList.removeUserSession(message.getFrom().getId());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return false;
        }
    }

    private void handleBusinessNameInput(Message message) {
        String businessName = message.getText();
        addBusinessSessionList.getUserSession(message.getFrom().getId()).getBusiness().setName(businessName);
        boolean b = nameCheck(addBusinessSessionList.getUserSession(message.getFrom().getId()).getBusiness());
        if(!b){
            return;
        }
        try {
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("请输入业务文案").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        BusinessSession businessSession = addBusinessSessionList.getUserSession(message.getFrom().getId());
        businessSession.setState(BusinessSessionState.WAITING_FOR_COPYWRITING);

    }




    private void handleBusinessCopywritingInput(Message message) {
//        String businessCopywriting = message.getText();
//        CopyMessage copyMessage = new CopyMessage();
//        copyMessage.setChatId(message.getChatId());
//        copyMessage.setMessageId(message.getMessageId());
//        copyMessage.setFromChatId(message.getChatId());

//        try {
//            bot.execute(copyMessage);
//        } catch (TelegramApiException e) {
//            throw new RuntimeException(e);
//        }

        addBusinessSessionList.getUserSession(message.getFrom().getId()).getBusiness().setMessageId(message.getMessageId());
        try {
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("请输入业务价格").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        BusinessSession businessSession = addBusinessSessionList.getUserSession(message.getFrom().getId());
        businessSession.setState(BusinessSessionState.WAITING_FOR_PRICE);

    }

    private void handleMoneyInput(Message message) {
        String businessMoney = message.getText();
        BigDecimal bigDecimal = parseBusinessPrice(businessMoney);
        BusinessSession businessSession = addBusinessSessionList.getUserSession(message.getFrom().getId());
        businessSession.setState(BusinessSessionState.WAITING_FOR_PRICE);
        if(bigDecimal == null){
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("价格格式错误").build());
                addBusinessSessionList.removeUserSession(message.getFrom().getId());
//                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("请输入价格").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        Business business = businessSession.getBusiness();
        business.setMoney(bigDecimal);
        business.setTgId(message.getFrom().getId());
        //解析名字 判断是否是
        int insert = businessMapper.insert(business);
        if(insert<=0){
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("添加失败").build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        addBusinessSessionList.getUserSession(message.getFrom().getId()).getBusiness().setMoney(bigDecimal);
        try {
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text("添加成功").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        addBusinessSessionList.removeUserSession(message.getFrom().getId());

    }

    public static BigDecimal parseBusinessPrice(String input) {
        // 使用正则表达式验证是否是数字且最多保留两位小数
        Pattern pattern = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            // 使用 DecimalFormat 保留两位小数
            DecimalFormat df = new DecimalFormat("0.00");
            String format = df.format(Double.parseDouble(input));
            return new BigDecimal(format);
        } else {
            // 如果不是有效的数字或格式不正确，返回 null
            return null;
        }
    }


}
