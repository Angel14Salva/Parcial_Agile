package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.Solicitud;
import com.municipalidad.licencias.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByUsuario(Usuario usuario);
    List<Solicitud> findByEstado(Enums.EstadoTramite estado);

    @Query("SELECT s FROM Solicitud s WHERE s.estado NOT IN " +
           "('BORRADOR','DENEGADO','APROBADO') ORDER BY s.creadoEn ASC")
    List<Solicitud> findTramitesActivos();
    java.util.List<Solicitud> findByDistrito(com.municipalidad.licencias.model.Enums.Distrito distrito);
    java.util.Optional<Solicitud> findByCodigoSeguimientoAndDni(String codigo, String dni);
    java.util.Optional<Solicitud> findByCodigoSeguimiento(String codigo);
}
