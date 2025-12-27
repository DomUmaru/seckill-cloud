package com.seckill.service.controller;

import com.seckill.common.context.UserContext;
import com.seckill.common.entity.SeckillGoods;
import com.seckill.common.result.Result;
import com.seckill.service.mapper.SeckillGoodsMapper;
import com.seckill.service.mapper.SeckillOrderMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import com.seckill.common.util.MD5Util;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;


    // 获取秒杀地址接口
    @GetMapping("/path")
    public Result<Map<String, Object>> getSeckillPath(@RequestParam Long goodsId) {

        // 生成当前时间戳
        long timestamp = System.currentTimeMillis();

        // 1. 【安全重构】从网关透传的 ThreadLocal 中获取用户 ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录失败");
        }
        // 生成签名 (Stateless)
        String sign = MD5Util.createSign(userId, goodsId, timestamp);

        // 返回给前端，前端下次请求要带上这两个东西
        Map<String, Object> map = new HashMap<>();
        map.put("sign", sign);      // 这就是之前的 "path"
        map.put("timestamp", timestamp);

        return Result.success(map);
    }

    // 模拟运营后台：发布秒杀活动，同步库存到 Redis
    @PostMapping("/publish")
    public Result<String> publishSeckill(@RequestParam Long goodsId) {
        // 1. 查数据库
        SeckillGoods goods = seckillGoodsMapper.selectById(goodsId);
        if (goods == null) {
            return Result.error("商品不存在");
        }

        // 2. 写入 Redis
        String key = "seckill:stock:" + goodsId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(goods.getStockCount()));

        // 3. (进阶) 这里还可以把用来做一人一单的 Redis Set 也清空一下，方便测试
        //stringRedisTemplate.delete("seckill:bought:" + goodsId); // 假设是 Set 结构的话

        return Result.success("活动发布成功，库存已热加载至 Redis");
    }

    /**
     * 秒杀接口
     * @param goodsId 商品ID
     * @param userId 模拟的用户ID（实际开发中从 Token 获取）
     */

    @PostMapping("/{sign}/seckill")
    public Result<String> seckill(@PathVariable("sign") String receivedSign,
                                  @RequestParam Long timestamp,
                                  @RequestParam Long goodsId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401,"请先登录");
        }

        // 1. 检查有效期 (比如 60秒内有效)
        // 防止黑客拿了一个去年的签名现在来刷
        long now = System.currentTimeMillis();
        if (now - timestamp > 60 * 100000) {
            return Result.error("链接已过期，请重新获取！");
        }

        // 2. 重新计算签名
        // 我们用同样的算法、同样的私钥(SALT)，算一遍应该是多少
        String expectedSign = MD5Util.createSign(userId, goodsId, timestamp);

        // 3. 比对签名
        if (!expectedSign.equals(receivedSign)) {
            // 如果不一样，说明有人篡改了参数，或者伪造了请求
            return Result.error("非法请求，签名验证失败！");
        }

        //redisson分布式锁是为了解决一人一单问题
        String lockKey = "lock:user:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(0, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return Result.error("系统繁忙");
        }
        if (!isLocked) {
            return Result.error("操作太频繁，请稍后再试！");
        }
        String stockKey = "seckill:stock:" + goodsId;
        String boughtKey = "seckill:bought:" + goodsId + ":" + userId;
        String msgBody = userId + "," + goodsId;

        if (stringRedisTemplate.hasKey(boughtKey)) {
            return Result.error("您已经购买过了");
        }

        Message<String> message = MessageBuilder.withPayload(msgBody).build();

        try {
            rocketMQTemplate.sendMessageInTransaction("seckill-topic", message, new Object[]{userId, goodsId, stockKey, boughtKey});
            return Result.success("排队中");
        }catch (Exception e) {
            e.printStackTrace(); // <--- 加上这一行
            System.out.println("MQ 发送失败原因: " + e.getMessage());
            return Result.error("系统繁忙");
        }finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }
}
