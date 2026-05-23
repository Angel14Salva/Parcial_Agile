package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.Inspeccion;
import com.municipalidad.licencias.model.Observacion;
import com.municipalidad.licencias.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ObservacionRepository extends JpaRepository<Observacion, Long> {
    List<Observacion> findByInspeccion(Inspeccion inspeccion);

    @Query("SELECT o FROM Observacion o WHERE o.inspeccion.solicitud = :solicitud AND o.subsanada = false")
    List<Observacion> findObservacionesPendientes(Solicitud solicitud);
}
