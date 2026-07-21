package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.CajaSesion;
import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CajaSesionRepository extends JpaRepository<CajaSesion, Long> {

    Optional<CajaSesion> findFirstByCajeroAndEstadoInOrderByFechaAperturaDesc(
        Usuario cajero, List<Enums.EstadoSesionCaja> estados);

    List<CajaSesion> findByEstadoOrderByFechaAperturaAsc(Enums.EstadoSesionCaja estado);
}
