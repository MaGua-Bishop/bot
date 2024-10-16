package com.li.bot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * @Author: li
 * @CreateTime: 2024-10-16
 */
@Data
public class ConvoysExitMembers {

    @TableField("invite_id")
    private Long inviteId;
    @TableField("convoy_id")
    private Long convoyId;
    @TableField("convoy_name")
    private String convoyName ;
    @TableField("invite_name")
    private String inviteName;

}
