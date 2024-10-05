package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Reply;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface ReplyMapper extends BaseMapper<Reply> {

    @Insert("insert into tg_reply(order_id,tg_id) values(#{arg0},#{arg1})")
    int insertReply(UUID orderId,Long tgId);

    int updateReply(Reply reply);

    @Select("select * from tg_reply where tg_id = #{arg0} and status = #{arg1}")
    List<Reply> getReplyListByStuta(Long tgId,Integer status);



}
