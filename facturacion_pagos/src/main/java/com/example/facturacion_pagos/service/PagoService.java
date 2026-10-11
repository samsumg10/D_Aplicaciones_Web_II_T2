package com.example.facturacion_pagos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.example.facturacion_pagos.domain.Cargo;
import com.example.facturacion_pagos.domain.EstadoCargo;
import com.example.facturacion_pagos.domain.Pago;
import com.example.facturacion_pagos.dto.PagoRequest;
import com.example.facturacion_pagos.dto.PagoResponse;
import com.example.facturacion_pagos.exception.BusinessException;
import com.example.facturacion_pagos.messaging.EventoDominio;
import com.example.facturacion_pagos.messaging.EventoPublisher;
import com.example.facturacion_pagos.messaging.RabbitConfig;
import com.example.facturacion_pagos.repository.CargoRepository;
import com.example.facturacion_pagos.repository.PagoRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PagoService {

    private static final String RECURSO = "PAGO";

    private final PagoRepository repository;
    private final CargoRepository cargoRepository;
    private final CargoService cargoService;
    private final TransactionalOperator transaccion;
    private final EventoPublisher publisher;

    /**
     * RN-11: monto mayor que 0, número de operación único, cargo PENDIENTE y pago por el total del cargo.
     * El pago y el cambio del cargo a PAGADO se guardan en una sola transacción.
     */
    public Mono<PagoResponse> registrar(PagoRequest request) {
        Mono<Pago> registrarPago = Mono.fromRunnable(() -> validarMonto(request.monto()))
                .then(Mono.defer(() -> repository.existsByNumeroOperacion(request.numeroOperacion())))
                .flatMap(existe -> existe ? Mono.<Cargo>error(operacionDuplicada(request.numeroOperacion()))
                        : cargoService.obtener(request.cargoId()))
                .flatMap(cargo -> validarCargo(cargo, request.monto()))
                .flatMap(cargo -> {
                    cargo.setEstado(EstadoCargo.PAGADO);
                    cargo.setFechaActualizacion(LocalDateTime.now());
                    return cargoRepository.save(cargo).then(repository.save(nuevoPago(request, cargo)));
                })
                // Dos pagos simultáneos con el mismo número pasan el exists: el UNIQUE los detiene
                .onErrorMap(DataIntegrityViolationException.class, e -> operacionDuplicada(request.numeroOperacion()));

        return transaccion.transactional(registrarPago)
                .flatMap(p -> publicar(p).thenReturn(p))
                .map(PagoResponse::from);
    }

    public Flux<PagoResponse> listar(Long pacienteId) {
        Flux<Pago> pagos = pacienteId == null ? repository.findAll() : repository.findByPacienteIdOrderByFechaPagoDesc(pacienteId);
        return pagos.map(PagoResponse::from);
    }

    public Mono<PagoResponse> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "PAGO_NO_ENCONTRADO",
                        "No existe un pago con id " + id)))
                .map(PagoResponse::from);
    }

    public Mono<PagoResponse> buscarPorOperacion(String numeroOperacion) {
        return repository.findByNumeroOperacion(numeroOperacion)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "PAGO_NO_ENCONTRADO",
                        "No existe un pago con número de operación " + numeroOperacion)))
                .map(PagoResponse::from);
    }

    /**
     * RN-11: el monto debe ser mayor que cero. La anotación @DecimalMin del request ya lo valida en la API;
     * se repite aquí para que la regla se cumpla aunque el servicio se invoque desde otro punto.
     */
    static void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RN-11", "El monto debe ser mayor que 0");
        }
    }

    private Mono<Cargo> validarCargo(Cargo cargo, BigDecimal monto) {
        if (cargo.getEstado() != EstadoCargo.PENDIENTE) {
            return Mono.error(new BusinessException(HttpStatus.CONFLICT, "RN-11",
                    "El cargo " + cargo.getCodigo() + " está " + cargo.getEstado() + " y no admite pagos"));
        }
        if (cargo.getMonto().compareTo(monto) != 0) {
            return Mono.error(new BusinessException(HttpStatus.BAD_REQUEST, "RN-11",
                    "El pago debe cubrir el total del cargo: S/ " + cargo.getMonto()));
        }
        return Mono.just(cargo);
    }

    private Pago nuevoPago(PagoRequest r, Cargo cargo) {
        return Pago.builder()
                .cargoId(cargo.getId())
                .pacienteId(cargo.getPacienteId())
                .concepto(cargo.getConcepto())
                .monto(r.monto())
                .medioPago(r.medioPago())
                .numeroOperacion(r.numeroOperacion())
                .estado(Pago.ESTADO_REGISTRADO)
                .fechaPago(LocalDateTime.now())
                .build();
    }

    private Mono<Void> publicar(Pago p) {
        String detalle = "Pago " + p.getNumeroOperacion() + " por S/ " + p.getMonto() + " con " + p.getMedioPago()
                + " (" + p.getConcepto() + ")";
        return publisher.publicar(RabbitConfig.RK_PAGO_REGISTRADO, EventoDominio.de("PagoRegistrado", RECURSO, p.getId(),
                p.getPacienteId(), null, p.getEstado(), detalle));
    }

    private static BusinessException operacionDuplicada(String numero) {
        return new BusinessException(HttpStatus.CONFLICT, "RN-11", "Ya existe un pago con el número de operación " + numero);
    }
}
