package com.li.bot.entity.database;

import lombok.Data;

/**
 * @Author: li
 * @CreateTime: 2024-10-13
 */
@Data
public class ConvoysInfoListVo {

    private Long convoysId;

    private String convoysName;

    private Long convoysCapacity;

    private Long currentCapacity ;

    private Long convoysSubscription ;

}
