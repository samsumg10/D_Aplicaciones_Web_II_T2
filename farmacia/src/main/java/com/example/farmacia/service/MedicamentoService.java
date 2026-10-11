package com.example.farmacia.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.farmacia.domain.Medicamento;
import com.example.farmacia.dto.MedicamentoRequest;
import com.example.farmacia.dto.MedicamentoResponse;
import com.example.farmacia.exception.BusinessException;
import com.example.farmacia.repository.MedicamentoRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MedicamentoService {

    private final MedicamentoRepository repository;

    public Mono<MedicamentoResponse> registrar(MedicamentoRequest request) {
        return repository.existsByCodigo(request.codigo())
                .flatMap(existe -> existe ? Mono.<Medicamento>error(codigoDuplicado(request.codigo()))
                        : repository.save(Medicamento.builder()
                                .codigo(request.codigo())
                                .nombre(request.nombre().trim())
                                .presentacion(request.presentacion().trim())
                                .stock(request.stock())
                                .fechaRegistro(LocalDateTime.now())
                                .build()))
                .onErrorMap(DataIntegrityViolationException.class, e -> codigoDuplicado(request.codigo()))
                .map(MedicamentoResponse::from);
    }

    public Flux<MedicamentoResponse> listar() {
        return repository.findAll().map(MedicamentoResponse::from);
    }

    public Mono<MedicamentoResponse> buscarPorId(Long id) {
        return obtener(id).map(MedicamentoResponse::from);
    }

    public Mono<MedicamentoResponse> reponerStock(Long id, Integer cantidad) {
        return obtener(id)
                .flatMap(m -> repository.aumentarStock(id, cantidad, LocalDateTime.now()))
                .then(Mono.defer(() -> obtener(id)))
                .map(MedicamentoResponse::from);
    }

    Mono<Medicamento> obtener(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(HttpStatus.NOT_FOUND, "MEDICAMENTO_NO_ENCONTRADO",
                        "No existe un medicamento con id " + id)));
    }

    private static BusinessException codigoDuplicado(String codigo) {
        return new BusinessException(HttpStatus.CONFLICT, "RN-10", "Ya existe un medicamento con código " + codigo);
    }
}
