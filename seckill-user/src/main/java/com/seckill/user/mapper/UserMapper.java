package com.seckill.user.mapper;

import com.seckill.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * User Mapper 接口
 * 负责与数据库进行交互
 */
@Mapper
public interface UserMapper {

    /**
     * 新增用户
     * @param user 用户实体
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 根据 ID 删除用户
     * 在 Mapper XML 中实现 delete 语句
     * @param id 用户ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新用户信息
     * 在 Mapper XML 中实现 update 语句
     * @param user 用户实体
     * @return 影响行数
     */
    int update(User user);

    /**
     * 根据 ID 查询用户
     * 在 Mapper XML 中实现 select 语句
     * @param id 用户ID
     * @return User 实体
     */
    User selectById(@Param("id") Long id);

    /**
     * 分页查询用户列表
     * TODO: 在 Mapper XML 中实现 select 语句，注意 limit 分页
     * @param offset 偏移量 ( (page - 1) * size )
     * @param limit 每页数量
     * @return User 实体列表
     */
    List<User> selectList(@Param("offset") int offset, @Param("limit") int limit);

    int updateById(User user);

    User selectByUsername(String username);
}
