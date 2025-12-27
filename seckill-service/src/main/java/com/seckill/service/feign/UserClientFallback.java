package com.seckill.service.feign;

import com.seckill.common.entity.User;
import com.seckill.common.result.Result;
import org.springframework.stereotype.Component;

/**
 * 这是一个备胎类。
 * 当 UserClient 调不通（挂了、超时、被限流）时，OpenFeign 会自动调用这里的代码。
 */
@Component // 记得交给 Spring 管理
public class UserClientFallback implements UserClient {

    @Override
    public Result<User> getUserById(Long id) {
        // 这里不要抛异常，而是返回一个“兜底数据”
        User mockUser = new User();
        mockUser.setId(id);
        mockUser.setUsername("暂无信息(降级)");

        return Result.error(500, "用户服务繁忙，请稍后再试 (Fallback)");
    }
}