package com.li.bot.sessions;

import com.li.bot.entity.Code;
import com.li.bot.entity.Files;
import com.li.bot.entity.database.Business;
import com.li.bot.handle.key.BotKeyFactory;
import com.li.bot.handle.key.IKeyboard;
import com.li.bot.handle.menu.BotMenuFactory;
import com.li.bot.handle.menu.IBotMenu;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.sessions.enums.AdminEditSessionState;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Slf4j
public class AdminEdit {

    private BotServiceImpl bot;

    private Message message;

    private AdminEditSessionList adminEditSessionList;

    private BusinessMapper businessMapper;

    private FileService fileService;

    public AdminEdit(BotServiceImpl bot, Message message, AdminEditSessionList adminEditSessionList, BusinessMapper businessMapper, FileService fileService) {
        this.bot = bot;
        this.message = message;
        this.adminEditSessionList = adminEditSessionList;
        this.businessMapper = businessMapper;
        this.fileService = fileService;
    }

    public void execute(BotMenuFactory botMenuFactory, BotKeyFactory botKeyFactory) {

        IBotMenu menu = botMenuFactory.getMenu(message.getText());
        if (menu != null) {
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("操作已取消").build());
                adminEditSessionList.removeUserSession(message.getFrom().getId());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            menu.execute(bot, message);
            return;
        } else {
            IKeyboard key = botKeyFactory.getKey(message.getText());
            if (key != null) {
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("操作已取消").build());
                    adminEditSessionList.removeUserSession(message.getFrom().getId());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                key.execute(bot, message);
                return;
            }
        }
        AdminEditSession userSession = adminEditSessionList.getUserSession(message.getFrom().getId());
        AdminEditSessionState state = userSession.getState();
        switch (state) {
            case WAITING_FOR_USER_MESSAGE:
                handleUserMessageInput(message, userSession);
                break;
            default:
                break;
        }
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

    private void handleUserMessageInput(Message message, AdminEditSession userSession) {
        Integer type = userSession.getType();
        if (0 == type) {
            Business business = userSession.getBusiness();

            business.setMessageId(message.getMessageId());
            business.setTgId(message.getFrom().getId());
            businessMapper.updateById(business);
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text(business.getName() + "文案修改成功").build());
                adminEditSessionList.removeUserSession(message.getFrom().getId());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (2 == type) {
            //保存用户发送的二维码
            // 获取最清晰的图片（最大的 PhotoSize）
            PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);

            // 获取图片的 file_id
            String fileId = photo.getFileId();
            Code codeImage = fileService.getCodeImage();
            List<Files> files = codeImage.getFiles();
            Files files1 = new Files();
            files1.setFile_id(fileId);
            files1.setId(files.size());
            files.add(files1);
            fileService.setCodeImage(codeImage);
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("新增二维码成功").build());
                adminEditSessionList.removeUserSession(message.getFrom().getId());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (type == 3) {
            Code codeImage = fileService.getCodeImage();
            String text = message.getText();
            codeImage.setText(text);
            fileService.setCodeImage(codeImage);
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text("修改充值文案成功").build());
                adminEditSessionList.removeUserSession(message.getFrom().getId());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else {
            Business business = userSession.getBusiness();

            String text = message.getText();
            BigDecimal money = isMoney(text);
            if (money == null) {
                try {
                    bot.execute(SendMessage.builder().chatId(message.getChatId()).text("金额错误,请重新修改业务价格").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            business.setMoney(money);
            businessMapper.updateById(business);
            try {
                bot.execute(SendMessage.builder().chatId(message.getChatId()).text(business.getName() + "价格修改成功").build());
                adminEditSessionList.removeUserSession(message.getFrom().getId());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

        adminEditSessionList.removeUserSession(message.getFrom().getId());
    }
}
