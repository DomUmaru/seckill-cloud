package com.seckill.service.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;

@Slf4j
@RocketMQTransactionListener // 标记这是事务监听器
public class SeckillTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 1. 执行本地事务 (当你调用 sendMessageInTransaction 后，会执行这里)
     * 在这里执行 Redis 扣减和标记逻辑
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            // 解析参数 (arg 是我们在 Controller 里传过来的参数数组)
            Object[] args = (Object[]) arg;
            Long userId = (Long) args[0];
            Long goodsId = (Long) args[1];
            String stockKey = (String) args[2];
            String boughtKey = (String) args[3];

            // === 核心业务：Redis 操作放这里 ===

            // 1. 再次检查是否重复 (双重保险)
            if (redisTemplate.hasKey(boughtKey)) {
                log.info("用户{}已经购买过物品{}", userId, goodsId);
                return RocketMQLocalTransactionState.ROLLBACK;
            }

            // 2. 扣减库存
            Long stock = redisTemplate.opsForValue().decrement(stockKey);
            if (stock < 0) {
                return RocketMQLocalTransactionState.ROLLBACK;
            }

            // 3. 标记已买
            redisTemplate.opsForValue().set(boughtKey, "1");

            log.info("【本地事务】Redis 扣减成功，通知 MQ 提交消息");
            return RocketMQLocalTransactionState.COMMIT; // 告诉 MQ：由于我成功了，你可以把消息发给消费者了

        } catch (Exception e) {
            log.error("本地事务执行异常", e);
            return RocketMQLocalTransactionState.ROLLBACK; // 告诉 MQ：我失败了，把消息删了吧
        }
    }

    /**
     * 2. 回查本地事务 (万一上面那步执行完，还没来得及告诉 MQ 结果，机器挂了，MQ 会来问这个方法)
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        // 解析消息体里的 userId 和 goodsId
        String body = new String((byte[]) msg.getPayload());
        String[] args = body.split(",");
        String boughtKey = "seckill:bought:" + args[1] + ":" + args[0];

        // 检查 Redis 里有没有“已买”标记
        if (redisTemplate.hasKey(boughtKey)) {
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}