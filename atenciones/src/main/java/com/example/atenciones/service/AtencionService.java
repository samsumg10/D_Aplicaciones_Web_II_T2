package com.example.atenciones.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.atenciones.client.CitaClient;
import com.example.atenciones.client.CitaDto;
import com.example.atenciones.domain.Atencion;
import com.example.atenciones.domain.EstadoAtencion;
import com.example.atenciones.dto.AtencionRequest;
import com.example.atenciones.dto.AtencionResponse;
import com.example.atenciones.exception.BusinessException;
import com.example.atenciones.messaging.EventoDominio;
import com.example.atenciones.messaging.EventoPublisher;
import com.example.atenciones.messaging.RabbitConfig;
import com.example.atenciones.repository.AtencionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtencionService {

    private static final String RECURSO = "ATENCION";

    private final AtencionRepository repository;
    private final CitaClient citaClient;
    private final EventoPublisher publisher;

    /**
     * RN-07: la atención requiere una cita CONFIRMADA y solo puede haber una por cita.
     * Pasos: validar la cita (síncrono) -> guardar la atención -> pedir a citas que la marque ATENDIDA.
     * Si citas rechaza el último paso, se elimina la atención guardada (acción compensatoria) y se informa
     * el error, de modo que no quede una atención sin su cita ATENDIDA.
     */
    public Mono<AtencionResponse> registrar(AtencionRequest request) {
        return repository.existsByCitaId(request.citaId())
                .flatMap(existe -> existe
                        ? Mono.<CitaDto>error(atencionDuplicada(request.citaId()))
                        : citaClient.obtenerCita(request.citaId()))
                .flatMap(cita -> cita.estaConfirmada()
                        ? Mono.just(cita)
                        : Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-07",
                                "Solo se registran atenciones de citas CONFIRMADAS; la cita " + cita.codigo()
                                        + " está " + cita.estado())))
                .flatMap(cita -> repository.save(nuevaAtencion(request, cita))
                        .onErrorMap(DataIntegrityViolationException.class, e -> atencionDuplicada(request.citaId())))
                .flatMap(atencion -> citaClient.marcarAtendida(atencion.getCitaId())
                        .thenReturn(atencion)
                        .onErrorResume(ex -> {
                            log.warn("Citas rechazó marcar la cita {} como ATENDIDA; se revierte la atención {}",
                                    atencion.getCitaId(), atencion.getCodigo());
                            return repository.delete(atencion).then(Mono.error(ex));
                        }))
                .flatMap(atencion -> publicar(atencion, "AtencionRegistrada", RabbitConfig.RK_ATENCION_REGISTRADA, null)
                        .thenReturn(atencion))
                .map(AtencionResponse::from);
    }

    public Flux<AtencionResponse> listar(Long pacienteId) {
        Flux<Atencion> atenciones = pacienteId == null ? repository.findAll()
                : repository.findByPacienteIdOrderByFechaAtencionDesc(pacienteId);
        return atenciones.map(AtencionResponse::from);
    }

    /** Contrato síncrono que usa laboratorio para validar la atención. */
    public Mono<AtencionResponse> buscarPorId(Long id) {
        return obtener(id).map(AtencionResponse::from);
    }

    public Mono<AtencionResponse> cambiarEstado(Long id, EstadoAtencion destino) {
        return obtener(id)
                .flatMap(atencion -> {
                    EstadoAtencion anterior = atencion.getEstado();
                    if (!anterior.puedeCambiarA(destino)) {
                        return Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-07",
                                "No se puede cambiar una atención " + anterior + " a " + destino));
                    }
                    atencion.setEstado(destino);
                    atencion.setFechaActualizacion(LocalDateTime.now());
                    String tipo = destino == EstadoAtencion.FINALIZADA ? "AtencionFinalizada" : "AtencionAnulada";
                    String routingKey = destino == EstadoAtencion.FINALIZADA ? RabbitConfig.RK_ATENCION_FINALIZADA
                            : RabbitConfig.RK_ATENCION_ANULADA;
                    return repository.save(atencion)
                            .flatMap(guardada -> publicar(guardada, tipo, routingKey, anterior).thenReturn(guardada));
                })
                .map(AtencionResponse::from);
    }

    private Mono<Atencion> obtener(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "ATENCION_NO_ENCONTRADA",
                        "No existe una atención con id " + id)));
    }

    private Atencion nuevaAtencion(AtencionRequest r, CitaDto cita) {
        return Atencion.builder()
                .codigo("ATE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .pacienteId(cita.pacienteId())
                .citaId(cita.id())
                .area(r.area())
                .observacion(r.observacion() == null || r.observacion().isBlank() ? null : r.observacion().trim())
                .estado(EstadoAtencion.REGISTRADA)
                .fechaAtencion(LocalDateTime.now())
                .build();
    }

    private Mono<Void> publicar(Atencion a, String tipo, String routingKey, EstadoAtencion anterior) {
        String detalle = "Atención " + a.getCodigo() + " en " + a.getArea() + " (cita " + a.getCitaId() + "): "
                + a.getEstado();
        return publisher.publicar(routingKey, EventoDominio.de(tipo, RECURSO, a.getId(), a.getPacienteId(),
                anterior == null ? null : anterior.name(), a.getEstado().name(), detalle));
    }

    private static BusinessException atencionDuplicada(Long citaId) {
        return new BusinessException(HttpStatus.CONFLICT, "RN-07", "La cita " + citaId + " ya tiene una atención registrada");
    }
}
