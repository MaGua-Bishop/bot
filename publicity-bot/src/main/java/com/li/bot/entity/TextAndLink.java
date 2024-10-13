package com.li.bot.entity;

import lombok.Data;

/**
 * @Author: li
 * @CreateTime: 2024-10-13
 */
@Data
public class TextAndLink {

    private String text ;

    private String link ;


    public TextAndLink() {
    }

    public TextAndLink(String text, String link) {
        this.text = text;
        this.link = link;
    }
}
