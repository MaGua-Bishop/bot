package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Invite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Mapper
public interface InviteMapper extends BaseMapper<Invite> {


    @Select("select * from tg_invite where tg_id = #{tgId} and member_count >= #{memberCount} and is_review = false")
    List<Invite> getInviteListByChatIdAndMemberCount(@Param("tgId") Long tgId,@Param("memberCount") Long memberCount);


    @Select("<script>" +
            "SELECT * FROM tg_invite WHERE invite_id IN " +
            "<foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<Invite> getInviteListByIds(@Param("ids")List<Long> ids);

    @Update("UPDATE tg_invite SET message_id = #{messageId} WHERE invite_id = #{inviteId}")
    int updateMessageIdById(@Param("messageId") Integer messageId, @Param("inviteId") Long inviteId);



}
