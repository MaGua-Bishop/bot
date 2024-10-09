package com.li.bot.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-08
 */
public class Workgroup {

    @JsonProperty("groupList")
    private List<String> groupList;

    public List<String> getGroupList() {
        return groupList;
    }

    public void setGroupList(List<String> groupList) {
        this.groupList = groupList;
    }
}
