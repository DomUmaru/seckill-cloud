package com.seckill.service.controller;

import com.seckill.common.entity.User;
import com.seckill.common.result.Result;
import com.seckill.service.feign.UserClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/feign")
public class FeignTestController {

    @Autowired
    private UserClient userClient; // 注入刚才定义的接口

    @GetMapping("/user/{id}")
// 重点在这里：加 ("id")
    public Result<User> testGetUserInfo(@PathVariable("id") Long id) {
        System.out.println("正在发起 RPC 调用，目标：seckill-user...");
        Result<User> result = userClient.getUserById(id);
        System.out.println("调用结束，结果：" + result);
        return result;
    }
}