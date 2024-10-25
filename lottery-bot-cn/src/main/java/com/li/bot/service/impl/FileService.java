package com.li.bot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.ReviewGroup;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-08
 */
@Service
public class FileService {

    @Autowired
    private BotConfig botConfig;


    public String readFileContent() {
        String reviewgroup = botConfig.getReviewgroup();
        try {
            String string = FileUtils.readFileToString(new File(reviewgroup));
            return string;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void addGroupId(String newGroupId) {
        try {
            // 读取JSON文件
            ReviewGroup reviewGroup = new ObjectMapper().readValue(readFileContent(), ReviewGroup.class);

            // 添加新的ID
            if (reviewGroup.getGroupIdList() == null) {
                reviewGroup.setGroupIdList(Arrays.asList(newGroupId));
            } else {
                reviewGroup.getGroupIdList().add(newGroupId);
            }

            // 将更新后的对象写回文件
            new ObjectMapper().writeValue(new File(botConfig.getReviewgroup()), reviewGroup);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getGroupIdList() {
        try {
            ReviewGroup reviewGroup = new ObjectMapper().readValue(readFileContent(), ReviewGroup.class);
            return reviewGroup.getGroupIdList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
