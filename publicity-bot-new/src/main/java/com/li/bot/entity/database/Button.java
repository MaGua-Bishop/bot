package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Author: li
 * @CreateTime: 2024-10-10
 */
@TableName("tg_button")
@Data
public class Button {

    @TableId(value = "button_id",type = IdType.AUTO)
    private Long buttonId;

    @TableField("name")
    private String name ;

    @TableField("tg_id")
    private Long tgId ;

    @TableField("url")
    private String url ;

}
