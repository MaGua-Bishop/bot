package com.li.bot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.AddAdminGroup;
import com.li.bot.entity.AdminChannelFile;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-08
 */
@Service
public class FileService {

    @Autowired
    private BotConfig botConfig;


    public AddAdminGroup getAddAdminGroup() {
        String groupFilePath = botConfig.getAdminChannelFile();
        try {
            String string = FileUtils.readFileToString(new File(groupFilePath));
            AddAdminGroup addAdminGroup = JSON.parseObject(string, AddAdminGroup.class);
            return addAdminGroup;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addAdminGroup(AddAdminGroup addAdminGroup){
        try (FileWriter fileWriter = new FileWriter(new File(botConfig.getAdminChannelFile()))) {
            // 将字符串写入文件
            String text = JSON.toJSONString(addAdminGroup);
            fileWriter.write(text);
        } catch (IOException e) {
            e.printStackTrace();
            // 处理异常
        }
    }


    public void addText(String text){
        try (FileWriter fileWriter = new FileWriter(new File(botConfig.getTextFile()))) {
            // 将字符串写入文件
            fileWriter.write(text);
        } catch (IOException e) {
            e.printStackTrace();
            // 处理异常
        }
    }

    public void addButtonText(String text){
        try (FileWriter fileWriter = new FileWriter(new File(botConfig.getTextBottomFile()))) {
            // 将字符串写入文件
            fileWriter.write(text);
        } catch (IOException e) {
            e.printStackTrace();
            // 处理异常
        }
    }

    public String getText(){
        String string = null;
        try {
            string = FileUtils.readFileToString(new File(botConfig.getTextFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return string;
    }

    public String getButtonText(){
        String string = null;
        try {
            string = FileUtils.readFileToString(new File(botConfig.getTextBottomFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return string;
    }

}
