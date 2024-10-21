package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Data
@TableName("tg_user")
public class User {
    @TableId(value = "tg_id",type = IdType.INPUT)
    private Long tgId;
    @TableField(value = "tg_name")
    private String tgName ;
    @TableField("is_admin")
    private Boolean isAdmin ;
    @TableField("create_time")
    private Date createTime;
}
