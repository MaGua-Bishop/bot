package com.li.bot.entity.database.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Data
public class BusinessListVo {

    @TableField("businessId")
    private Long businessId;
    @TableField("name")
    private String name ;
    @TableField("size")
    private Integer size ;


}
