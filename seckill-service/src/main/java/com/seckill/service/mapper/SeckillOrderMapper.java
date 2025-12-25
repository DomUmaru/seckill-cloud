package com.seckill.service.mapper;

import com.seckill.common.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


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
}
