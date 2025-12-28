package com.seckill.service.mapper;

import com.seckill.common.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
public interface SeckillOrderMapper {
    /**
     * 插入订单
     * @param seckillOrder 订单信息
     * @return 影响行数
     */
    int insert(SeckillOrder seckillOrder);
    
    /**
     * 根据用户和商品查询订单（用于防重）
     */
    SeckillOrder selectByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    int countByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    @Update("UPDATE seckill_order SET status = #{status} WHERE id = #{orderId} AND status = 0")
    int updateStatus(@Param("orderId") Long orderId, @Param("status") Integer status);

    @Select("SELECT * FROM seckill_order WHERE id = #{orderId}")
    SeckillOrder selectById(@Param("orderId") Long orderId);

    @Select("select * from seckill_order where user_id = #{userId} and goods_id = #{goodsId} and status >= 0 && status <= 1")
    SeckillOrder checkActiveOrder(@Param("userId")Long userId, @Param("goodsId")Long goodsId);
}
