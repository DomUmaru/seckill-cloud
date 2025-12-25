package com.seckill.common.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * User View Object
 * 用于返回给前端展示的数据（已隐藏敏感信息如密码）
 */
@Data
public class UserVO {
    /**
     * ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
