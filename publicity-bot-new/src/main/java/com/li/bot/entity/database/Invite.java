package com.li.bot.entity.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@TableName("tg_invite")
@Data
public class Invite {
    @TableId(value = "invite_id",type = IdType.AUTO)
    private Long inviteId;
    @TableField("tg_id")
    private Long tgId;
    @TableField("chat_id")
    private Long chatId;
    @TableField("name")
    private String name ;
    @TableField("type")
    private Integer type ;
    @TableField("member_count")
    private Long memberCount ;
    @TableField("is_permissions")
    private Boolean isPermissions ;
    @TableField("link")
    private String link;
    @TableField("is_review")
    private Boolean isReview;
    @TableField("user_name")
    private String userName ;
    @TableField("message_id")
    private Integer messageId ;
}
