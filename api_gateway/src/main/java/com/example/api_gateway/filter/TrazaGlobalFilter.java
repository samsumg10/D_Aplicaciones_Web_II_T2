package com.example.api_gateway.filter;

import java.net.URI;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Se aplica a todas las rutas. Asigna un X-Correlation-Id a cada petición (si el cliente no envía uno),
 * lo reenvía al microservicio y lo devuelve en la respuesta, y registra a qué servicio se enrutó,
 * con qué código respondió y cuánto tardó. Sirve como evidencia del enrutamiento del Gateway.
 */
@Component
public class TrazaGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID = "X-Correlation-Id";

    private static final Logger log = LoggerFactory.getLogger(TrazaGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long inicio = System.currentTimeMillis();
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        String id = correlationId != null ? correlationId : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest().mutate().header(CORRELATION_ID, id).build();
        exchange.getResponse().getHeaders().set(CORRELATION_ID, id);

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(senal -> {
                    Route ruta = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    URI destino = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                    log.info("{} {} -> ruta {} ({}) | {} | {} ms | {}", request.getMethod(), request.getURI().getPath(),
                            ruta != null ? ruta.getId() : "sin ruta", destino != null ? destino.getAuthority() : "-",
                            exchange.getResponse().getStatusCode(), System.currentTimeMillis() - inicio, id);
                });
    }

    /** Antes que los filtros que enrutan, para que el encabezado llegue al microservicio. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
