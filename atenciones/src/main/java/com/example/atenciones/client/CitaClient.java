package com.example.atenciones.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.atenciones.exception.BusinessException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Comunicación síncrona con el servicio de citas.
 *
 * <ul>
 *   <li>GET /api/citas/{id}: 200 con la cita o 404. Se reintenta 2 veces ante fallos transitorios.</li>
 *   <li>PATCH /api/citas/{id}/estado {"estado":"ATENDIDA"}: citas valida la transición (409 si no corresponde).
 *       No se reintenta: si el primer intento llegó a aplicarse, un reintento recibiría 409 y se confundiría
 *       con un rechazo real.</li>
 *   <li>Tiempo de espera: servicios.timeout (3 s). Sin respuesta, se devuelve 503 CITAS_NO_DISPONIBLE.</li>
 * </ul>
 */
@Component
public class CitaClient {

    private final WebClient webClient;
    private final Duration timeout;

    public CitaClient(@LoadBalanced WebClient.Builder builder,
            @Value("${servicios.citas.url}") String url,
            @Value("${servicios.timeout:3s}") Duration timeout) {
        this.webClient = builder.baseUrl(url).build();
        this.timeout = timeout;
    }

    public Mono<CitaDto> obtenerCita(Long id) {
        return webClient.get()
                .uri("/api/citas/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == 404, r -> Mono.error(citaNoEncontrada(id)))
                .bodyToMono(CitaDto.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(CitaClient::esTransitorio)
                        .onRetryExhaustedThrow((spec, senal) -> senal.failure()))
                .onErrorMap(CitaClient::esTransitorio, ex -> noDisponible());
    }

    public Mono<CitaDto> marcarAtendida(Long id) {
        return webClient.patch()
                .uri("/api/citas/{id}/estado", id)
                .bodyValue(Map.of("estado", "ATENDIDA"))
                .retrieve()
                .onStatus(status -> status.value() == 404, r -> Mono.error(citaNoEncontrada(id)))
                .onStatus(status -> status.value() == 409, r -> Mono.error(new BusinessException(HttpStatus.CONFLICT,
                        "RN-07", "El servicio de citas no permitió marcar la cita " + id + " como ATENDIDA")))
                .bodyToMono(CitaDto.class)
                .timeout(timeout)
                .onErrorMap(CitaClient::esTransitorio, ex -> noDisponible());
    }

    static boolean esTransitorio(Throwable ex) {
        return ex instanceof TimeoutException
                || ex instanceof WebClientRequestException
                || (ex instanceof WebClientResponseException respuesta && respuesta.getStatusCode().is5xxServerError());
    }

    private static BusinessException citaNoEncontrada(Long id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "CITA_NO_ENCONTRADA", "No existe una cita con id " + id);
    }

    private static BusinessException noDisponible() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CITAS_NO_DISPONIBLE",
                "El servicio de citas no está disponible; intente nuevamente en unos minutos");
    }
}
