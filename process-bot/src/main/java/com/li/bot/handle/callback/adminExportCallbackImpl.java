package com.li.bot.handle.callback;

import com.google.common.collect.Lists;
import com.li.bot.entity.database.UserMoney;
import com.li.bot.enums.UserMoneyStatus;
import com.li.bot.mapper.UserMapper;
import com.li.bot.mapper.UserMoneyMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
public class adminExportCallbackImpl implements ICallback {

    @Override
    public String getCallbackName() {
        return "adminExport";
    }


    @Autowired
    private UserMoneyMapper userMoneyMapper;

    private ByteArrayInputStream createExcelStream(List<UserMoney> userMonies) throws IOException {
        Workbook workbook = new XSSFWorkbook(); // 创建工作簿
        Sheet sheet = workbook.createSheet("record"); // 创建工作表

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("用户ID");
        headerRow.createCell(1).setCellValue("原余额");
        headerRow.createCell(2).setCellValue("金额");
        headerRow.createCell(3).setCellValue("现余额");
        headerRow.createCell(4).setCellValue("类型");
        headerRow.createCell(5).setCellValue("时间");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int i = 1;
        for (UserMoney userMony : userMonies) {
            Row row = sheet.createRow(i);
            // 用户 ID
            row.createCell(0).setCellValue(userMony.getTgId() == null ? "" : userMony.getTgId().toString());
            // 原余额
            row.createCell(1).setCellValue(userMony.getUserMoney() == null ? "" : userMony.getUserMoney().toString());
            // 金额
            row.createCell(2).setCellValue(userMony.getMoney() == null ? "" : userMony.getMoney().toString());
            // 现余额
            row.createCell(3).setCellValue(userMony.getAfterMoney() == null ? "" : userMony.getAfterMoney().toString());
            // 类型
            row.createCell(4).setCellValue(UserMoneyStatus.getMessageByCode(userMony.getType()));
            // 创建时间
            String formattedTime = userMony.getCreateTime() == null ? "" : dateFormat.format(userMony.getCreateTime());
            row.createCell(5).setCellValue(formattedTime);
            i++;
        }


        // 将数据写入内存流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return new ByteArrayInputStream(outputStream.toByteArray()); // 转为输入流
    }


    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {

        List<UserMoney> userMonies = userMoneyMapper.selectUserMoneys();
        if (userMonies.isEmpty()) {
            bot.execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId()).text("没有数据").build());
            return;
        }

        try {
            // 创建 Excel 文件数据流
            ByteArrayInputStream excelStream = createExcelStream(userMonies);

            // 准备发送的文档
            InputFile inputFile = new InputFile(excelStream, "record.xlsx");

            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(callbackQuery.getMessage().getChatId());
            sendDocument.setDocument(inputFile);

            bot.execute(sendDocument); // 发送文件

        } catch (IOException | TelegramApiException e) {
            e.printStackTrace();
        }

    }

}
