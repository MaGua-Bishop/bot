package com.li.bot.entity;

import lombok.Data;

/**
 * @Author: li
 * @CreateTime: 2024-10-11
 */
@Data
public class OrderVo {

    private String name ;

    private Integer number ;

    public OrderVo(String name, Integer number) {
        this.name = name;
        this.number = number;
    }
}
