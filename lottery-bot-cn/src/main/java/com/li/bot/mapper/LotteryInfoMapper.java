package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Lottery;
import com.li.bot.entity.database.LotteryInfo;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
@Mapper
public interface LotteryInfoMapper extends BaseMapper<LotteryInfo> {

    @Select("select * from tg_lottery_info where lottery_id  = #{lotteryId} and (status = 0 or status = 1)")
    List<LotteryInfo> getLotteryInfoList(@Param("lotteryId")String lotteryId);

    @Select("select * from tg_lottery_info where tg_id  = #{tgId} and (status = 0 or status = 1)")
    List<LotteryInfo> getLotteryInfoListByTgId(@Param("tgId")Long tgId);


}
