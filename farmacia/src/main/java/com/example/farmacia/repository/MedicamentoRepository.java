package com.example.farmacia.repository;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.farmacia.domain.Medicamento;

import reactor.core.publisher.Mono;

public interface MedicamentoRepository extends ReactiveCrudRepository<Medicamento, Long> {

    Mono<Boolean> existsByCodigo(String codigo);

    /**
     * RN-10: descuenta el stock solo si alcanza, en una única sentencia. Devuelve 1 si se descontó y 0 si no
     * había stock suficiente. Al hacerlo en el UPDATE (y no leer-restar-guardar) dos dispensaciones
     * simultáneas no pueden dejar el stock en negativo.
     */
    @Modifying
    @Query("UPDATE medicamento SET stock = stock - :cantidad, fecha_actualizacion = :fecha "
            + "WHERE id = :id AND stock >= :cantidad")
    Mono<Integer> descontarStock(Long id, Integer cantidad, LocalDateTime fecha);

    @Modifying
    @Query("UPDATE medicamento SET stock = stock + :cantidad, fecha_actualizacion = :fecha WHERE id = :id")
    Mono<Integer> aumentarStock(Long id, Integer cantidad, LocalDateTime fecha);
}
