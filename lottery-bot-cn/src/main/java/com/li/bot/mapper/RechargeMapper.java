package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Recharge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface RechargeMapper extends BaseMapper<Recharge> {

    @Select("SELECT * FROM tg_recharge WHERE create_time >= NOW() - INTERVAL '10 minutes'")
    List<Recharge> selectWithinTenMinutesRechargeList();

    @Select("SELECT * FROM tg_recharge WHERE create_time >= NOW() - INTERVAL '10 minutes' and money = #{money} and status = 0")
    Recharge selectWithinTenMinutesRecharge(@Param("money") BigDecimal money);


}
