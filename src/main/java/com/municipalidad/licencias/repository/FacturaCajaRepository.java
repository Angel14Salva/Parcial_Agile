package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.FacturaCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacturaCajaRepository extends JpaRepository<FacturaCaja, Long> {

    Optional<FacturaCaja> findByNumeroOperacion(String numeroOperacion);

    @Query("SELECT COALESCE(MAX(f.numero), 0) FROM FacturaCaja f")
    Long obtenerUltimoNumero();
}
