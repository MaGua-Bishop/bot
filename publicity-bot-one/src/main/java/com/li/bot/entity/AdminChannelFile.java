package com.li.bot.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-10
 */
@Data
public class AdminChannelFile {

    @JsonProperty("adminChannelList")
    private List<String> adminChannelList;

}
