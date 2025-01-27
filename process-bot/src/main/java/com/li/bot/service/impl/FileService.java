package com.li.bot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.Code;
import com.li.bot.entity.Workgroup;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @Author: li
 * @CreateTime: 2024-10-08
 */
@Service
public class FileService {

    @Autowired
    private BotConfig botConfig;


    public String readFileContent() {
        String groupFilePath = botConfig.getGroupFile();
        try {
            String string = FileUtils.readFileToString(new File(groupFilePath));
            return string;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readFileContent02() {
        String groupFilePath = botConfig.getGroupFile02();
        try {
            String string = FileUtils.readFileToString(new File(groupFilePath));
            return string;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readFileContent03() {
        String groupFilePath = botConfig.getGroupFile03();
        try {
            String string = FileUtils.readFileToString(new File(groupFilePath));
            return string;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getChannelLink() {
        String channelFile = botConfig.getChannelFile();
        try {
            String string = FileUtils.readFileToString(new File(channelFile));
            JSONObject jsonObject = JSON.parseObject(string);
            String s = jsonObject.getString("link");
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Code getCodeImage() {
        String channelFile = botConfig.getCodeFile();
        try {
            String string = FileUtils.readFileToString(new File(channelFile));
            JSONObject jsonObject = JSON.parseObject(string);
            Code s = jsonObject.to(Code.class);
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

public void setCodeImage(Code code) {
    try {
        new ObjectMapper().writeValue(new File(botConfig.getCodeFile()), code);
    } catch (IOException e) {
        e.printStackTrace();
    }
}



    public void addGroupId(String newGroupId) {
        try {
            // 读取JSON文件
            Workgroup workgroup = new ObjectMapper().readValue(readFileContent(), Workgroup.class);

            // 添加新的ID
            if (workgroup.getGroupList() == null) {
                workgroup.setGroupList(Arrays.asList(newGroupId));
            } else {
                workgroup.getGroupList().add(newGroupId);
            }

            // 将更新后的对象写回文件
            new ObjectMapper().writeValue(new File(botConfig.getGroupFile()), workgroup);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addGroupId02(String newGroupId) {
        try {
            // 读取JSON文件
            Workgroup workgroup = new ObjectMapper().readValue(readFileContent02(), Workgroup.class);

            // 添加新的ID
            if (workgroup.getGroupList() == null) {
                workgroup.setGroupList(Arrays.asList(newGroupId));
            } else {
                workgroup.getGroupList().add(newGroupId);
            }

            // 将更新后的对象写回文件
            new ObjectMapper().writeValue(new File(botConfig.getGroupFile02()), workgroup);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addGroupId03(String newGroupId) {
        try {
            // 读取JSON文件
            Workgroup workgroup = new ObjectMapper().readValue(readFileContent03(), Workgroup.class);

            // 添加新的ID
            if (workgroup.getGroupList() == null) {
                workgroup.setGroupList(Arrays.asList(newGroupId));
            } else {
                workgroup.getGroupList().clear();
                workgroup.getGroupList().add(newGroupId);
            }

            // 将更新后的对象写回文件
            new ObjectMapper().writeValue(new File(botConfig.getGroupFile03()), workgroup);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
