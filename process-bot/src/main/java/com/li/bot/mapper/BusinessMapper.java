package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.vo.BusinessListVo;
import org.apache.ibatis.annotations.*;

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

    List<BusinessListVo> selectBusinessList(@Param("type")Integer type);

    @Insert("INSERT INTO tg_business (tg_id, name, message_id, money, status) VALUES (#{tgId}, #{name}, #{messageId}, #{money}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "businessId", keyColumn = "business_id")
    int insertBusiness(Business business);

    @Update("UPDATE tg_business SET type = #{type} WHERE business_id = #{businessId}")
    int updateBusinessByBusinessId(@Param("businessId")Long businessId, @Param("type")Integer type);



}
