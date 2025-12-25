package com.seckill.common.dto;

import lombok.Data;

/**
 * User Data Transfer Object
 * 用于接收前端传入的参数（如添加用户、修改用户）
 */
@Data
public class UserDTO {
    /**
     * ID (修改时必填，新增时忽略)
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
}
