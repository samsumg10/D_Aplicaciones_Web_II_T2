package com.example.facturacion_pagos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.facturacion_pagos.client.PacienteClient;
import com.example.facturacion_pagos.client.PacienteDto;
import com.example.facturacion_pagos.domain.EstadoCargo;
import com.example.facturacion_pagos.domain.MedioPago;
import com.example.facturacion_pagos.dto.CargoRequest;
import com.example.facturacion_pagos.dto.PagoRequest;
import com.example.facturacion_pagos.exception.BusinessException;
import com.example.facturacion_pagos.messaging.EventoPublisher;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Cargo y pago con base de datos real (H2 vía R2DBC). Se reemplazan solo el cliente de pacientes y RabbitMQ. */
@SpringBootTest
@ActiveProfiles("test")
class PagoServiceIntegracionTest {

    @Autowired
    private CargoService cargoService;

    @Autowired
    private PagoService pagoService;

    @MockitoBean
    private PacienteClient pacienteClient;

    @MockitoBean
    private EventoPublisher publisher;

    @BeforeEach
    void dobles() {
        when(pacienteClient.obtenerPaciente(anyLong())).thenReturn(Mono.just(new PacienteDto(1L, "Ana", "Prueba", "ACTIVO")));
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());
    }

    @Test
    void pagarUnCargoLoDejaPagadoYNoAdmiteRepetirElNumeroDeOperacion() {
        Long cargoId = cargoService.registrar(new CargoRequest(1L, null, "Consulta externa (demo)", new BigDecimal("150.00")))
                .block().id();

        StepVerifier.create(pagoService.registrar(new PagoRequest(cargoId, new BigDecimal("150.00"), MedioPago.EFECTIVO,
                        "OP-INT-0001")))
                .assertNext(p -> assertThat(p.monto()).isEqualByComparingTo("150.00"))
                .verifyComplete();

        StepVerifier.create(cargoService.buscarPorId(cargoId))
                .assertNext(c -> assertThat(c.estado()).isEqualTo(EstadoCargo.PAGADO))
                .verifyComplete();

        Long otroCargo = cargoService.registrar(new CargoRequest(1L, null, "Laboratorio (demo)", new BigDecimal("40.00")))
                .block().id();
        StepVerifier.create(pagoService.registrar(new PagoRequest(otroCargo, new BigDecimal("40.00"), MedioPago.TARJETA,
                        "OP-INT-0001")))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .verify();

        StepVerifier.create(cargoService.buscarPorId(otroCargo))   // el segundo cargo sigue pendiente
                .assertNext(c -> assertThat(c.estado()).isEqualTo(EstadoCargo.PENDIENTE))
                .verifyComplete();
    }
}
