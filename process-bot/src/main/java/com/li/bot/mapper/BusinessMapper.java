package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface BusinessMapper extends BaseMapper<Business> {


    @Select("<script>" +
            "SELECT * FROM tg_business WHERE business_id IN " +
            "<foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<Business> selectAllBusiness(@Param("ids")List<Long> ids);


}
