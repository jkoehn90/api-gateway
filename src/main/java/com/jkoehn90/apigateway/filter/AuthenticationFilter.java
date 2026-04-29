package com.jkoehn90.apigateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    // List of endpoints that don't require authentication
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/auth/register",
            "/auth/login"
    );

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // 1. Skip authentication for open endpoints
            if (isOpenEndpoint(path)) {
                return chain.filter(exchange);
            }

            // 2. Check Authorization header exists
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header");
            }

            // 3. Extract token from header
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format");
            }

            String token = authHeader.substring(7);

            // 4. Validate token
            try {
                jwtUtil.validateToken(token);
            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                return onError(exchange, "Invalid or expired token");
            }

            // 5. Forward request to the appropriate service
            return chain.filter(exchange);
        };
    }

    // Check if the request path is an open endpoint
    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream()
                .anyMatch(path::equals);
    }

    // Return 401 Unauthorized response
    private Mono<Void> onError(ServerWebExchange exchange, String message) {
        log.error("Authentication error: {}", message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // Required config class for AbstractGatewayFilterFactory
    public static class Config {
    }
}
