package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Lottery;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
@Mapper
public interface LotteryMapper extends BaseMapper<Lottery> {



}
