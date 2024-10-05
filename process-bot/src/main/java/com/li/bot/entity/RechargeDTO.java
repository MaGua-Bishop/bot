package com.li.bot.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: li
 * @CreateTime: 2024-10-05
 */
@Data
public class RechargeDTO {

    private String from ;

    private String to ;

    private BigDecimal amount;

    private Long transactionTime ;

    private String txId ;

}
