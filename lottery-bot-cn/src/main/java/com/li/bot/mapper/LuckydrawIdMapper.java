package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Luckydraw;
import com.li.bot.entity.database.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface LuckydrawIdMapper extends BaseMapper<Luckydraw> {

    // 根据用户的tgId和日期统计参与记录
    @Select("SELECT COUNT(*) FROM tg_luckydraw WHERE tg_id = #{tgId} AND DATE(create_time) = #{date}")
    int countByTgIdAndDate(@Param("tgId") Long tgId, @Param("date") LocalDate date);

    // 查询当天的所有记录，且状态为0（未开奖）
    @Select("SELECT * FROM tg_luckydraw WHERE DATE(luckydraw_time) = #{date} AND status = 0")
    List<Luckydraw> findLuckydrawsBeforeCutoff(@Param("date") LocalDate date);

    // 批量更新中奖者信息
    @Update({
            "<script>",
            "UPDATE tg_luckydraw",
            "SET money = CASE luckydraw_id",
            "<foreach collection='list' item='luckydraw' separator=' '>",
            "WHEN #{luckydraw.luckydrawId} THEN #{luckydraw.money}",
            "</foreach>",
            "END,",
            "update_time = #{updateTime}",
            "WHERE luckydraw_id IN",
            "<foreach collection='list' item='luckydraw' open='(' separator=',' close=')'>",
            "#{luckydraw.luckydrawId}",
            "</foreach>",
            "</script>"
    })
    void batchUpdate(@Param("list") List<Luckydraw> luckydraws, @Param("updateTime") LocalDateTime updateTime);

    // 批量更新状态为1
    @Update({
            "<script>",
            "UPDATE tg_luckydraw",
            "SET status = 1, update_time = NOW()",
            "WHERE luckydraw_id IN",
            "<foreach collection='list' item='luckydrawId' open='(' separator=',' close=')'>",
            "#{luckydrawId}",
            "</foreach>",
            "</script>"
    })
    void updateStatusToOne(@Param("list") List<Long> luckydrawIds);
}
