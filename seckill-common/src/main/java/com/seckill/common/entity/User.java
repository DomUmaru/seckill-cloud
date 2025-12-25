package com.seckill.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * User Entity
 * 对应数据库中的 user 表
 */
@Data
public class User {
    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

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
