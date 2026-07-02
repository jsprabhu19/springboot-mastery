package com.quickeats.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filter that generates and propagates a correlation ID for distributed tracing.
 * Checks for "X-Correlation-Id" in request headers, generates one if missing, 
 * inserts it into downstream headers, and sets it in the response headers.
 */
@Component
public class CorrelationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        
        String correlationId = headers.getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            logger.debug("No Correlation ID found in request. Generated: {}", correlationId);
        } else {
            logger.debug("Found Correlation ID in request: {}", correlationId);
        }

        // Add to MDC for gateway log statements.
        MDC.put(MDC_CORRELATION_ID_KEY, correlationId);

        // Mutate request to propagate header to downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();
                
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        // Also add the correlation ID to the response headers so the client has it
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> MDC.remove(MDC_CORRELATION_ID_KEY));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
