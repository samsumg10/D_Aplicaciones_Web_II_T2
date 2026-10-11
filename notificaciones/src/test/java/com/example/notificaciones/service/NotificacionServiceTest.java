package com.example.notificaciones.service;

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

import com.example.notificaciones.domain.Notificacion;
import com.example.notificaciones.messaging.EventoDominio;
import com.example.notificaciones.repository.NotificacionRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Consumo del evento CitaConfirmada: qué aviso se genera y cómo se tratan las entregas duplicadas. */
@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository repository;

    @InjectMocks
    private NotificacionService service;

    @Test
    void citaConfirmadaGeneraUnAvisoSimulado() {
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            n.setId(1L);
            return Mono.just(n);
        });

        StepVerifier.create(service.procesar(evento("CitaConfirmada")))
                .assertNext(n -> assertThat(n.getId()).isEqualTo(1L))
                .verifyComplete();

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getAsunto()).isEqualTo("Confirmación de cita");
        assertThat(guardada.getValue().getPacienteId()).isEqualTo(1L);
        assertThat(guardada.getValue().getCanal()).isEqualTo("SIMULADO");
        assertThat(guardada.getValue().getMensaje()).contains("CIT-0000001");
    }

    @Test
    void unEventoRepetidoNoGeneraUnSegundoAviso() {
        when(repository.save(any(Notificacion.class)))
                .thenReturn(Mono.error(new DuplicateKeyException("uk_notificacion_evento")));

        StepVerifier.create(service.procesar(evento("CitaConfirmada"))).verifyComplete();
    }

    @Test
    void cadaTipoDeEventoTieneSuAsunto() {
        assertThat(NotificacionService.asunto("CitaCancelada")).isEqualTo("Cancelación de cita");
        assertThat(NotificacionService.asunto("PagoRegistrado")).isEqualTo("Pago registrado");
        assertThat(NotificacionService.asunto("HospitalizacionDadaDeAlta")).isEqualTo("Alta administrativa registrada");
        assertThat(NotificacionService.asunto("OtroEvento")).isEqualTo("Aviso del hospital");
    }

    private EventoDominio evento(String tipo) {
        return new EventoDominio(UUID.randomUUID().toString(), tipo, "citas", "CITA", "10", 1L, "PENDIENTE",
                "CONFIRMADA", "Cita CIT-0000001 de CARDIOLOGIA el 2030-03-04T10:00 (CAR-001): CONFIRMADA",
                LocalDateTime.now());
    }
}
