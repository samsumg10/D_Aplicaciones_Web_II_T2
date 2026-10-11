package com.example.pacientes.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.pacientes.domain.EstadoPaciente;
import com.example.pacientes.domain.Paciente;
import com.example.pacientes.domain.TipoDocumento;
import com.example.pacientes.dto.PacienteRequest;
import com.example.pacientes.dto.PacienteResponse;
import com.example.pacientes.exception.BusinessException;
import com.example.pacientes.messaging.EventoDominio;
import com.example.pacientes.messaging.EventoPublisher;
import com.example.pacientes.messaging.RabbitConfig;
import com.example.pacientes.repository.PacienteRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private static final String RECURSO = "PACIENTE";

    private final PacienteRepository repository;
    private final EventoPublisher publisher;

    public Mono<PacienteResponse> registrar(PacienteRequest request) {
        return Mono.fromCallable(() -> {
                    validarFormatoDocumento(request.tipoDocumento(), request.numeroDocumento());   // RN-02
                    return nuevoPaciente(request);
                })
                .flatMap(paciente -> repository
                        .existsByTipoDocumentoAndNumeroDocumento(paciente.getTipoDocumento(), paciente.getNumeroDocumento())
                        .flatMap(existe -> existe
                                ? Mono.<Paciente>error(documentoDuplicado(request))                  // RN-01
                                : repository.save(paciente)))
                // Dos registros simultáneos pueden pasar el exists: el UNIQUE de la tabla es la última defensa
                .onErrorMap(DataIntegrityViolationException.class, e -> documentoDuplicado(request))
                .flatMap(guardado -> publisher.publicar(RabbitConfig.RK_PACIENTE_REGISTRADO,
                        EventoDominio.de("PacienteRegistrado", RECURSO, guardado.getId(), guardado.getId(),
                                null, guardado.getEstado().name(), "Paciente registrado"))
                        .thenReturn(guardado))
                .map(PacienteResponse::from);
    }

    public Flux<PacienteResponse> listar(EstadoPaciente estado) {
        Flux<Paciente> pacientes = estado == null ? repository.findAll() : repository.findByEstado(estado);
        return pacientes.map(PacienteResponse::from);
    }

    public Mono<PacienteResponse> buscarPorId(Long id) {
        return obtener(id).map(PacienteResponse::from);
    }

    public Mono<PacienteResponse> buscarPorDocumento(TipoDocumento tipo, String numero) {
        return repository.findByTipoDocumentoAndNumeroDocumento(tipo, numero.toUpperCase())
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "PACIENTE_NO_ENCONTRADO",
                        "No existe un paciente con documento " + tipo + " " + numero)))
                .map(PacienteResponse::from);
    }

    /** Actualiza los datos del paciente. Si cambia el documento, se vuelve a validar que no esté duplicado. */
    public Mono<PacienteResponse> actualizar(Long id, PacienteRequest request) {
        return obtener(id)
                .flatMap(actual -> {
                    validarFormatoDocumento(request.tipoDocumento(), request.numeroDocumento());   // RN-02
                    return repository.findByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(),
                                    request.numeroDocumento().toUpperCase())
                            .filter(otro -> !otro.getId().equals(id))
                            .flatMap(otro -> Mono.<Paciente>error(documentoDuplicado(request)))       // RN-01
                            .switchIfEmpty(Mono.defer(() -> {
                                copiarDatos(request, actual);
                                actual.setFechaActualizacion(LocalDateTime.now());
                                return repository.save(actual);
                            }));
                })
                .onErrorMap(DataIntegrityViolationException.class, e -> documentoDuplicado(request))
                .flatMap(guardado -> publisher.publicar(RabbitConfig.RK_PACIENTE_ACTUALIZADO,
                        EventoDominio.de("PacienteActualizado", RECURSO, guardado.getId(), guardado.getId(),
                                guardado.getEstado().name(), guardado.getEstado().name(), "Datos del paciente actualizados"))
                        .thenReturn(guardado))
                .map(PacienteResponse::from);
    }

    /** Activa o inactiva al paciente. Un paciente INACTIVO no puede recibir nuevas citas (lo valida citas). */
    public Mono<PacienteResponse> cambiarEstado(Long id, EstadoPaciente nuevoEstado) {
        return obtener(id)
                .flatMap(paciente -> {
                    EstadoPaciente anterior = paciente.getEstado();
                    if (anterior == nuevoEstado) {
                        return Mono.error(new BusinessException(HttpStatus.CONFLICT, "ESTADO_SIN_CAMBIO",
                                "El paciente ya se encuentra " + nuevoEstado));
                    }
                    paciente.setEstado(nuevoEstado);
                    paciente.setFechaActualizacion(LocalDateTime.now());
                    return repository.save(paciente)
                            .flatMap(guardado -> publisher.publicar(RabbitConfig.RK_PACIENTE_ESTADO_ACTUALIZADO,
                                    EventoDominio.de("PacienteEstadoActualizado", RECURSO, id, id, anterior.name(),
                                            nuevoEstado.name(), "Estado del paciente: " + anterior + " -> " + nuevoEstado))
                                    .thenReturn(guardado));
                })
                .map(PacienteResponse::from);
    }

    private Mono<Paciente> obtener(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "PACIENTE_NO_ENCONTRADO",
                        "No existe un paciente con id " + id)));
    }

    /** RN-02: el número debe corresponder al tipo de documento. */
    void validarFormatoDocumento(TipoDocumento tipo, String numero) {
        boolean valido = switch (tipo) {
            case DNI -> numero.matches("\\d{8}");
            case CE -> numero.matches("[A-Za-z0-9]{9,12}");
            case PASAPORTE -> numero.matches("[A-Za-z0-9]{6,12}");
        };
        if (!valido) {
            String formato = switch (tipo) {
                case DNI -> "8 dígitos";
                case CE -> "9 a 12 caracteres alfanuméricos";
                case PASAPORTE -> "6 a 12 caracteres alfanuméricos";
            };
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RN-02",
                    "El documento " + tipo + " debe tener " + formato);
        }
    }

    private Paciente nuevoPaciente(PacienteRequest request) {
        Paciente paciente = Paciente.builder()
                .estado(EstadoPaciente.ACTIVO)
                .fechaRegistro(LocalDateTime.now())
                .build();
        copiarDatos(request, paciente);
        return paciente;
    }

    private void copiarDatos(PacienteRequest r, Paciente p) {
        p.setTipoDocumento(r.tipoDocumento());
        p.setNumeroDocumento(r.numeroDocumento().toUpperCase());
        p.setNombres(r.nombres().trim());
        p.setApellidos(r.apellidos().trim());
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setTelefono(r.telefono());
        p.setCorreo(r.correo().trim().toLowerCase());
        p.setDireccion(r.direccion() == null || r.direccion().isBlank() ? null : r.direccion().trim());
    }

    private BusinessException documentoDuplicado(PacienteRequest r) {
        return new BusinessException(HttpStatus.CONFLICT, "RN-01",
                "Ya existe un paciente registrado con el documento " + r.tipoDocumento() + " " + r.numeroDocumento());
    }
}
