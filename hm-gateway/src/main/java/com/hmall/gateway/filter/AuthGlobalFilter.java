package com.hmall.gateway.filter;

import cn.hutool.core.text.AntPathMatcher;
import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class AuthGlobalFilter implements Ordered, GlobalFilter {
    private final JwtTool jwtTool;
    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取请求参数
        ServerHttpRequest request = exchange.getRequest();
        // 2. 查看请求路径是否需要拦截
        if(exclude(authProperties, request.getPath().value())) {
            return chain.filter(exchange);
        }

        List<String> authorization = request.getHeaders().get("authorization");
        String token = null;
        if(authorization != null && !authorization.isEmpty()) {
            token = authorization.get(0);
        }
        // 3. 解析token
        Long userId = null;

        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e){
            // jwt校验未通过，响应401
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 4. 将用户信息添加到请求头中
        ServerWebExchange swe = exchange;
        if (userId != null) {
            String userInfo = userId.toString();
             swe = exchange.mutate()
                     .request(builder -> builder.header("user-info", userInfo))
                     .build();
        }
        // 5. 放行
        return chain.filter(swe);
    }

    private boolean exclude(AuthProperties authProperties, String path) {
        List<String> excludePaths = authProperties.getExcludePaths();
        for (String excludePath : excludePaths) {
            if(antPathMatcher.match(excludePath, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
