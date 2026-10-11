package com.example.pacientes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.pacientes.domain.EstadoPaciente;
import com.example.pacientes.domain.Paciente;
import com.example.pacientes.domain.TipoDocumento;
import com.example.pacientes.dto.PacienteRequest;
import com.example.pacientes.exception.BusinessException;
import com.example.pacientes.messaging.EventoDominio;
import com.example.pacientes.messaging.EventoPublisher;
import com.example.pacientes.repository.PacienteRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Reglas de negocio del registro de pacientes, sin base de datos ni RabbitMQ (dobles de prueba con Mockito). */
@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository repository;

    @Mock
    private EventoPublisher publisher;

    @InjectMocks
    private PacienteService service;

    @Test
    void registraPacienteValidoComoActivoYPublicaEvento() {
        when(repository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "70000001")).thenReturn(Mono.just(false));
        when(repository.save(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            p.setId(1L);
            return Mono.just(p);
        });
        when(publisher.publicar(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(request(TipoDocumento.DNI, "70000001")))
                .assertNext(r -> {
                    assertThat(r.id()).isEqualTo(1L);
                    assertThat(r.estado()).isEqualTo(EstadoPaciente.ACTIVO);
                    assertThat(r.correo()).isEqualTo("ana.prueba@correo.test");
                    assertThat(r.fechaRegistro()).isNotNull();
                })
                .verifyComplete();

        ArgumentCaptor<EventoDominio> evento = ArgumentCaptor.forClass(EventoDominio.class);
        verify(publisher).publicar(eq("paciente.registrado"), evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo("PacienteRegistrado");
        assertThat(evento.getValue().pacienteId()).isEqualTo(1L);
    }

    @Test
    void rechazaPacienteConDocumentoDuplicado() {
        when(repository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "70000001")).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(request(TipoDocumento.DNI, "70000001")))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-01");
                })
                .verify();

        verify(repository, never()).save(any());
        verify(publisher, never()).publicar(anyString(), any());
    }

    @Test
    void rechazaDniQueNoTieneOchoDigitos() {
        StepVerifier.create(service.registrar(request(TipoDocumento.DNI, "ABC12345")))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("RN-02"))
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void buscarPacienteInexistenteDevuelveNoEncontrado() {
        when(repository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.buscarPorId(99L))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND))
                .verify();
    }

    @Test
    void noPermiteCambiarAlMismoEstado() {
        Paciente activo = Paciente.builder().id(1L).estado(EstadoPaciente.ACTIVO).build();
        when(repository.findById(1L)).thenReturn(Mono.just(activo));

        StepVerifier.create(service.cambiarEstado(1L, EstadoPaciente.ACTIVO))
                .expectErrorSatisfies(ex -> assertThat(((BusinessException) ex).getCodigo()).isEqualTo("ESTADO_SIN_CAMBIO"))
                .verify();
    }

    private PacienteRequest request(TipoDocumento tipo, String numero) {
        return new PacienteRequest(tipo, numero, "Ana", "Prueba Ficticia", LocalDate.of(1990, 5, 20),
                "900000001", "Ana.Prueba@correo.test", null);
    }
}
