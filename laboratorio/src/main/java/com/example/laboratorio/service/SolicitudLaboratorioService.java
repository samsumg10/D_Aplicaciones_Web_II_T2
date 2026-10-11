package com.example.laboratorio.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.laboratorio.client.AtencionClient;
import com.example.laboratorio.client.AtencionDto;
import com.example.laboratorio.domain.EstadoSolicitud;
import com.example.laboratorio.domain.SolicitudLaboratorio;
import com.example.laboratorio.dto.CambioEstadoSolicitudRequest;
import com.example.laboratorio.dto.SolicitudRequest;
import com.example.laboratorio.dto.SolicitudResponse;
import com.example.laboratorio.exception.BusinessException;
import com.example.laboratorio.messaging.EventoDominio;
import com.example.laboratorio.messaging.EventoPublisher;
import com.example.laboratorio.messaging.RabbitConfig;
import com.example.laboratorio.repository.SolicitudLaboratorioRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SolicitudLaboratorioService {

    private static final String RECURSO = "SOLICITUD_LABORATORIO";

    private final SolicitudLaboratorioRepository repository;
    private final AtencionClient atencionClient;
    private final EventoPublisher publisher;

    /** RN-08: la solicitud se asocia a una atención existente y no anulada; el paciente se toma de ella. */
    public Mono<SolicitudResponse> registrar(SolicitudRequest request) {
        return atencionClient.obtenerAtencion(request.atencionId())
                .flatMap(atencion -> atencion.estaAnulada()
                        ? Mono.<AtencionDto>error(new BusinessException(HttpStatus.CONFLICT, "RN-08",
                                "La atención " + atencion.codigo() + " está ANULADA y no admite solicitudes"))
                        : Mono.just(atencion))
                .flatMap(atencion -> repository.existsByAtencionIdAndTipoExamenAndEstadoIn(atencion.id(),
                                request.tipoExamen(), EstadoSolicitud.ACTIVOS)
                        .flatMap(existe -> existe
                                ? Mono.<SolicitudLaboratorio>error(new BusinessException(HttpStatus.CONFLICT, "RN-08",
                                        "Ya hay una solicitud de " + request.tipoExamen() + " en curso para esta atención"))
                                : repository.save(nuevaSolicitud(request, atencion))))
                .flatMap(solicitud -> publicar(solicitud, "SolicitudLaboratorioRegistrada",
                        RabbitConfig.RK_SOLICITUD_REGISTRADA, null).thenReturn(solicitud))
                .map(SolicitudResponse::from);
    }

    public Flux<SolicitudResponse> listar(Long pacienteId, Long atencionId, EstadoSolicitud estado) {
        Flux<SolicitudLaboratorio> solicitudes = atencionId != null ? repository.findByAtencionId(atencionId)
                : pacienteId != null ? repository.findByPacienteIdOrderByFechaSolicitudDesc(pacienteId)
                : repository.findAll();
        return solicitudes.filter(s -> estado == null || s.getEstado() == estado).map(SolicitudResponse::from);
    }

    public Mono<SolicitudResponse> buscarPorId(Long id) {
        return obtener(id).map(SolicitudResponse::from);
    }

    /** Avanza el estado. El resultado ficticio es obligatorio al COMPLETAR y no se acepta en otro estado. */
    public Mono<SolicitudResponse> cambiarEstado(Long id, CambioEstadoSolicitudRequest request) {
        EstadoSolicitud destino = request.estado();
        boolean traeResultado = request.resultadoFicticio() != null && !request.resultadoFicticio().isBlank();
        return obtener(id)
                .flatMap(solicitud -> {
                    EstadoSolicitud anterior = solicitud.getEstado();
                    if (!anterior.puedeCambiarA(destino)) {
                        return Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-08",
                                "No se puede cambiar una solicitud " + anterior + " a " + destino));
                    }
                    if (destino == EstadoSolicitud.COMPLETADO && !traeResultado) {
                        return Mono.error(new BusinessException(HttpStatus.BAD_REQUEST, "RN-08",
                                "Para completar la solicitud se debe registrar el resultado ficticio"));
                    }
                    if (destino != EstadoSolicitud.COMPLETADO && traeResultado) {
                        return Mono.error(new BusinessException(HttpStatus.BAD_REQUEST, "RN-08",
                                "El resultado solo se registra al pasar a COMPLETADO"));
                    }
                    solicitud.setEstado(destino);
                    if (traeResultado) {
                        solicitud.setResultadoFicticio(request.resultadoFicticio().trim());
                    }
                    solicitud.setFechaActualizacion(LocalDateTime.now());
                    return repository.save(solicitud)
                            .flatMap(guardada -> publicar(guardada, "SolicitudLaboratorioActualizada",
                                    RabbitConfig.RK_ESTADO_ACTUALIZADO, anterior).thenReturn(guardada));
                })
                .map(SolicitudResponse::from);
    }

    private Mono<SolicitudLaboratorio> obtener(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "SOLICITUD_NO_ENCONTRADA",
                        "No existe una solicitud de laboratorio con id " + id)));
    }

    private SolicitudLaboratorio nuevaSolicitud(SolicitudRequest r, AtencionDto atencion) {
        return SolicitudLaboratorio.builder()
                .codigo("LAB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .pacienteId(atencion.pacienteId())
                .atencionId(atencion.id())
                .tipoExamen(r.tipoExamen())
                .estado(EstadoSolicitud.SOLICITADO)
                .fechaSolicitud(LocalDateTime.now())
                .build();
    }

    /** El resultado ficticio no viaja en el evento: el aviso solo informa el cambio de estado. */
    private Mono<Void> publicar(SolicitudLaboratorio s, String tipo, String routingKey, EstadoSolicitud anterior) {
        String detalle = "Solicitud " + s.getCodigo() + " de " + s.getTipoExamen() + ": " + s.getEstado();
        return publisher.publicar(routingKey, EventoDominio.de(tipo, RECURSO, s.getId(), s.getPacienteId(),
                anterior == null ? null : anterior.name(), s.getEstado().name(), detalle));
    }
}
