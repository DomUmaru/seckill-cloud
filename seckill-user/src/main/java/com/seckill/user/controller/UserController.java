package com.seckill.user.controller;


import com.seckill.common.dto.UserDTO;
import com.seckill.common.entity.User;
import com.seckill.common.result.Result;
import com.seckill.common.vo.UserVO;
import com.seckill.user.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Controller
 * 接收 HTTP 请求，调用 Service 层
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 新增用户
     * POST /users
     * @param userDTO 请求体参数
     * @return 创建成功的用户
     */
    @PostMapping
    public Result<UserVO> addUser(@RequestBody UserDTO userDTO) {
        UserVO userVO = userService.createUser(userDTO);
        return Result.success(userVO);
    }

    /**
     * 删除用户
     * DELETE /users/{id}
     * @param id 用户ID
     * @return 无内容
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success(null);
    }

    /**
     * 修改用户
     * PUT /users
     * @param userDTO 请求体参数 (需包含ID)
     * @return 修改后的用户
     */
    @PutMapping
    public Result<UserVO> updateUser(@RequestBody UserDTO userDTO) {
        UserVO userVO = userService.updateUser(userDTO);
        return Result.success(userVO);
    }

    /**
     * 查询用户详情
     * GET /users/{id}
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        UserVO userVO = userService.getUser(id);
        return Result.success(userVO);
    }

    /**
     * 分页查询用户列表
     * GET /users?page=1&size=10
     * @param page 页码，默认为1
     * @param size 每页数量，默认为10
     * @return 用户列表
     */
    @GetMapping
    public ResponseEntity<List<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // TODO: 调用 userService.getUserList(page, size)
        // TODO: 返回 ResponseEntity.ok(result)
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserVO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        UserVO userVO = userService.updateUserById(id, userDTO);
        return ResponseEntity.ok(userVO);
    }

    @GetMapping("/search")
    public Result<User> search(@RequestParam String username) {
        User user = userService.getUserByUsername(username);
        return Result.success(user);
    }

    /**
     * 模拟 CPU 100% 的死循环接口
     */
    @GetMapping("/cpu/loop")
    public String cpuLoop() {
        System.out.println("完蛋了，CPU 要爆炸了...");

        // 死循环：没有任何休眠，疯狂占用 CPU 时间片
        while (true) {
            // 做点无意义的计算，防止被编译器优化掉
            Math.sqrt(123456789.0);
        }
    }

    @GetMapping("/info/{id}")
    public Result<User> getUserById(@PathVariable("id") Long id) {
        UserVO userVO = userService.getUser(id);
        User user = new User();
        BeanUtils.copyProperties(userVO, user);
        return Result.success(user);
    }
}
