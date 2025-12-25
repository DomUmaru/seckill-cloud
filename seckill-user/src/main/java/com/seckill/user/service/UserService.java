package com.seckill.user.service;



import com.seckill.common.dto.UserDTO;
import com.seckill.common.entity.User;
import com.seckill.common.vo.UserVO;

import java.util.List;

/**
 * User Service 接口
 * 定义业务逻辑规范
 */
public interface UserService {

    /**
     * 创建用户
     * @param userDTO 前端传入的参数
     * @return 创建成功后的 UserVO (可选，也可以只返回 boolean 或 void)
     */
    UserVO createUser(UserDTO userDTO);

    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 更新用户
     * @param userDTO 前端传入的参数 (包含ID)
     * @return 更新后的 UserVO
     */
    UserVO updateUser(UserDTO userDTO);

    /**
     * 获取用户详情
     * @param id 用户ID
     * @return UserVO 展示对象
     */
    UserVO getUser(Long id);

    /**
     * 分页获取用户列表
     * @param page 页码 (从1开始)
     * @param size 每页数量
     * @return UserVO 列表
     */
    List<UserVO> getUserList(int page, int size);

    UserVO updateUserById(Long id, UserDTO userDTO);

    User getUserByUsername(String username);
}
