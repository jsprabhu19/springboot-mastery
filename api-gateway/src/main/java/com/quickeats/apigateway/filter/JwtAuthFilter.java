package com.quickeats.apigateway.filter;

import com.quickeats.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Custom Gateway Filter to authenticate and authorize requests using JWTs.
 * Extracts the JWT, validates its signature, and forwards the user context 
 * (ID, name, and role) downstream via custom HTTP headers.
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey("Authorization")) {
                logger.warn("Authorization header is missing");
                return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Authorization header format is incorrect or not a Bearer token");
                return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = jwtUtil.getClaims(token);
                if (jwtUtil.isExpired(claims)) {
                    logger.warn("JWT is expired");
                    return onError(exchange, "Token has expired", HttpStatus.UNAUTHORIZED);
                }

                String userId = jwtUtil.getClaimValue(claims, "userId");
                String role = jwtUtil.getClaimValue(claims, "role");
                String username = jwtUtil.getUsername(claims);

                logger.debug("Successfully validated token for user: {}, role: {}, userId: {}", username, role, userId);

                // Mutate the request to append the verified context headers
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-User-Role", role != null ? role : "")
                        .header("X-User-Name", username != null ? username : "")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                logger.error("JWT validation error: {}", e.getMessage());
                return onError(exchange, "Invalid or malformed token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        // Clean response body can be set here if needed, otherwise standard status code suffices
        return response.setComplete();
    }

    public static class Config {
        // Can hold configuration parameters for the filter if needed
    }
}
