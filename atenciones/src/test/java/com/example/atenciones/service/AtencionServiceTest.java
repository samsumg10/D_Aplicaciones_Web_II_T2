package com.example.atenciones.service;

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

import com.example.atenciones.client.CitaClient;
import com.example.atenciones.client.CitaDto;
import com.example.atenciones.domain.AreaAtencion;
import com.example.atenciones.domain.Atencion;
import com.example.atenciones.domain.EstadoAtencion;
import com.example.atenciones.dto.AtencionRequest;
import com.example.atenciones.exception.BusinessException;
import com.example.atenciones.messaging.EventoPublisher;
import com.example.atenciones.repository.AtencionRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AtencionServiceTest {

    @Mock
    private AtencionRepository repository;

    @Mock
    private CitaClient citaClient;

    @Mock
    private EventoPublisher publisher;

    @InjectMocks
    private AtencionService service;

    private final AtencionRequest request = new AtencionRequest(10L, AreaAtencion.CONSULTA_EXTERNA, "Ingreso por admisión");

    @Test
    void registraAtencionDeCitaConfirmadaYMarcaLaCitaComoAtendida() {
        when(repository.existsByCitaId(10L)).thenReturn(Mono.just(false));
        when(citaClient.obtenerCita(10L)).thenReturn(Mono.just(cita("CONFIRMADA")));
        when(repository.save(any(Atencion.class))).thenAnswer(inv -> {
            Atencion a = inv.getArgument(0);
            a.setId(5L);
            return Mono.just(a);
        });
        when(citaClient.marcarAtendida(10L)).thenReturn(Mono.just(cita("ATENDIDA")));
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(request))
                .assertNext(r -> {
                    assertThat(r.pacienteId()).isEqualTo(1L);   // se toma de la cita
                    assertThat(r.estado()).isEqualTo(EstadoAtencion.REGISTRADA);
                    assertThat(r.codigo()).startsWith("ATE-");
                })
                .verifyComplete();

        verify(publisher).publicar(eq("atencion.registrada"), any());
    }

    @Test
    void noRegistraAtencionSiLaCitaNoEstaConfirmada() {
        when(repository.existsByCitaId(10L)).thenReturn(Mono.just(false));
        when(citaClient.obtenerCita(10L)).thenReturn(Mono.just(cita("PENDIENTE")));

        StepVerifier.create(service.registrar(request))
                .expectErrorSatisfies(ex -> {
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-07");
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void noRegistraDosAtencionesParaLaMismaCita() {
        when(repository.existsByCitaId(10L)).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(request))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-07"))
                .verify();

        verify(citaClient, never()).obtenerCita(any());
    }

    @Test
    void siCitasRechazaElCambioSeEliminaLaAtencion() {
        when(repository.existsByCitaId(10L)).thenReturn(Mono.just(false));
        when(citaClient.obtenerCita(10L)).thenReturn(Mono.just(cita("CONFIRMADA")));
        when(repository.save(any(Atencion.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(citaClient.marcarAtendida(10L)).thenReturn(Mono.error(new BusinessException(HttpStatus.CONFLICT,
                "RN-07", "El servicio de citas no permitió marcar la cita 10 como ATENDIDA")));
        when(repository.delete(any(Atencion.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(request))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-07"))
                .verify();

        verify(repository).delete(any(Atencion.class));
        verify(publisher, never()).publicar(anyString(), any());
    }

    @Test
    void citaInexistenteDevuelveNoEncontrado() {
        when(repository.existsByCitaId(10L)).thenReturn(Mono.just(false));
        when(citaClient.obtenerCita(10L)).thenReturn(Mono.error(new BusinessException(HttpStatus.NOT_FOUND,
                "CITA_NO_ENCONTRADA", "No existe una cita con id 10")));

        StepVerifier.create(service.registrar(request))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND))
                .verify();
    }

    @Test
    void noSeReabreUnaAtencionAnulada() {
        when(repository.findById(5L)).thenReturn(Mono.just(Atencion.builder().id(5L).estado(EstadoAtencion.ANULADA).build()));

        StepVerifier.create(service.cambiarEstado(5L, EstadoAtencion.FINALIZADA))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .verify();
    }

    private CitaDto cita(String estado) {
        return new CitaDto(10L, "CIT-0000010", 1L, "CARDIOLOGIA", LocalDateTime.of(2030, 3, 4, 10, 0), estado);
    }
}
