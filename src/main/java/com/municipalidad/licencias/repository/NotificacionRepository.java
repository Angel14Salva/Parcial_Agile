package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.Notificacion;
import com.municipalidad.licencias.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioOrderByCreadoEnDesc(Usuario usuario);
    long countByUsuarioAndLeidaFalse(Usuario usuario);
}
