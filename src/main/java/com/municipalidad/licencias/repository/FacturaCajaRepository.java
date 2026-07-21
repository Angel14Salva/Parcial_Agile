package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.FacturaCaja;
import com.municipalidad.licencias.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FacturaCajaRepository extends JpaRepository<FacturaCaja, Long> {

    Optional<FacturaCaja> findByNumeroOperacion(String numeroOperacion);

    @Query("SELECT COALESCE(MAX(f.numero), 0) FROM FacturaCaja f")
    Long obtenerUltimoNumero();

    @Query("SELECT COALESCE(SUM(f.importeTotal), 0) FROM FacturaCaja f " +
        "WHERE f.cajero = :cajero AND f.metodoPago = :metodoPago AND f.estado = :estado " +
        "AND f.creadoEn BETWEEN :desde AND :hasta")
    BigDecimal sumarPorCajeroYMetodo(@Param("cajero") Usuario cajero,
                                      @Param("metodoPago") Enums.MetodoPago metodoPago,
                                      @Param("estado") Enums.EstadoFactura estado,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta);
}
