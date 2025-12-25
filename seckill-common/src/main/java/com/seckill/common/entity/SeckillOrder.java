package com.seckill.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeckillOrder {
    /**
     * ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 商品ID
     */
    private Long goodsId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
