package com.li.bot.entity;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-11-16
 */
public class Code {
    private String text ;
    private List<Files> files ;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Files> getFiles() {
        return files;
    }

    public void setFiles(List<Files> files) {
        this.files = files;
    }
}
