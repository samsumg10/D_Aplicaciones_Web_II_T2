package com.example.auditoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.example.auditoria.domain.RegistroAuditoria;
import com.example.auditoria.messaging.EventoDominio;
import com.example.auditoria.repository.RegistroAuditoriaRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private RegistroAuditoriaRepository repository;

    @InjectMocks
    private AuditoriaService service;

    @Test
    void registraFechaRecursoEstadosYOrigenDelCambio() {
        when(repository.save(any(RegistroAuditoria.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        LocalDateTime fecha = LocalDateTime.of(2030, 3, 1, 9, 0);

        StepVerifier.create(service.registrar(new EventoDominio(UUID.randomUUID().toString(), "CitaConfirmada", "citas",
                        "CITA", "10", 1L, "PENDIENTE", "CONFIRMADA", "detalle", fecha)))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<RegistroAuditoria> registro = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repository).save(registro.capture());
        assertThat(registro.getValue().getRecurso()).isEqualTo("CITA");
        assertThat(registro.getValue().getRecursoId()).isEqualTo("10");
        assertThat(registro.getValue().getEstadoAnterior()).isEqualTo("PENDIENTE");
        assertThat(registro.getValue().getEstadoNuevo()).isEqualTo("CONFIRMADA");
        assertThat(registro.getValue().getOrigen()).isEqualTo("citas");
        assertThat(registro.getValue().getFechaEvento()).isEqualTo(fecha);
    }

    @Test
    void unEventoRepetidoSeIgnora() {
        when(repository.save(any(RegistroAuditoria.class)))
                .thenReturn(Mono.error(new DuplicateKeyException("uk_auditoria_evento")));

        StepVerifier.create(service.registrar(new EventoDominio("e-1", "CitaConfirmada", "citas", "CITA", "10", 1L,
                        "PENDIENTE", "CONFIRMADA", "detalle", LocalDateTime.now())))
                .verifyComplete();
    }
}
