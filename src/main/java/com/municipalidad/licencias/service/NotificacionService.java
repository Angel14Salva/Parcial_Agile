package com.municipalidad.licencias.service;

import com.municipalidad.licencias.model.Notificacion;
import com.municipalidad.licencias.model.Usuario;
import com.municipalidad.licencias.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository repo;

    public NotificacionService(NotificacionRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void crear(Usuario usuario, String titulo, String mensaje) {
        crear(usuario, titulo, mensaje, null);
    }

    public void crear(Usuario usuario, String titulo, String mensaje, String enlace) {
        repo.save(Notificacion.builder()
            .usuario(usuario).titulo(titulo).mensaje(mensaje).enlace(enlace).build());
    }

    public List<Notificacion> obtenerPorUsuario(Usuario usuario) {
        return repo.findByUsuarioOrderByCreadoEnDesc(usuario);
    }

    public long contarNoLeidas(Usuario usuario) {
        return repo.countByUsuarioAndLeidaFalse(usuario);
    }

    @Transactional
    public void marcarTodasLeidas(Usuario usuario) {
        repo.findByUsuarioOrderByCreadoEnDesc(usuario)
            .forEach(n -> { n.setLeida(true); repo.save(n); });
    }
}
