package com.example.citas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.citas.client.PacienteClient;
import com.example.citas.client.PacienteDto;
import com.example.citas.domain.Cita;
import com.example.citas.domain.Especialidad;
import com.example.citas.domain.EstadoCita;
import com.example.citas.dto.CitaRequest;
import com.example.citas.dto.ReprogramacionRequest;
import com.example.citas.exception.BusinessException;
import com.example.citas.messaging.EventoDominio;
import com.example.citas.messaging.EventoPublisher;
import com.example.citas.repository.CitaRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    /** Próximo lunes a las 10:00: siempre futura y dentro de la agenda. */
    private static final LocalDateTime LUNES_10 = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10, 0);

    @Mock
    private CitaRepository repository;

    @Mock
    private PacienteClient pacienteClient;

    @Mock
    private EventoPublisher publisher;

    @InjectMocks
    private CitaService service;

    @Test
    void registraCitaParaPacienteActivoEnEstadoPendiente() {
        when(pacienteClient.obtenerPaciente(1L)).thenReturn(Mono.just(paciente("ACTIVO")));
        sinCitasEnEseHorario();
        when(repository.save(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setId(10L);
            return Mono.just(c);
        });
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(request(LUNES_10)))
                .assertNext(r -> {
                    assertThat(r.id()).isEqualTo(10L);
                    assertThat(r.estado()).isEqualTo(EstadoCita.PENDIENTE);
                    assertThat(r.codigo()).startsWith("CIT-");
                })
                .verifyComplete();

        verify(publisher).publicar(eq("cita.registrada"), any(EventoDominio.class));
    }

    @Test
    void noRegistraCitaSiElPacienteNoExiste() {
        when(pacienteClient.obtenerPaciente(1L)).thenReturn(Mono.error(new BusinessException(HttpStatus.NOT_FOUND,
                "PACIENTE_NO_ENCONTRADO", "No existe un paciente con id 1")));

        StepVerifier.create(service.registrar(request(LUNES_10)))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND))
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void noRegistraCitaParaPacienteInactivo() {
        when(pacienteClient.obtenerPaciente(1L)).thenReturn(Mono.just(paciente("INACTIVO")));

        StepVerifier.create(service.registrar(request(LUNES_10)))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-03"))
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void noRegistraCitaSiElProfesionalYaTieneEseHorario() {
        when(pacienteClient.obtenerPaciente(1L)).thenReturn(Mono.just(paciente("ACTIVO")));
        when(repository.findByProfesionalCodigoAndFechaHoraAndEstadoIn(eq("CAR-001"), eq(LUNES_10), any()))
                .thenReturn(Flux.just(cita(5L, EstadoCita.CONFIRMADA)));
        lenient().when(repository.findByPacienteIdAndFechaHoraAndEstadoIn(eq(1L), eq(LUNES_10), any()))
                .thenReturn(Flux.empty());

        StepVerifier.create(service.registrar(request(LUNES_10)))
                .expectErrorSatisfies(ex -> {
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-05");
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void rechazaHorarioFueraDeLaAgenda() {
        LocalDateTime domingo = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).atTime(10, 0);
        LocalDateTime temprano = LUNES_10.withHour(7);
        LocalDateTime fueraDeBloque = LUNES_10.withMinute(15);

        for (LocalDateTime fecha : new LocalDateTime[] { domingo, temprano, fueraDeBloque }) {
            StepVerifier.create(service.registrar(request(fecha)))
                    .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-04"))
                    .verify();
        }
        verify(repository, never()).save(any());
    }

    @Test
    void confirmarCitaPendientePublicaCitaConfirmada() {
        when(repository.findById(10L)).thenReturn(Mono.just(cita(10L, EstadoCita.PENDIENTE)));
        when(repository.save(any(Cita.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.cambiarEstado(10L, EstadoCita.CONFIRMADA))
                .assertNext(r -> assertThat(r.estado()).isEqualTo(EstadoCita.CONFIRMADA))
                .verifyComplete();

        ArgumentCaptor<EventoDominio> evento = ArgumentCaptor.forClass(EventoDominio.class);
        verify(publisher).publicar(eq("cita.confirmada"), evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo("CitaConfirmada");
        assertThat(evento.getValue().estadoAnterior()).isEqualTo("PENDIENTE");
        assertThat(evento.getValue().estadoNuevo()).isEqualTo("CONFIRMADA");
    }

    @Test
    void noPermiteConfirmarUnaCitaCancelada() {
        when(repository.findById(10L)).thenReturn(Mono.just(cita(10L, EstadoCita.CANCELADA)));

        StepVerifier.create(service.cambiarEstado(10L, EstadoCita.CONFIRMADA))
                .expectErrorSatisfies(ex -> {
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-06");
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();

        verify(repository, never()).save(any());
        verify(publisher, never()).publicar(anyString(), any());
    }

    @Test
    void cambiarEstadoDeCitaInexistenteDevuelveNoEncontrado() {
        when(repository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.cambiarEstado(99L, EstadoCita.CONFIRMADA))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND))
                .verify();
    }

    @Test
    void reprogramarCitaConfirmadaLaDevuelveAPendiente() {
        LocalDateTime nuevaFecha = LUNES_10.plusDays(1).withHour(11);
        when(repository.findById(10L)).thenReturn(Mono.just(cita(10L, EstadoCita.CONFIRMADA)));
        when(repository.findByProfesionalCodigoAndFechaHoraAndEstadoIn(eq("CAR-001"), eq(nuevaFecha), any()))
                .thenReturn(Flux.empty());
        when(repository.findByPacienteIdAndFechaHoraAndEstadoIn(eq(1L), eq(nuevaFecha), any())).thenReturn(Flux.empty());
        when(repository.save(any(Cita.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.reprogramar(10L, new ReprogramacionRequest(nuevaFecha, null)))
                .assertNext(r -> {
                    assertThat(r.estado()).isEqualTo(EstadoCita.PENDIENTE);
                    assertThat(r.fechaHora()).isEqualTo(nuevaFecha);
                })
                .verifyComplete();

        verify(publisher).publicar(eq("cita.reprogramada"), any(EventoDominio.class));
    }

    private void sinCitasEnEseHorario() {
        when(repository.findByProfesionalCodigoAndFechaHoraAndEstadoIn(eq("CAR-001"), eq(LUNES_10), any()))
                .thenReturn(Flux.empty());
        when(repository.findByPacienteIdAndFechaHoraAndEstadoIn(eq(1L), eq(LUNES_10), any())).thenReturn(Flux.empty());
    }

    private CitaRequest request(LocalDateTime fechaHora) {
        return new CitaRequest(1L, Especialidad.CARDIOLOGIA, fechaHora, "CAR-001", "Control administrativo");
    }

    private PacienteDto paciente(String estado) {
        return new PacienteDto(1L, "Ana", "Prueba Ficticia", estado);
    }

    private Cita cita(Long id, EstadoCita estado) {
        return Cita.builder().id(id).codigo("CIT-0000000" + id).pacienteId(1L).especialidad(Especialidad.CARDIOLOGIA)
                .fechaHora(LUNES_10).profesionalCodigo("CAR-001").estado(estado).fechaCreacion(LocalDateTime.now())
                .build();
    }
}
