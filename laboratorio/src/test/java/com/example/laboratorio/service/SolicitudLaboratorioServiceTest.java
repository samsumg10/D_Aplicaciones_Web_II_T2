package com.example.laboratorio.service;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.laboratorio.client.AtencionClient;
import com.example.laboratorio.client.AtencionDto;
import com.example.laboratorio.domain.EstadoSolicitud;
import com.example.laboratorio.domain.SolicitudLaboratorio;
import com.example.laboratorio.domain.TipoExamen;
import com.example.laboratorio.dto.CambioEstadoSolicitudRequest;
import com.example.laboratorio.dto.SolicitudRequest;
import com.example.laboratorio.exception.BusinessException;
import com.example.laboratorio.messaging.EventoDominio;
import com.example.laboratorio.messaging.EventoPublisher;
import com.example.laboratorio.repository.SolicitudLaboratorioRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SolicitudLaboratorioServiceTest {

    @Mock
    private SolicitudLaboratorioRepository repository;

    @Mock
    private AtencionClient atencionClient;

    @Mock
    private EventoPublisher publisher;

    @InjectMocks
    private SolicitudLaboratorioService service;

    @Test
    void registraSolicitudParaUnaAtencionValida() {
        when(atencionClient.obtenerAtencion(5L)).thenReturn(Mono.just(atencion("REGISTRADA")));
        when(repository.existsByAtencionIdAndTipoExamenAndEstadoIn(eq(5L), eq(TipoExamen.HEMOGRAMA), any()))
                .thenReturn(Mono.just(false));
        when(repository.save(any(SolicitudLaboratorio.class))).thenAnswer(inv -> {
            SolicitudLaboratorio s = inv.getArgument(0);
            s.setId(3L);
            return Mono.just(s);
        });
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(new SolicitudRequest(5L, TipoExamen.HEMOGRAMA)))
                .assertNext(r -> {
                    assertThat(r.pacienteId()).isEqualTo(1L);
                    assertThat(r.estado()).isEqualTo(EstadoSolicitud.SOLICITADO);
                    assertThat(r.resultadoFicticio()).isNull();
                })
                .verifyComplete();
        verify(publisher).publicar(eq("laboratorio.solicitud-registrada"), any());
    }

    @Test
    void noAceptaSolicitudesDeUnaAtencionAnulada() {
        when(atencionClient.obtenerAtencion(5L)).thenReturn(Mono.just(atencion("ANULADA")));

        StepVerifier.create(service.registrar(new SolicitudRequest(5L, TipoExamen.HEMOGRAMA)))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-08"))
                .verify();
        verify(repository, never()).save(any());
    }

    @Test
    void completarExigeResultadoFicticio() {
        when(repository.findById(3L)).thenReturn(Mono.just(solicitud(EstadoSolicitud.EN_PROCESO)));

        StepVerifier.create(service.cambiarEstado(3L, new CambioEstadoSolicitudRequest(EstadoSolicitud.COMPLETADO, null)))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verify();
    }

    @Test
    void completarConResultadoPublicaSolicitudActualizada() {
        when(repository.findById(3L)).thenReturn(Mono.just(solicitud(EstadoSolicitud.EN_PROCESO)));
        when(repository.save(any(SolicitudLaboratorio.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.cambiarEstado(3L, new CambioEstadoSolicitudRequest(EstadoSolicitud.COMPLETADO,
                        "Resultado de demostración: dentro de rangos ficticios")))
                .assertNext(r -> {
                    assertThat(r.estado()).isEqualTo(EstadoSolicitud.COMPLETADO);
                    assertThat(r.resultadoFicticio()).startsWith("Resultado de demostración");
                })
                .verifyComplete();

        ArgumentCaptor<EventoDominio> evento = ArgumentCaptor.forClass(EventoDominio.class);
        verify(publisher).publicar(eq("laboratorio.estado-actualizado"), evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo("SolicitudLaboratorioActualizada");
        assertThat(evento.getValue().estadoAnterior()).isEqualTo("EN_PROCESO");
        assertThat(evento.getValue().detalle()).doesNotContain("Resultado");   // el resultado no viaja en el evento
    }

    @Test
    void noSePuedeCancelarUnaSolicitudCompletada() {
        when(repository.findById(3L)).thenReturn(Mono.just(solicitud(EstadoSolicitud.COMPLETADO)));

        StepVerifier.create(service.cambiarEstado(3L, new CambioEstadoSolicitudRequest(EstadoSolicitud.CANCELADO, null)))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .verify();
        verify(repository, never()).save(any());
    }

    private AtencionDto atencion(String estado) {
        return new AtencionDto(5L, "ATE-0000005", 1L, 10L, estado);
    }

    private SolicitudLaboratorio solicitud(EstadoSolicitud estado) {
        return SolicitudLaboratorio.builder().id(3L).codigo("LAB-00000003").pacienteId(1L).atencionId(5L)
                .tipoExamen(TipoExamen.HEMOGRAMA).estado(estado).fechaSolicitud(LocalDateTime.now()).build();
    }
}
