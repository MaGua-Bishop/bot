package com.li.bot.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-25
 */
public class ReviewGroup {

    @JsonProperty("id")
    private List<String> groupIdList;

    public List<String> getGroupIdList() {
        return groupIdList;
    }

    public void setGroupIdList(List<String> groupIdList) {
        this.groupIdList = groupIdList;
    }
}
