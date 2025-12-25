package com.seckill.user.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.common.dto.UserDTO;
import com.seckill.common.entity.User;
import com.seckill.common.vo.UserVO;
import com.seckill.user.mapper.UserMapper;
import com.seckill.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * User Service 实现类
 * 处理具体的业务逻辑，如参数校验、DTO/VO转换、调用Mapper
 * 引入 Redis 缓存
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public UserVO createUser(UserDTO userDTO) {
        User user = new User();
        user.setCreateTime(LocalDateTime.now());
        BeanUtils.copyProperties(userDTO, user);
        userMapper.insert(user);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public void deleteUser(Long id) {
        // 1. 删除数据库
        int rows = userMapper.deleteById(id);
        if (rows == 0) throw new IllegalArgumentException("删除失败，ID不存在" + id);
        
        // 2. 删除缓存 (Cache Aside: 先更新数据库，后删除缓存)
        String key = "user:cache:" + id;
        redisTemplate.delete(key);
        log.info("删除缓存: {}", key);
    }

    @Override
    public UserVO updateUser(UserDTO userDTO) {
        // 兼容旧接口
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        userMapper.update(user);
        
        // 删除缓存
        if (userDTO.getId() != null) {
            String key = "user:cache:" + userDTO.getId();
            redisTemplate.delete(key);
            log.info("删除缓存: {}", key);
        }
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
    


    @Override
    public UserVO getUser(Long id) {
        String key = "user:cache:" + id;

        // 1. 先查 Redis
        String json = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(json)) {
            try {
                // 如果存在，反序列化并返回
                UserVO userVO = objectMapper.readValue(json, UserVO.class);
                log.info("命中缓存: {}", key);
                return userVO;
            } catch (JsonProcessingException e) {
                log.error("JSON 解析失败", e);
            }
        }

        // 2. 再查数据库
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在，ID：" + id);
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        // 3. 写入 Redis (过期时间 30 分钟)
        try {
            String cacheValue = objectMapper.writeValueAsString(userVO);
            redisTemplate.opsForValue().set(key, cacheValue, 30, TimeUnit.MINUTES);
            log.info("写入缓存: {}", key);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败", e);
        }

        return userVO;
    }

    @Override
    public List<UserVO> getUserList(int page, int size) {
        // TODO: 1. 计算 offset = (page - 1) * size
        // TODO: 2. 调用 userMapper.selectList(offset, size)
        // TODO: 3. 将 List<User> 转换为 List<UserVO>
        // TODO: 4. 返回 List<UserVO>
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    @Override
    public UserVO updateUserById(Long id, UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setId(id);

        // 1. 更新数据库
        userMapper.updateById(user);

        // 2. 删除缓存
        String key = "user:cache:" + id;
        redisTemplate.delete(key);
        log.info("删除缓存: {}", key);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public User getUserByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在，username：" + username);
        }
        return user;
    }
}
