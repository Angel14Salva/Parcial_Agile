package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.Licencia;
import com.municipalidad.licencias.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenciaRepository extends JpaRepository<Licencia, Long> {
    Optional<Licencia> findByNumeroLicencia(String numero);
    Optional<Licencia> findBySolicitud(Solicitud solicitud);
    List<Licencia> findByEstado(Enums.EstadoLicencia estado);

    @Query("SELECT l FROM Licencia l WHERE l.estado = 'VIGENTE' AND l.fechaVencimiento <= :fecha")
    List<Licencia> findLicenciasProximasAVencer(LocalDate fecha);
}
