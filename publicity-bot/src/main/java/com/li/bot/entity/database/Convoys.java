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
@TableName("tg_convoys")
public class Convoys {
    @TableId(value = "convoys_id",type = IdType.AUTO)
    private Long convoysId;
    @TableField("tg_id")
    private Long tgId ;
    @TableField("name")
    private String name ;
    @TableField("copywriter")
    private String copywriter;
    @TableField("capacity")
    private Long capacity;
    @TableField("subscription")
    private Long subscription;
    @TableField("interval_minutes")
    private int intervalMinutes;

}
