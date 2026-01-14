package com.geofleet.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter to handle SSE route timeouts.
 * Sets response timeout to -1 (no timeout) for SSE routes.
 */

@Component
@SuppressWarnings("null")
public class SseTimeoutConfig implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        if (route != null && route.getMetadata().containsKey("response-timeout")) {
            Object timeout = route.getMetadata().get("response-timeout");
            if (timeout instanceof Number && ((Number) timeout).intValue() == -1) {
                // Set attribute to disable timeout for this request
                exchange.getAttributes().put("response-timeout", -1L);
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
