package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface ConvoysInviteMapper extends BaseMapper<ConvoysInvite> {

    @Select("<script>" +
            "SELECT * FROM tg_convoys_invite WHERE convoys_id IN " +
            "<foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<Invite> getConvoysInviteListByConvoysIds(@Param("ids")List<Long> ids);

    @Select("SELECT  count(*) from tg_convoys_invite where convoys_id = #{convoysId} and is_review = true")
    Long getCountByConvoysId(@Param("convoysId") Long convoysId);

    @Select("UPDATE tg_convoys_invite SET message_id = #{messageId} WHERE invite_id = #{inviteId} and convoys_id = #{convoysId}")
    void updateMessageIdById(@Param("messageId") Integer messageId, @Param("inviteId") Long inviteId, @Param("convoysId") Long convoysId);


}
