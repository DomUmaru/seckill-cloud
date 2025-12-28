package com.seckill.service.mq;

import com.seckill.common.entity.SeckillOrder;
import com.seckill.service.mapper.SeckillGoodsMapper;
import com.seckill.service.mapper.SeckillOrderMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RocketMQMessageListener(topic = "order-close-topic", consumerGroup = "order-close-group")
public class OrderCloseConsumer implements RocketMQListener<Long> {

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启本地事务，保证 MySQL 回滚一致
    public void onMessage(Long orderId) {
        System.out.println("【关单检查】收到超时消息，订单ID: " + orderId);

        // 1. 查订单状态
        SeckillOrder order = seckillOrderMapper.selectById(orderId);

        // 这种情况可能是订单数据还没同步完，或者已经被删了
        if (order == null) {
            System.out.println("【关单检查】订单不存在，忽略");
            return;
        }

        // 2. 判断是否已支付
        if (order.getStatus() != 0) {
            System.out.println("【关单检查】订单状态为 " + order.getStatus() + " (非未支付)，无需处理");
            return;
        }

        // 3. 执行关单 (乐观锁思路: update ... where status=0)
        // 这一步非常关键，防止用户刚好在第 9.9 秒支付了，我们却在第 10 秒把它关了
        int rows = seckillOrderMapper.updateStatus(orderId, 2); // 2 = 已关闭
        if (rows > 0) {
            System.out.println("【超时关单】订单 " + orderId + " 超时未支付，已关闭");

            // 4. 回补 MySQL 库存
            seckillGoodsMapper.restoreStock(order.getGoodsId());
            System.out.println("【回补库存】MySQL 库存 +1");

            // 5. 回补 Redis 库存 (保证下一个人能抢到)
            String redisStockKey = "seckill:stock:" + order.getGoodsId();
            stringRedisTemplate.opsForValue().increment(redisStockKey);

            // 6. 清除 Redis 里的“已买过”标记 (如果是 Set 结构的一人一单)
            // 假设你的 key 是 seckill:bought:goodsId:userId
            String boughtKey = "seckill:bought:" + order.getGoodsId() + ":" + order.getUserId();
            stringRedisTemplate.delete(boughtKey);

            System.out.println("【回补库存】Redis 库存 +1，购买限制已清除");
        }
    }
}