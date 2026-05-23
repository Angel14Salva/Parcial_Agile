package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.Inspeccion;
import com.municipalidad.licencias.model.Solicitud;
import com.municipalidad.licencias.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InspeccionRepository extends JpaRepository<Inspeccion, Long> {
    List<Inspeccion> findBySolicitud(Solicitud solicitud);
    List<Inspeccion> findByInspector(Usuario inspector);
    List<Inspeccion> findByFechaProgramadaAndResultado(LocalDate fecha, Enums.ResultadoInspeccion resultado);

    @Query("SELECT i FROM Inspeccion i WHERE i.inspector = :inspector " +
           "AND i.resultado = 'PENDIENTE' ORDER BY i.fechaProgramada ASC")
    List<Inspeccion> findPendientesByInspector(Usuario inspector);
}
