package com.example.facturacion_pagos.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.facturacion_pagos.client.PacienteClient;
import com.example.facturacion_pagos.domain.Cargo;
import com.example.facturacion_pagos.domain.EstadoCargo;
import com.example.facturacion_pagos.dto.CargoRequest;
import com.example.facturacion_pagos.dto.CargoResponse;
import com.example.facturacion_pagos.exception.BusinessException;
import com.example.facturacion_pagos.messaging.EventoDominio;
import com.example.facturacion_pagos.messaging.EventoPublisher;
import com.example.facturacion_pagos.messaging.RabbitConfig;
import com.example.facturacion_pagos.repository.CargoRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CargoService {

    private static final String RECURSO = "CARGO";

    private final CargoRepository repository;
    private final PacienteClient pacienteClient;
    private final EventoPublisher publisher;

    /** Genera un cargo PENDIENTE para un paciente existente y ACTIVO. */
    public Mono<CargoResponse> registrar(CargoRequest request) {
        return Mono.fromRunnable(() -> PagoService.validarMonto(request.monto()))                 // RN-11
                .then(Mono.defer(() -> pacienteClient.obtenerPaciente(request.pacienteId())))
                .flatMap(paciente -> paciente.estaActivo()
                        ? repository.save(nuevo(request))
                        : Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-03",
                                "El paciente " + request.pacienteId() + " está INACTIVO")))
                .flatMap(c -> publicar(c, "CargoRegistrado", RabbitConfig.RK_CARGO_REGISTRADO, null).thenReturn(c))
                .map(CargoResponse::from);
    }

    public Mono<CargoResponse> anular(Long id) {
        return obtener(id)
                .flatMap(c -> {
                    if (c.getEstado() != EstadoCargo.PENDIENTE) {
                        return Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-11",
                                "Solo se anulan cargos PENDIENTES; el cargo " + c.getCodigo() + " está " + c.getEstado()));
                    }
                    c.setEstado(EstadoCargo.ANULADO);
                    c.setFechaActualizacion(LocalDateTime.now());
                    return repository.save(c);
                })
                .flatMap(c -> publicar(c, "CargoAnulado", RabbitConfig.RK_CARGO_ANULADO, EstadoCargo.PENDIENTE)
                        .thenReturn(c))
                .map(CargoResponse::from);
    }

    public Flux<CargoResponse> listar(Long pacienteId, EstadoCargo estado) {
        Flux<Cargo> cargos = pacienteId == null ? repository.findAll()
                : repository.findByPacienteIdOrderByFechaRegistroDesc(pacienteId);
        return cargos.filter(c -> estado == null || c.getEstado() == estado).map(CargoResponse::from);
    }

    public Mono<CargoResponse> buscarPorId(Long id) {
        return obtener(id).map(CargoResponse::from);
    }

    Mono<Cargo> obtener(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "CARGO_NO_ENCONTRADO",
                        "No existe un cargo con id " + id)));
    }

    private Cargo nuevo(CargoRequest r) {
        return Cargo.builder()
                .codigo("CAR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .pacienteId(r.pacienteId())
                .atencionId(r.atencionId())
                .concepto(r.concepto().trim())
                .monto(r.monto())
                .estado(EstadoCargo.PENDIENTE)
                .fechaRegistro(LocalDateTime.now())
                .build();
    }

    private Mono<Void> publicar(Cargo c, String tipo, String routingKey, EstadoCargo anterior) {
        String detalle = "Cargo " + c.getCodigo() + " por S/ " + c.getMonto() + " (" + c.getConcepto() + "): "
                + c.getEstado();
        return publisher.publicar(routingKey, EventoDominio.de(tipo, RECURSO, c.getId(), c.getPacienteId(),
                anterior == null ? null : anterior.name(), c.getEstado().name(), detalle));
    }
}
