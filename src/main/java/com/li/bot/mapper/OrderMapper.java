package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.vo.OrderAndBusinessVo;
import com.li.bot.entity.database.vo.UserAndOrderVo;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Insert("insert into tg_order(tg_id,business_id,message_id) values(#{tgId},#{businessId},#{messageId})")
    @Options(useGeneratedKeys = true, keyProperty = "orderId", keyColumn = "order_id")
    int insertOrder(Order order);

    @Select("select * from tg_order where order_id = #{arg0} and status = #{arg1}")
    Order getOrderByIdAndStatus(UUID orderId, Integer status);

    @Select("select * from tg_order where order_id = #{orderId}")
    Order getOrderByOrderId(@Param("orderId") UUID orderId);

    IPage<OrderAndBusinessVo> getOrderByBusinessIdAndStatus(@Param("page") Page<OrderAndBusinessVo> page , @Param("businessId") Long businessId, @Param("status") Integer status);

    @Update("update tg_order set status = #{arg0},review_tg_id = #{arg1},update_time = #{arg3} where order_id = #{arg2}")
    int updateOrderById(Integer status, Long reviewTgId, UUID orderId, LocalDateTime updateTime);

    @Update("update tg_order set status = #{arg0},update_time = #{arg2} where order_id = #{arg1}")
    int updateOrderStatusById(Integer status, UUID orderId, LocalDateTime updateTime);


    List<UserAndOrderVo> getUserAndOrderVoByTgId(Long tgId);

}
