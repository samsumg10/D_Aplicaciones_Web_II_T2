package com.example.laboratorio.client;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.laboratorio.exception.BusinessException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Comunicación síncrona con atenciones: GET /api/atenciones/{id} (200 o 404).
 * Timeout de servicios.timeout por intento, 2 reintentos ante fallos transitorios y 503 si no responde.
 */
@Component
public class AtencionClient {

    private final WebClient webClient;
    private final Duration timeout;

    public AtencionClient(@LoadBalanced WebClient.Builder builder,
            @Value("${servicios.atenciones.url}") String url,
            @Value("${servicios.timeout:3s}") Duration timeout) {
        this.webClient = builder.baseUrl(url).build();
        this.timeout = timeout;
    }

    public Mono<AtencionDto> obtenerAtencion(Long id) {
        return webClient.get()
                .uri("/api/atenciones/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == 404, r -> Mono.error(new BusinessException(HttpStatus.NOT_FOUND,
                        "ATENCION_NO_ENCONTRADA", "No existe una atención con id " + id)))
                .bodyToMono(AtencionDto.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(AtencionClient::esTransitorio)
                        .onRetryExhaustedThrow((spec, senal) -> senal.failure()))
                .onErrorMap(AtencionClient::esTransitorio, ex -> new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                        "ATENCIONES_NO_DISPONIBLE",
                        "El servicio de atenciones no está disponible; intente nuevamente en unos minutos"));
    }

    static boolean esTransitorio(Throwable ex) {
        return ex instanceof TimeoutException
                || ex instanceof WebClientRequestException
                || (ex instanceof WebClientResponseException respuesta && respuesta.getStatusCode().is5xxServerError());
    }
}
