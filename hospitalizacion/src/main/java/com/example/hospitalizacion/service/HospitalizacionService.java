package com.example.hospitalizacion.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.hospitalizacion.client.PacienteClient;
import com.example.hospitalizacion.domain.EstadoHospitalizacion;
import com.example.hospitalizacion.domain.Hospitalizacion;
import com.example.hospitalizacion.dto.HospitalizacionRequest;
import com.example.hospitalizacion.dto.HospitalizacionResponse;
import com.example.hospitalizacion.exception.BusinessException;
import com.example.hospitalizacion.messaging.EventoDominio;
import com.example.hospitalizacion.messaging.EventoPublisher;
import com.example.hospitalizacion.messaging.RabbitConfig;
import com.example.hospitalizacion.repository.HospitalizacionRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class HospitalizacionService {

    private static final String RECURSO = "HOSPITALIZACION";

    private final HospitalizacionRepository repository;
    private final PacienteClient pacienteClient;
    private final EventoPublisher publisher;

    /** Registra la solicitud de hospitalización de un paciente ACTIVO que no tenga otra abierta. */
    public Mono<HospitalizacionResponse> solicitar(HospitalizacionRequest request) {
        return pacienteClient.obtenerPaciente(request.pacienteId())
                .flatMap(paciente -> paciente.estaActivo()
                        ? repository.existsByPacienteIdAndEstadoIn(request.pacienteId(), EstadoHospitalizacion.ABIERTAS)
                        : Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-03",
                                "El paciente " + request.pacienteId() + " está INACTIVO")))
                .flatMap(tieneAbierta -> tieneAbierta
                        ? Mono.<Hospitalizacion>error(new BusinessException(HttpStatus.CONFLICT, "RN-09",
                                "El paciente ya tiene una hospitalización SOLICITADA o ACTIVA"))
                        : repository.save(nueva(request)))
                .flatMap(h -> publicar(h, "HospitalizacionSolicitada", RabbitConfig.RK_SOLICITADA, null).thenReturn(h))
                .map(HospitalizacionResponse::from);
    }

    /** Ingreso: asigna una cama libre y la hospitalización pasa a ACTIVA (RN-09). */
    public Mono<HospitalizacionResponse> registrarIngreso(Long id, String camaCodigo) {
        return obtener(id)
                .flatMap(h -> validarTransicion(h, EstadoHospitalizacion.ACTIVA))
                .flatMap(h -> repository.existsByCamaOcupada(camaCodigo)
                        .flatMap(ocupada -> {
                            if (ocupada) {
                                return Mono.<Hospitalizacion>error(camaOcupada(camaCodigo));
                            }
                            LocalDateTime ahora = LocalDateTime.now();
                            h.setCamaCodigo(camaCodigo);
                            h.setCamaOcupada(camaCodigo);
                            h.setEstado(EstadoHospitalizacion.ACTIVA);
                            h.setFechaIngreso(ahora);
                            h.setFechaActualizacion(ahora);
                            return repository.save(h);
                        }))
                // Si otra asignación de la misma cama ganó la carrera, el UNIQUE de cama_ocupada la rechaza
                .onErrorMap(DataIntegrityViolationException.class, e -> camaOcupada(camaCodigo))
                .flatMap(h -> publicar(h, "HospitalizacionIniciada", RabbitConfig.RK_INGRESO,
                        EstadoHospitalizacion.SOLICITADA).thenReturn(h))
                .map(HospitalizacionResponse::from);
    }

    /** Alta administrativa simulada: libera la cama. */
    public Mono<HospitalizacionResponse> darDeAlta(Long id) {
        return obtener(id)
                .flatMap(h -> validarTransicion(h, EstadoHospitalizacion.ALTA))
                .flatMap(h -> {
                    LocalDateTime ahora = LocalDateTime.now();
                    h.setEstado(EstadoHospitalizacion.ALTA);
                    h.setCamaOcupada(null);
                    h.setFechaAlta(ahora);
                    h.setFechaActualizacion(ahora);
                    return repository.save(h);
                })
                .flatMap(h -> publicar(h, "HospitalizacionDadaDeAlta", RabbitConfig.RK_ALTA,
                        EstadoHospitalizacion.ACTIVA).thenReturn(h))
                .map(HospitalizacionResponse::from);
    }

    public Mono<HospitalizacionResponse> cancelar(Long id) {
        return obtener(id)
                .flatMap(h -> validarTransicion(h, EstadoHospitalizacion.CANCELADA))
                .flatMap(h -> {
                    h.setEstado(EstadoHospitalizacion.CANCELADA);
                    h.setFechaActualizacion(LocalDateTime.now());
                    return repository.save(h);
                })
                .flatMap(h -> publicar(h, "HospitalizacionCancelada", RabbitConfig.RK_CANCELADA,
                        EstadoHospitalizacion.SOLICITADA).thenReturn(h))
                .map(HospitalizacionResponse::from);
    }

    public Flux<HospitalizacionResponse> listar(Long pacienteId, EstadoHospitalizacion estado) {
        Flux<Hospitalizacion> lista = pacienteId != null ? repository.findByPacienteIdOrderByFechaSolicitudDesc(pacienteId)
                : estado != null ? repository.findByEstado(estado)
                : repository.findAll();
        return lista.filter(h -> estado == null || h.getEstado() == estado).map(HospitalizacionResponse::from);
    }

    public Mono<HospitalizacionResponse> buscarPorId(Long id) {
        return obtener(id).map(HospitalizacionResponse::from);
    }

    private Mono<Hospitalizacion> validarTransicion(Hospitalizacion h, EstadoHospitalizacion destino) {
        return h.getEstado().puedeCambiarA(destino) ? Mono.just(h)
                : Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-09",
                        "No se puede pasar una hospitalización " + h.getEstado() + " a " + destino));
    }

    private Mono<Hospitalizacion> obtener(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "HOSPITALIZACION_NO_ENCONTRADA",
                        "No existe una hospitalización con id " + id)));
    }

    private Hospitalizacion nueva(HospitalizacionRequest r) {
        return Hospitalizacion.builder()
                .codigo("HOS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .pacienteId(r.pacienteId())
                .area(r.area())
                .motivoAdministrativo(r.motivoAdministrativo() == null || r.motivoAdministrativo().isBlank() ? null
                        : r.motivoAdministrativo().trim())
                .estado(EstadoHospitalizacion.SOLICITADA)
                .fechaSolicitud(LocalDateTime.now())
                .build();
    }

    private Mono<Void> publicar(Hospitalizacion h, String tipo, String routingKey, EstadoHospitalizacion anterior) {
        String detalle = "Hospitalización " + h.getCodigo() + " en " + h.getArea()
                + (h.getCamaCodigo() != null ? " (cama " + h.getCamaCodigo() + ")" : "") + ": " + h.getEstado();
        return publisher.publicar(routingKey, EventoDominio.de(tipo, RECURSO, h.getId(), h.getPacienteId(),
                anterior == null ? null : anterior.name(), h.getEstado().name(), detalle));
    }

    private static BusinessException camaOcupada(String camaCodigo) {
        return new BusinessException(HttpStatus.CONFLICT, "RN-09",
                "La cama " + camaCodigo + " está asignada a otra hospitalización activa");
    }
}
