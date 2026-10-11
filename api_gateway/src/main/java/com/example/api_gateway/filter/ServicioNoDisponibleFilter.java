package com.example.api_gateway.filter;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Si un microservicio se detiene, Eureka tarda unos segundos en retirarlo del registro. En ese intervalo
 * el Gateway intenta conectarse a la instancia caída y la conexión es rechazada. Sin este filtro el cliente
 * recibiría un 500 genérico; con él recibe 503 con el mismo formato de error que usan los microservicios.
 */
@Component
public class ServicioNoDisponibleFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(ServicioNoDisponibleFilter::esConexionRechazada, ex -> responder503(exchange, ex));
    }

    private static boolean esConexionRechazada(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConnectException) {
                return true;
            }
        }
        return false;
    }

    private static Mono<Void> responder503(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse respuesta = exchange.getResponse();
        if (respuesta.isCommitted()) {
            return Mono.error(ex);
        }
        Route ruta = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String servicio = ruta != null ? ruta.getId() : "solicitado";
        String json = "{\"fecha\":\"" + LocalDateTime.now() + "\",\"status\":503,\"codigo\":\"SERVICIO_NO_DISPONIBLE\","
                + "\"mensaje\":\"El servicio " + servicio + " no está disponible; intente nuevamente en unos minutos\"}";

        respuesta.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer cuerpo = respuesta.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return respuesta.writeWith(Mono.just(cuerpo));
    }

    /** Envuelve al filtro que enruta (NettyRoutingFilter) para capturar sus errores de conexión. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
