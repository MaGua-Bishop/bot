package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 指定用户增加money
    @Update("UPDATE tg_user SET money = money + #{amount} WHERE tg_id = #{userId}")
    void addMoney(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    //指定用户减少money
    @Update("UPDATE tg_user SET money = money - #{amount} WHERE tg_id = #{userId}")

    void reduceMoney(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
