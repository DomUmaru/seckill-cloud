package com.seckill.gateway.filter;

import com.seckill.common.util.JwtUtil; // 假设你的工具类在这里
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关全局鉴权过滤器
 * 作用：拦截所有请求，校验 Token，将 userId 放入 Header 传递给下游
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    // 白名单：这些路径不需要登录就能访问
    private static final List<String> WHITELIST = List.of(
            "/user/login",      // 登录
            "/user/register",   // 注册
            "/seckill/list",    // 商品列表（假设允许游客看）
            "/doc.html"         // Swagger 文档等
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单放行
        for (String allowPath : WHITELIST) {
            if (antPathMatcher.match(allowPath, path)) {
                return chain.filter(exchange);
            }
        }

        // 2. 获取 Token (通常在 Header: Authorization 中)
        String token = null;
        List<String> headers = request.getHeaders().get("Authorization");
        if (headers != null && !headers.isEmpty()) {
            token = headers.get(0);
        }

        // 3. 校验 Token
        Long userId = null;
        try {
            userId = JwtUtil.parseToken(token);
            System.out.println("网关校验通过，用户ID: " + userId);
        } catch (Exception e) {
            // 校验失败，拦截！
            System.out.println("网关校验失败: " + e.getMessage());
            return denyAccess(exchange);
        }

        // 4. Token 接力 (关键一步！)
        // 把 userId 塞进 Header，传给下游微服务
        ServerHttpRequest newRequest = request.mutate()
                .header("userId", userId.toString())
                .build();

        // 5. 放行
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    // 拒绝访问，返回 401
    private Mono<Void> denyAccess(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // 返回一段简单的 JSON 提示
        String data = "{\"code\": 401, \"msg\": \"Unauthorized: Please Login\"}";
        DataBuffer buffer = response.bufferFactory().wrap(data.getBytes(StandardCharsets.UTF_8));
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 优先级设置，越小越先执行
        return 0;
    }
}