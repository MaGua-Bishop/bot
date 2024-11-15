package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.UserMoney;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface UserMoneyMapper extends BaseMapper<UserMoney> {

    @Select("SELECT * FROM tg_user_money WHERE DATE(create_time) = CURRENT_DATE AND type IN (0, 3, 4);")
    List<UserMoney> selectTodayUserMoney();

}
