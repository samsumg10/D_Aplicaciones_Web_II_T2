package com.example.hospitalizacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.hospitalizacion.client.PacienteClient;
import com.example.hospitalizacion.client.PacienteDto;
import com.example.hospitalizacion.domain.AreaHospitalizacion;
import com.example.hospitalizacion.domain.EstadoHospitalizacion;
import com.example.hospitalizacion.domain.Hospitalizacion;
import com.example.hospitalizacion.dto.HospitalizacionRequest;
import com.example.hospitalizacion.exception.BusinessException;
import com.example.hospitalizacion.messaging.EventoPublisher;
import com.example.hospitalizacion.repository.HospitalizacionRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class HospitalizacionServiceTest {

    @Mock
    private HospitalizacionRepository repository;

    @Mock
    private PacienteClient pacienteClient;

    @Mock
    private EventoPublisher publisher;

    @InjectMocks
    private HospitalizacionService service;

    @Test
    void solicitaHospitalizacionParaPacienteActivo() {
        when(pacienteClient.obtenerPaciente(1L)).thenReturn(Mono.just(new PacienteDto(1L, "Ana", "Prueba", "ACTIVO")));
        when(repository.existsByPacienteIdAndEstadoIn(eq(1L), any())).thenReturn(Mono.just(false));
        when(repository.save(any(Hospitalizacion.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.solicitar(new HospitalizacionRequest(1L, AreaHospitalizacion.CIRUGIA, null)))
                .assertNext(r -> {
                    assertThat(r.estado()).isEqualTo(EstadoHospitalizacion.SOLICITADA);
                    assertThat(r.camaCodigo()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void noAsignaUnaCamaOcupadaPorOtraHospitalizacionActiva() {
        when(repository.findById(2L)).thenReturn(Mono.just(hospitalizacion(EstadoHospitalizacion.SOLICITADA)));
        when(repository.existsByCamaOcupada("MED-101")).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrarIngreso(2L, "MED-101"))
                .expectErrorSatisfies(ex -> {
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-09");
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();
        verify(repository, never()).save(any());
    }

    @Test
    void ingresoConCamaLibreActivaLaHospitalizacion() {
        when(repository.findById(2L)).thenReturn(Mono.just(hospitalizacion(EstadoHospitalizacion.SOLICITADA)));
        when(repository.existsByCamaOcupada("MED-101")).thenReturn(Mono.just(false));
        when(repository.save(any(Hospitalizacion.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.registrarIngreso(2L, "MED-101"))
                .assertNext(r -> {
                    assertThat(r.estado()).isEqualTo(EstadoHospitalizacion.ACTIVA);
                    assertThat(r.camaCodigo()).isEqualTo("MED-101");
                    assertThat(r.fechaIngreso()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void altaLiberaLaCamaYPublicaElEvento() {
        Hospitalizacion activa = hospitalizacion(EstadoHospitalizacion.ACTIVA);
        activa.setCamaCodigo("MED-101");
        activa.setCamaOcupada("MED-101");
        when(repository.findById(2L)).thenReturn(Mono.just(activa));
        when(repository.save(any(Hospitalizacion.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.darDeAlta(2L))
                .assertNext(r -> assertThat(r.estado()).isEqualTo(EstadoHospitalizacion.ALTA))
                .verifyComplete();

        assertThat(activa.getCamaOcupada()).isNull();
        assertThat(activa.getCamaCodigo()).isEqualTo("MED-101");   // se conserva como histórico
        verify(publisher).publicar(eq("hospitalizacion.alta"), any());
    }

    @Test
    void noSeDaDeAltaUnaHospitalizacionSoloSolicitada() {
        when(repository.findById(2L)).thenReturn(Mono.just(hospitalizacion(EstadoHospitalizacion.SOLICITADA)));

        StepVerifier.create(service.darDeAlta(2L))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .verify();
    }

    private Hospitalizacion hospitalizacion(EstadoHospitalizacion estado) {
        return Hospitalizacion.builder().id(2L).codigo("HOS-00000002").pacienteId(1L)
                .area(AreaHospitalizacion.MEDICINA_INTERNA).estado(estado).fechaSolicitud(LocalDateTime.now()).build();
    }
}
