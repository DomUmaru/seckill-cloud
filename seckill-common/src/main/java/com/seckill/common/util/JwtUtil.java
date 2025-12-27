package com.seckill.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

    // 1. 密钥：必须足够复杂，ACMer 就用这串随机字符吧
    // 在真实生产环境中，这个应该放在配置文件里
    private static final String SECRET = "SeckillCloudIsVeryCoolAndHardToBreakKey123456";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    // 2. 过期时间：1天 (毫秒)
    private static final long EXPIRATION = 1000 * 60 * 60 * 24;

    /**
     * 生成 Token
     * @param userId 用户ID
     * @return 加密后的 Token 字符串
     */
    public static String createToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString()) // 把 userId 存进去
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token
     * @param token 加密字符串
     * @return 用户ID (如果解析失败会抛出异常)
     */
    public static Long parseToken(String token) {
        if (token == null) return null;
        // 如果前端传了 "Bearer " 前缀，去掉它
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }
}