package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.PrizePool;
import com.li.bot.entity.vo.PrizePoolVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface PrizePoolMapper extends BaseMapper<PrizePool> {

    //批量插入
    int batchSavePrizePool(List<PrizePool> list);

    @Select("select prize_pool_id,money from tg_prize_pool where lottery_id = #{lotteryId} and status = 0")
    List<PrizePoolVO> getRandomMoney(@Param("lotteryId") String lotteryId);

}
