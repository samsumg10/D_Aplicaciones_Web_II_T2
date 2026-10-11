package com.example.farmacia.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.example.farmacia.domain.Dispensacion;
import com.example.farmacia.domain.EstadoDispensacion;
import com.example.farmacia.domain.Medicamento;
import com.example.farmacia.dto.DispensacionRequest;
import com.example.farmacia.dto.DispensacionResponse;
import com.example.farmacia.exception.BusinessException;
import com.example.farmacia.messaging.EventoDominio;
import com.example.farmacia.messaging.EventoPublisher;
import com.example.farmacia.messaging.RabbitConfig;
import com.example.farmacia.repository.DispensacionRepository;
import com.example.farmacia.repository.MedicamentoRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DispensacionService {

    private static final String RECURSO = "DISPENSACION";

    private final DispensacionRepository repository;
    private final MedicamentoRepository medicamentoRepository;
    private final MedicamentoService medicamentoService;
    private final TransactionalOperator transaccion;
    private final EventoPublisher publisher;

    /**
     * RN-10: descuenta el stock y registra la dispensación en una misma transacción reactiva.
     * Si no hay stock suficiente no se guarda nada. El evento se publica después del commit.
     */
    public Mono<DispensacionResponse> registrar(DispensacionRequest request) {
        Mono<Dispensacion> descontarYGuardar = medicamentoService.obtener(request.medicamentoId())
                .flatMap(medicamento -> medicamentoRepository
                        .descontarStock(medicamento.getId(), request.cantidad(), LocalDateTime.now())
                        .flatMap(filas -> filas == 0
                                ? Mono.<Dispensacion>error(stockInsuficiente(medicamento, request.cantidad()))
                                : repository.save(nuevaDispensacion(request))));

        return transaccion.transactional(descontarYGuardar)
                .flatMap(d -> publicar(d, "DispensacionRegistrada", RabbitConfig.RK_DISPENSACION_REGISTRADA, null)
                        .thenReturn(d))
                .map(DispensacionResponse::from);
    }

    /** Anula la dispensación y devuelve la cantidad al stock, también en una sola transacción. */
    public Mono<DispensacionResponse> anular(Long id) {
        Mono<Dispensacion> anularYDevolver = obtener(id)
                .flatMap(d -> {
                    if (d.getEstado() != EstadoDispensacion.REGISTRADA) {
                        return Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-10",
                                "La dispensación " + d.getCodigo() + " ya está " + d.getEstado()));
                    }
                    LocalDateTime ahora = LocalDateTime.now();
                    d.setEstado(EstadoDispensacion.ANULADA);
                    d.setFechaActualizacion(ahora);
                    return medicamentoRepository.aumentarStock(d.getMedicamentoId(), d.getCantidad(), ahora)
                            .then(repository.save(d));
                });

        return transaccion.transactional(anularYDevolver)
                .flatMap(d -> publicar(d, "DispensacionAnulada", RabbitConfig.RK_DISPENSACION_ANULADA,
                        EstadoDispensacion.REGISTRADA).thenReturn(d))
                .map(DispensacionResponse::from);
    }

    public Flux<DispensacionResponse> listar(Long pacienteId) {
        Flux<Dispensacion> lista = pacienteId == null ? repository.findAll()
                : repository.findByPacienteIdOrderByFechaDispensacionDesc(pacienteId);
        return lista.map(DispensacionResponse::from);
    }

    public Mono<DispensacionResponse> buscarPorId(Long id) {
        return obtener(id).map(DispensacionResponse::from);
    }

    private Mono<Dispensacion> obtener(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "DISPENSACION_NO_ENCONTRADA",
                        "No existe una dispensación con id " + id)));
    }

    private Dispensacion nuevaDispensacion(DispensacionRequest r) {
        return Dispensacion.builder()
                .codigo("DIS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .medicamentoId(r.medicamentoId())
                .pacienteId(r.pacienteId())
                .cantidad(r.cantidad())
                .referenciaAdministrativa(r.referenciaAdministrativa().trim())
                .estado(EstadoDispensacion.REGISTRADA)
                .fechaDispensacion(LocalDateTime.now())
                .build();
    }

    private Mono<Void> publicar(Dispensacion d, String tipo, String routingKey, EstadoDispensacion anterior) {
        String detalle = "Dispensación " + d.getCodigo() + " de " + d.getCantidad() + " unidad(es) del medicamento "
                + d.getMedicamentoId() + " (ref. " + d.getReferenciaAdministrativa() + "): " + d.getEstado();
        return publisher.publicar(routingKey, EventoDominio.de(tipo, RECURSO, d.getId(), d.getPacienteId(),
                anterior == null ? null : anterior.name(), d.getEstado().name(), detalle));
    }

    private static BusinessException stockInsuficiente(Medicamento m, Integer cantidad) {
        return new BusinessException(HttpStatus.CONFLICT, "RN-10", "Stock insuficiente de " + m.getNombre()
                + ": se solicitaron " + cantidad + " y hay " + m.getStock());
    }
}
