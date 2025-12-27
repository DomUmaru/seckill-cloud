package com.seckill.service.feign;

import com.seckill.common.entity.User;
import com.seckill.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// name = "seckill-user" 必须和 Nacos 里看到的服务名一模一样！
@FeignClient(name = "seckill-user", fallback = UserClientFallback.class)
public interface UserClient {

    // 这里的方法签名，要和 seckill-user Controller 里的方法一致
    // 假设 User 服务有一个 /user/info/{id} 的接口
    @GetMapping("/user/info/{id}")
    Result<User> getUserById(@PathVariable("id") Long id); // <--- 这里也要加
}