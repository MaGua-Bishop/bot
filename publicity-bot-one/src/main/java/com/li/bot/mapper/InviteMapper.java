package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.ConvoysExitMembers;
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


    @Select("select * from tg_invite where tg_id = #{tgId}")
    List<Invite> getInviteListByChatIdAndMemberCount(@Param("tgId") Long tgId);


    @Select("<script>" +
            "SELECT * FROM tg_invite WHERE invite_id IN " +
            "<foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<Invite> getInviteListByIds(@Param("ids")List<Long> ids);

    @Update("UPDATE tg_invite SET message_id = #{messageId} WHERE invite_id = #{inviteId}")
    int updateMessageIdById(@Param("messageId") Integer messageId, @Param("inviteId") Long inviteId);

    @Select("select i.invite_id invite_id, c.convoys_id convoy_id,c.name convoy_name,i.name invite_name from tg_invite i left join tg_convoys_invite ci ON i.invite_id = ci.invite_id LEFT JOIN tg_convoys c on c.convoys_id = ci.convoys_id\n" +
            "WHERE i.invite_id =#{inviteId}")
    ConvoysExitMembers getConvoysExitMembersByInviteId(@Param("inviteId") Long inviteId);



}
