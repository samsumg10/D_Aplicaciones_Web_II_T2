package com.example.citas.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.citas.client.PacienteClient;
import com.example.citas.domain.Cita;
import com.example.citas.domain.EstadoCita;
import com.example.citas.dto.CitaRequest;
import com.example.citas.dto.CitaResponse;
import com.example.citas.dto.ReprogramacionRequest;
import com.example.citas.exception.BusinessException;
import com.example.citas.messaging.EventoDominio;
import com.example.citas.messaging.EventoPublisher;
import com.example.citas.messaging.RabbitConfig;
import com.example.citas.repository.CitaRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CitaService {

    private static final String RECURSO = "CITA";

    /** Supuestos de la agenda simplificada (RN-04): lunes a sábado, de 08:00 a 20:00, bloques de 30 minutos. */
    static final LocalTime INICIO_AGENDA = LocalTime.of(8, 0);
    static final LocalTime ULTIMO_BLOQUE = LocalTime.of(19, 30);

    private final CitaRepository repository;
    private final PacienteClient pacienteClient;
    private final EventoPublisher publisher;

    public Mono<CitaResponse> registrar(CitaRequest request) {
        return Mono.fromRunnable(() -> validarAgenda(request.fechaHora()))                         // RN-04
                .then(Mono.defer(() -> pacienteClient.obtenerPaciente(request.pacienteId())))     // síncrono
                .flatMap(paciente -> paciente.estaActivo()
                        ? Mono.just(paciente)
                        : Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-03",
                                "El paciente " + request.pacienteId() + " está INACTIVO y no puede recibir citas")))
                .then(Mono.defer(() -> validarDisponibilidad(request.pacienteId(), request.profesionalCodigo(),
                        request.fechaHora(), null)))                                                    // RN-05
                .then(Mono.defer(() -> repository.save(nuevaCita(request))))
                .flatMap(cita -> publicar(cita, "CitaRegistrada", RabbitConfig.RK_CITA_REGISTRADA, null).thenReturn(cita))
                .map(CitaResponse::from);
    }

    public Flux<CitaResponse> listar(Long pacienteId, EstadoCita estado) {
        Flux<Cita> citas = pacienteId != null ? repository.findByPacienteIdOrderByFechaHoraDesc(pacienteId)
                : repository.findAll();
        return citas.filter(c -> estado == null || c.getEstado() == estado).map(CitaResponse::from);
    }

    public Mono<CitaResponse> buscarPorId(Long id) {
        return obtener(id).map(CitaResponse::from);
    }

    public Mono<CitaResponse> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo.toUpperCase())
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "CITA_NO_ENCONTRADA",
                        "No existe una cita con código " + codigo)))
                .map(CitaResponse::from);
    }

    /** Confirmar, cancelar, marcar como atendida o ausente. Solo se aceptan las transiciones de RN-06. */
    public Mono<CitaResponse> cambiarEstado(Long id, EstadoCita destino) {
        return obtener(id)
                .flatMap(cita -> {
                    EstadoCita anterior = cita.getEstado();
                    if (!anterior.puedeCambiarA(destino)) {
                        return Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-06",
                                "No se puede cambiar una cita " + anterior + " a " + destino));
                    }
                    cita.setEstado(destino);
                    cita.setFechaActualizacion(LocalDateTime.now());
                    return repository.save(cita)
                            .flatMap(guardada -> publicar(guardada, tipoEvento(destino), routingKey(destino), anterior)
                                    .thenReturn(guardada));
                })
                .map(CitaResponse::from);
    }

    /** Cambia la fecha (y opcionalmente el profesional). La cita vuelve a PENDIENTE para confirmarse otra vez. */
    public Mono<CitaResponse> reprogramar(Long id, ReprogramacionRequest request) {
        return obtener(id)
                .flatMap(cita -> {
                    EstadoCita anterior = cita.getEstado();
                    if (!anterior.esActivo()) {
                        return Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-06",
                                "Solo se reprograman citas PENDIENTE o CONFIRMADA; la cita está " + anterior));
                    }
                    String profesional = request.profesionalCodigo() != null ? request.profesionalCodigo()
                            : cita.getProfesionalCodigo();
                    if (request.fechaHora().equals(cita.getFechaHora()) && profesional.equals(cita.getProfesionalCodigo())) {
                        return Mono.error(new BusinessException(HttpStatus.BAD_REQUEST, "RN-04",
                                "La nueva fecha u hora debe ser distinta a la actual"));
                    }
                    validarAgenda(request.fechaHora());                                                // RN-04
                    return validarDisponibilidad(cita.getPacienteId(), profesional, request.fechaHora(), id) // RN-05
                            .then(Mono.defer(() -> {
                                cita.setFechaHora(request.fechaHora());
                                cita.setProfesionalCodigo(profesional);
                                cita.setEstado(EstadoCita.PENDIENTE);
                                cita.setFechaActualizacion(LocalDateTime.now());
                                return repository.save(cita);
                            }))
                            .flatMap(guardada -> publicar(guardada, "CitaReprogramada",
                                    RabbitConfig.RK_CITA_REPROGRAMADA, anterior).thenReturn(guardada));
                })
                .map(CitaResponse::from);
    }

    private Mono<Cita> obtener(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "CITA_NO_ENCONTRADA",
                        "No existe una cita con id " + id)));
    }

    /** RN-04: la cita cae en un bloque válido de la agenda. */
    void validarAgenda(LocalDateTime fechaHora) {
        LocalTime hora = fechaHora.toLocalTime();
        if (fechaHora.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RN-04", "No se programan citas los domingos");
        }
        if (hora.isBefore(INICIO_AGENDA) || hora.isAfter(ULTIMO_BLOQUE)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RN-04",
                    "El horario de atención es de 08:00 a 20:00 (último bloque 19:30)");
        }
        if ((hora.getMinute() != 0 && hora.getMinute() != 30) || hora.getSecond() != 0 || hora.getNano() != 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RN-04",
                    "Las citas se programan en bloques de 30 minutos (hh:00 o hh:30)");
        }
    }

    /** RN-05: ni el profesional ni el paciente pueden tener otra cita activa en el mismo horario. */
    private Mono<Void> validarDisponibilidad(Long pacienteId, String profesional, LocalDateTime fechaHora, Long excluirId) {
        Mono<Boolean> profesionalOcupado = repository
                .findByProfesionalCodigoAndFechaHoraAndEstadoIn(profesional, fechaHora, EstadoCita.ACTIVOS)
                .filter(c -> !Objects.equals(c.getId(), excluirId))
                .hasElements();
        Mono<Boolean> pacienteOcupado = repository
                .findByPacienteIdAndFechaHoraAndEstadoIn(pacienteId, fechaHora, EstadoCita.ACTIVOS)
                .filter(c -> !Objects.equals(c.getId(), excluirId))
                .hasElements();

        return profesionalOcupado.flatMap(ocupado -> ocupado
                        ? Mono.<Void>error(new BusinessException(HttpStatus.CONFLICT, "RN-05",
                                "El profesional " + profesional + " ya tiene una cita el " + fechaHora))
                        : pacienteOcupado.flatMap(pacienteTiene -> pacienteTiene
                                ? Mono.<Void>error(new BusinessException(HttpStatus.CONFLICT, "RN-05",
                                        "El paciente ya tiene una cita el " + fechaHora))
                                : Mono.<Void>empty()));
    }

    private Cita nuevaCita(CitaRequest r) {
        LocalDateTime ahora = LocalDateTime.now();
        return Cita.builder()
                .codigo("CIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .pacienteId(r.pacienteId())
                .especialidad(r.especialidad())
                .fechaHora(r.fechaHora())
                .profesionalCodigo(r.profesionalCodigo())
                .motivo(r.motivo() == null || r.motivo().isBlank() ? null : r.motivo().trim())
                .estado(EstadoCita.PENDIENTE)
                .fechaCreacion(ahora)
                .build();
    }

    private Mono<Void> publicar(Cita cita, String tipo, String routingKey, EstadoCita anterior) {
        String detalle = "Cita " + cita.getCodigo() + " de " + cita.getEspecialidad() + " el " + cita.getFechaHora()
                + " (" + cita.getProfesionalCodigo() + "): " + cita.getEstado();
        return publisher.publicar(routingKey, EventoDominio.de(tipo, RECURSO, cita.getId(), cita.getPacienteId(),
                anterior == null ? null : anterior.name(), cita.getEstado().name(), detalle));
    }

    private static String tipoEvento(EstadoCita destino) {
        return switch (destino) {
            case CONFIRMADA -> "CitaConfirmada";
            case CANCELADA -> "CitaCancelada";
            case ATENDIDA -> "CitaAtendida";
            case AUSENTE -> "CitaAusente";
            case PENDIENTE -> "CitaPendiente";
        };
    }

    private static String routingKey(EstadoCita destino) {
        return switch (destino) {
            case CONFIRMADA -> RabbitConfig.RK_CITA_CONFIRMADA;
            case CANCELADA -> RabbitConfig.RK_CITA_CANCELADA;
            case ATENDIDA -> RabbitConfig.RK_CITA_ATENDIDA;
            case AUSENTE -> RabbitConfig.RK_CITA_AUSENTE;
            case PENDIENTE -> RabbitConfig.RK_CITA_REGISTRADA;
        };
    }
}
