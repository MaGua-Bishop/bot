package com.li.bot;

import com.alibaba.fastjson2.JSONObject;
import com.li.bot.entity.Workgroup;
import com.li.bot.service.impl.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ProcessBotApplicationTests {

    @Autowired
    FileService fileService ;

    @Test
    void contextLoads()  {

        String fileContent = fileService.readFileContent();

        Workgroup workgroup = JSONObject.parseObject(fileContent, Workgroup.class);
        List<String> groupList = workgroup.getGroupList();
        for (String string : groupList) {
            System.out.println("群聊:"+string);
        }

    }

}
