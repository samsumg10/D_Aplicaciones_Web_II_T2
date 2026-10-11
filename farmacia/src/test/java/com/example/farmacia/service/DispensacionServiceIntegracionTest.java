package com.example.farmacia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.farmacia.domain.EstadoDispensacion;
import com.example.farmacia.dto.DispensacionRequest;
import com.example.farmacia.dto.MedicamentoRequest;
import com.example.farmacia.exception.BusinessException;
import com.example.farmacia.messaging.EventoPublisher;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Flujo completo con base de datos real (H2 vía R2DBC) y transacciones reactivas.
 * Solo se reemplaza el publicador de eventos para no depender de RabbitMQ.
 */
@SpringBootTest
@ActiveProfiles("test")
class DispensacionServiceIntegracionTest {

    private static final AtomicLong SECUENCIA = new AtomicLong(100);

    @Autowired
    private MedicamentoService medicamentoService;

    @Autowired
    private DispensacionService dispensacionService;

    @MockitoBean
    private EventoPublisher publisher;

    private Long medicamentoId;

    @BeforeEach
    void crearMedicamentoConStock5() {
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());
        String codigo = String.format("MED-%04d", SECUENCIA.incrementAndGet());
        medicamentoId = medicamentoService.registrar(new MedicamentoRequest(codigo, "Producto de prueba", "Caja x 10", 5))
                .block().id();
    }

    @Test
    void dispensarDescuentaElStockYPublicaElEvento() {
        StepVerifier.create(dispensacionService.registrar(request(3)))
                .assertNext(d -> assertThat(d.estado()).isEqualTo(EstadoDispensacion.REGISTRADA))
                .verifyComplete();

        StepVerifier.create(medicamentoService.buscarPorId(medicamentoId))
                .assertNext(m -> assertThat(m.stock()).isEqualTo(2))
                .verifyComplete();
        verify(publisher).publicar(eq("farmacia.dispensacion-registrada"), any());
    }

    @Test
    void sinStockSuficienteNoSeDispensaNiCambiaElStock() {
        StepVerifier.create(dispensacionService.registrar(request(6)))
                .expectErrorSatisfies(ex -> {
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-10");
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();

        StepVerifier.create(medicamentoService.buscarPorId(medicamentoId))
                .assertNext(m -> assertThat(m.stock()).isEqualTo(5))
                .verifyComplete();
        verify(publisher, never()).publicar(anyString(), any());
    }

    @Test
    void anularDevuelveLaCantidadAlStock() {
        Long dispensacionId = dispensacionService.registrar(request(4)).block().id();

        StepVerifier.create(dispensacionService.anular(dispensacionId))
                .assertNext(d -> assertThat(d.estado()).isEqualTo(EstadoDispensacion.ANULADA))
                .verifyComplete();
        StepVerifier.create(medicamentoService.buscarPorId(medicamentoId))
                .assertNext(m -> assertThat(m.stock()).isEqualTo(5))
                .verifyComplete();

        StepVerifier.create(dispensacionService.anular(dispensacionId))   // no se anula dos veces
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-10"))
                .verify();
    }

    @Test
    void medicamentoInexistenteDevuelveNoEncontrado() {
        StepVerifier.create(dispensacionService.registrar(new DispensacionRequest(99999L, 1L, 1, "ATE-DEMO")))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND))
                .verify();
    }

    private DispensacionRequest request(int cantidad) {
        return new DispensacionRequest(medicamentoId, 1L, cantidad, "ATE-DEMO0001");
    }
}
