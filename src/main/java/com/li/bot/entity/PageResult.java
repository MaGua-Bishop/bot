package com.li.bot.entity;

import lombok.Data;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-02
 */
@Data
public class PageResult<T> {
    private Integer pageNum;
    private Long pages; //总页数
    private List<T> list;

}
