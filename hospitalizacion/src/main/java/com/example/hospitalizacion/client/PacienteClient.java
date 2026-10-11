package com.example.hospitalizacion.client;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.hospitalizacion.exception.BusinessException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Comunicación síncrona (no bloqueante) con el servicio de pacientes.
 *
 * <ul>
 *   <li>Contrato: GET /api/pacientes/{id} -> 200 con el paciente, 404 si no existe.</li>
 *   <li>Tiempo de espera: servicios.timeout (3 s por defecto) por intento.</li>
 *   <li>Reintentos: 2, solo ante fallos transitorios (sin conexión, timeout, 5xx). Un 404 no se reintenta.</li>
 *   <li>Indisponibilidad: si se agotan los reintentos se responde 503 PACIENTES_NO_DISPONIBLE y la operación no se realiza.</li>
 * </ul>
 */
@Component
public class PacienteClient {

    private final WebClient webClient;
    private final Duration timeout;

    public PacienteClient(@LoadBalanced WebClient.Builder builder,
            @Value("${servicios.pacientes.url}") String url,
            @Value("${servicios.timeout:3s}") Duration timeout) {
        this.webClient = builder.baseUrl(url).build();
        this.timeout = timeout;
    }

    public Mono<PacienteDto> obtenerPaciente(Long id) {
        return webClient.get()
                .uri("/api/pacientes/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == 404, respuesta -> Mono.error(new BusinessException(
                        HttpStatus.NOT_FOUND, "PACIENTE_NO_ENCONTRADO", "No existe un paciente con id " + id)))
                .bodyToMono(PacienteDto.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(PacienteClient::esTransitorio)
                        .onRetryExhaustedThrow((spec, senal) -> senal.failure()))
                .onErrorMap(PacienteClient::esTransitorio, ex -> new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                        "PACIENTES_NO_DISPONIBLE",
                        "El servicio de pacientes no está disponible; intente nuevamente en unos minutos"));
    }

    static boolean esTransitorio(Throwable ex) {
        return ex instanceof TimeoutException
                || ex instanceof WebClientRequestException
                || (ex instanceof WebClientResponseException respuesta && respuesta.getStatusCode().is5xxServerError());
    }
}
