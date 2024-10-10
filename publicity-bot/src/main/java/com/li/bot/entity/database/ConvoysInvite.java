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
@TableName("tg_convoys_invite")
@Data
public class ConvoysInvite {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    @TableField("convoys_id")
    private Long convoysId;

    @TableField("invite_id")
    private Long inviteId;

    @TableField("review_tg_id")
    private Long reviewTgId;

    @TableField("is_review")
    private Boolean isReview;

}
