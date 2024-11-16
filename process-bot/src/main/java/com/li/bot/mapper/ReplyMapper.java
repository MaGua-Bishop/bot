package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.li.bot.entity.database.Reply;
import org.apache.ibatis.annotations.*;

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

    @Select(" select r.reply_id,r.tg_id,r.order_id,r.message_chat_id ,r.message_id,r.message_type,r.status,r.create_time,r.update_time from tg_reply r LEFT JOIN tg_order o on r.order_id::uuid = o.order_id LEFT JOIN tg_business b on o.business_id = b.business_id where r.tg_id = #{arg0} and r.status = #{arg1} and b.type = #{arg2}")
    List<Reply> getReplyListByStuta(Long tgId,Integer status,Integer type);

    @Update("update tg_reply set status = 1 where order_id = #{orderId}")
    int updateStatusByOrderId(@Param("orderId") String orderId);

    @Update("update tg_reply set status = -2 where order_id = #{orderId}")
    int updateStatusByOrderId02(@Param("orderId") String orderId);
}
