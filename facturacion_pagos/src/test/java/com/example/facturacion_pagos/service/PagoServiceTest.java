package com.example.facturacion_pagos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.example.facturacion_pagos.domain.Cargo;
import com.example.facturacion_pagos.domain.EstadoCargo;
import com.example.facturacion_pagos.domain.MedioPago;
import com.example.facturacion_pagos.domain.Pago;
import com.example.facturacion_pagos.dto.PagoRequest;
import com.example.facturacion_pagos.exception.BusinessException;
import com.example.facturacion_pagos.messaging.EventoPublisher;
import com.example.facturacion_pagos.repository.CargoRepository;
import com.example.facturacion_pagos.repository.PagoRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** RN-11: validaciones del registro de pagos. */
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository repository;

    @Mock
    private CargoRepository cargoRepository;

    @Mock
    private CargoService cargoService;

    @Mock
    private TransactionalOperator transaccion;

    @Mock
    private EventoPublisher publisher;

    @InjectMocks
    private PagoService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void transaccionTransparente() {
        lenient().when(transaccion.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void rechazaMontoCero() {
        StepVerifier.create(service.registrar(request("0.00", "OP-000001")))
                .expectErrorSatisfies(ex -> {
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-11");
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .verify();
        verifyNoInteractions(repository, cargoRepository);
    }

    @Test
    void rechazaMontoNegativo() {
        StepVerifier.create(service.registrar(request("-50.00", "OP-000001")))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-11"))
                .verify();
        verifyNoInteractions(repository, cargoRepository);
    }

    @Test
    void rechazaNumeroDeOperacionDuplicado() {
        when(repository.existsByNumeroOperacion("OP-000001")).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(request("150.00", "OP-000001")))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .verify();
        verify(repository, never()).save(any());
    }

    @Test
    void noSePagaDosVecesElMismoCargo() {
        when(repository.existsByNumeroOperacion("OP-000002")).thenReturn(Mono.just(false));
        when(cargoService.obtener(7L)).thenReturn(Mono.just(cargo(EstadoCargo.PAGADO)));

        StepVerifier.create(service.registrar(request("150.00", "OP-000002")))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .verify();
        verify(repository, never()).save(any());
    }

    @Test
    void elPagoDebeCubrirElTotalDelCargo() {
        when(repository.existsByNumeroOperacion("OP-000003")).thenReturn(Mono.just(false));
        when(cargoService.obtener(7L)).thenReturn(Mono.just(cargo(EstadoCargo.PENDIENTE)));

        StepVerifier.create(service.registrar(request("100.00", "OP-000003")))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verify();
    }

    @Test
    void pagoValidoMarcaElCargoComoPagadoYPublicaElEvento() {
        Cargo pendiente = cargo(EstadoCargo.PENDIENTE);
        when(repository.existsByNumeroOperacion("OP-000004")).thenReturn(Mono.just(false));
        when(cargoService.obtener(7L)).thenReturn(Mono.just(pendiente));
        when(cargoRepository.save(any(Cargo.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(repository.save(any(Pago.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(request("150.00", "OP-000004")))
                .assertNext(p -> {
                    assertThat(p.pacienteId()).isEqualTo(1L);
                    assertThat(p.estado()).isEqualTo("REGISTRADO");
                    assertThat(p.concepto()).isEqualTo("Consulta externa (demo)");
                })
                .verifyComplete();

        assertThat(pendiente.getEstado()).isEqualTo(EstadoCargo.PAGADO);
        verify(publisher).publicar(eq("pago.registrado"), any());
    }

    private PagoRequest request(String monto, String operacion) {
        return new PagoRequest(7L, new BigDecimal(monto), MedioPago.TARJETA, operacion);
    }

    private Cargo cargo(EstadoCargo estado) {
        return Cargo.builder().id(7L).codigo("CAR-00000007").pacienteId(1L).concepto("Consulta externa (demo)")
                .monto(new BigDecimal("150.00")).estado(estado).fechaRegistro(LocalDateTime.now()).build();
    }
}
