package com.seckill.service.mq;

import com.seckill.common.entity.SeckillOrder;
import com.seckill.service.mapper.SeckillGoodsMapper;
import com.seckill.service.mapper.SeckillOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;

@Slf4j
@Service
@RocketMQMessageListener(topic = "seckill-topic", consumerGroup = "seckill-group")
public class SeckillConsumer implements RocketMQListener<String> {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启本地事务：扣库存和插订单要么全成，要么全败
    public void onMessage(String message) {
        // 1. 解析消息 "userId,goodsId"
        String[] args = message.split(",");
        Long userId = Long.parseLong(args[0]);
        Long goodsId = Long.parseLong(args[1]);

        try {
            log.info("【消费者】开始处理用户 {} 的秒杀请求...", userId);

            // 1. 先扣减库存
            int rows = seckillGoodsMapper.decreaseStock(goodsId);

            if (rows > 0) {

                SeckillOrder existOrder = seckillOrderMapper.checkActiveOrder(userId, goodsId);
                if (existOrder != null) {
                    System.out.println("【消费者】检测到已有有效订单，拒绝重复下单。");
                    return;
                }

                // 2. 扣库存成功 -> 下订单
                SeckillOrder order = new SeckillOrder();
                order.setUserId(userId);
                order.setGoodsId(goodsId);
                order.setCreateTime(LocalDateTime.now());
                order.setStatus(0);// 0-未支付
                // ⚠️ 关键点：这里插入时，如果数据库已经有该用户的单，会抛出 DuplicateKeyException
                seckillOrderMapper.insert(order);
                log.info(">>> 恭喜！用户 {} 秒杀成功！", userId);

                // ---【新增核心代码：发送延时消息】---
                // 消息内容直接传 orderId 即可，消费者拿到 ID 去查状态
                // 这里的 delayLevel = 3，对应 RocketMQ 的第 3 级 (10s)。
                // 级别参考: 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                // 真实业务通常用 16级 (30m)
                Message<Long> delayMsg = MessageBuilder.withPayload(order.getId()).build();

                rocketMQTemplate.syncSend("order-close-topic", delayMsg, 3000, 3);

                System.out.println("【下单成功】订单号: " + order.getId() + ", 已发送延时关闭消息(10s后检查)");

            } else {
                log.warn(">>> 遗憾！用户 {} 秒杀失败，库存不足", userId);
                // 库存不足不需要重试，直接结束即可
            }

        } catch (DuplicateKeyException e) {
            // 3. 【幂等性处理】捕获唯一索引冲突异常
            // 说明该消息已经被消费成功过，订单已存在。
            // 此时应该“吞掉”异常，不要抛出，告诉 RocketMQ "我消费成功了"，停止重试。
            log.warn("【消费者】消息重复消费，订单已存在，直接忽略。userId={}, goodsId={}", userId, goodsId);
            //把扣减库存恢复，catch后不会执行事务回滚，抛出异常才会，但是抛出异常在MQ中又会重试操作
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        } catch (Exception e) {
            // 4. 【异常重试处理】捕获其他未知异常 (如数据库挂了、网络抖动)
            log.error("【消费者】发生异常，准备抛出以触发 RocketMQ 重试", e);
            // 🚨 必须抛出异常！RocketMQ 只有捕获到异常才会进行第 2次、第 3次... 重试
            throw new RuntimeException("消费失败，请重试");
        }
    }
}