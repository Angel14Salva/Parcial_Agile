package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    List<Usuario> findByRol(Enums.Rol rol);
}

@Repository
interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByUsuario(Usuario usuario);
    List<Solicitud> findByEstado(Enums.EstadoTramite estado);

    @Query("SELECT s FROM Solicitud s WHERE s.estado NOT IN " +
           "('BORRADOR','DENEGADO','APROBADO') ORDER BY s.creadoEn ASC")
    List<Solicitud> findTramitesActivos();
}

@Repository
interface InspeccionRepository extends JpaRepository<Inspeccion, Long> {
    List<Inspeccion> findBySolicitud(Solicitud solicitud);
    List<Inspeccion> findByInspector(Usuario inspector);
    List<Inspeccion> findByFechaProgramadaAndResultado(
        LocalDate fecha, Enums.ResultadoInspeccion resultado);

    @Query("SELECT i FROM Inspeccion i WHERE i.inspector = :inspector " +
           "AND i.resultado = 'PENDIENTE' ORDER BY i.fechaProgramada ASC")
    List<Inspeccion> findPendientesByInspector(Usuario inspector);
}

@Repository
interface ObservacionRepository extends JpaRepository<Observacion, Long> {
    List<Observacion> findByInspeccion(Inspeccion inspeccion);

    @Query("SELECT o FROM Observacion o WHERE o.inspeccion.solicitud = :solicitud " +
           "AND o.subsanada = false")
    List<Observacion> findObservacionesPendientes(Solicitud solicitud);
}

@Repository
interface LicenciaRepository extends JpaRepository<Licencia, Long> {
    Optional<Licencia> findByNumeroLicencia(String numero);
    Optional<Licencia> findBySolicitud(Solicitud solicitud);
    List<Licencia> findByEstado(Enums.EstadoLicencia estado);

    @Query("SELECT l FROM Licencia l WHERE l.estado = 'VIGENTE' " +
           "AND l.fechaVencimiento <= :fecha")
    List<Licencia> findLicenciasProximasAVencer(LocalDate fecha);
}

@Repository
interface RenovacionRepository extends JpaRepository<Renovacion, Long> {
    List<Renovacion> findByLicencia(Licencia licencia);
}
