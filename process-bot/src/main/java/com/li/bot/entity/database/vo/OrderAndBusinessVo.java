package com.li.bot.entity.database.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: li
 * @CreateTime: 2024-10-02
 */
@Data
public class OrderAndBusinessVo {

    private String orderId;

    private Integer orderStatus;

    private Long businessId;

    private String businessName;

    private BigDecimal businessMoney;



}
