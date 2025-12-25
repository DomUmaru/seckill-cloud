package com.seckill.service.config;

import com.seckill.common.entity.SeckillGoods;
import com.seckill.service.mapper.SeckillGoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockWarmup implements CommandLineRunner {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> 开始进行缓存预热...");

        // 1. 从数据库查询所有参与秒杀的商品
        // (实际业务中可能只查 status=1 的活动商品)
        List<SeckillGoods> goodsList = seckillGoodsMapper.selectList(); // MP 写法，或者你自己写 selectAll

        // 2. 遍历商品，把库存写入 Redis
        for (SeckillGoods goods : goodsList) {
            String key = "seckill:stock:" + goods.getId();
            // 将数据库的库存覆盖到 Redis
            redisTemplate.opsForValue().set(key, String.valueOf(goods.getStockCount()));
            System.out.println(">>> 商品 " + goods.getId() + " 库存预热完成: " + goods.getStockCount());
        }

        System.out.println(">>> 缓存预热结束！");
    }
}