package com.li.bot.entity.database.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
@Data
public class UserAndOrderVo {

    private String orderId ;

    private Long tgId ;

    private String businessName ;

    private BigDecimal businessMoney ;

    private Integer orderStatus ;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime ;


}
