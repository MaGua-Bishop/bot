package com.li.bot.utils.entity;

/**
 * @Author: li
 * @CreateTime: 2024-09-29
 * @Description: 实体类
 */
public class StartMessage {

    private Long userId ;

    private String name ;

    private Long number ;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }
}
