package com.li.bot.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.AdminChannelFile;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * @Author: li
 * @CreateTime: 2024-10-08
 */
@Service
public class FileService {

    @Autowired
    private BotConfig botConfig;


    public String readFileContent() {
        String groupFilePath = botConfig.getAdminChannelFile();
        try {
            String string = FileUtils.readFileToString(new File(groupFilePath));
            return string;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String,String> getAdminChannelList() {
        String string = readFileContent();
        Map<String,String> adminChannelFile = JSONObject.parseObject(string, Map.class);
        return adminChannelFile;
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



    public void addGroupId(String newGroupId) {
        try {
            // 读取JSON文件
            AdminChannelFile workgroup = new ObjectMapper().readValue(readFileContent(), AdminChannelFile.class);

            // 添加新的ID
            if (workgroup.getAdminChannelList() == null) {
                workgroup.setAdminChannelList(Arrays.asList(newGroupId));
            } else {
                workgroup.getAdminChannelList().add(newGroupId);
            }

            // 将更新后的对象写回文件
            new ObjectMapper().writeValue(new File(botConfig.getAdminChannelFile()), workgroup);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
