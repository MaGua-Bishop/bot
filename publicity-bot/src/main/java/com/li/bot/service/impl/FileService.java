package com.li.bot.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.AdminChannelFile;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<Long> getAdminChannelList() {
        String string = readFileContent();
        AdminChannelFile adminChannelFile = JSONObject.parseObject(string, AdminChannelFile.class);
        List<String> adminChannelList = adminChannelFile.getAdminChannelList();
        return adminChannelList.stream().map(Long::parseLong).collect(Collectors.toList());
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
