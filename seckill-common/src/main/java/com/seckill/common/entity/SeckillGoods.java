package com.seckill.common.entity;

import lombok.Data;

@Data
public class SeckillGoods {
    /**
     * ID
     */
    private Long id;
    
    /**
     * 商品名字
     */
    private String goodsName;
    
    /**
     * 库存
     */
    private Integer stockCount;
}
