package com.li.bot.entity.database.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: li
 * @CreateTime: 2024-10-02
 */
@Data
public class OrderBusinessVo {

    private String orderId;

    private Long tgId;
    private Integer status;

    private String businessName;

    private BigDecimal businessMoney;

    private Long replyTgId ;

    private String userName ;


}
