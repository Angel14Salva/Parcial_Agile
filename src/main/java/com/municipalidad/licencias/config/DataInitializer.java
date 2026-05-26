package com.municipalidad.licencias.config;

import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.Usuario;
import com.municipalidad.licencias.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder encoder;
    private final com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo;
    private final com.municipalidad.licencias.repository.InspeccionRepository inspeccionRepo;
    private final com.municipalidad.licencias.repository.LicenciaRepository licenciaRepo;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public DataInitializer(UsuarioRepository usuarioRepo, PasswordEncoder encoder,
                           com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo,
                           com.municipalidad.licencias.repository.InspeccionRepository inspeccionRepo,
                           com.municipalidad.licencias.repository.LicenciaRepository licenciaRepo,
                           org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.usuarioRepo = usuarioRepo;
        this.encoder = encoder;
        this.solicitudRepo = solicitudRepo;
        this.inspeccionRepo = inspeccionRepo;
        this.licenciaRepo = licenciaRepo;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // Eliminar usuario negocio1 via JdbcTemplate
        try {
            jdbc.execute("DELETE FROM notificacion WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1')");
            jdbc.execute("DELETE FROM observacion WHERE solicitud_id IN (SELECT id FROM solicitud WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1'))");
            jdbc.execute("DELETE FROM inspeccion WHERE solicitud_id IN (SELECT id FROM solicitud WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1'))");
            jdbc.execute("DELETE FROM licencia WHERE solicitud_id IN (SELECT id FROM solicitud WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1'))");
            jdbc.execute("DELETE FROM solicitud WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1')");
            jdbc.execute("DELETE FROM usuario WHERE username = 'negocio1'");
            log.info("Usuario negocio1 y todos sus datos eliminados.");
        } catch (Exception ex) { log.warn("No se pudo limpiar negocio1: {}", ex.getMessage()); }

        // Limpiar borradores huérfanos al arrancar
        solicitudRepo.findByEstado(com.municipalidad.licencias.model.Enums.EstadoTramite.BORRADOR)
            .forEach(solicitudRepo::delete);
        log.info("Borradores huérfanos eliminados al arrancar.");
        // Actualizar usernames a formato nombreapellidorol
        for (com.municipalidad.licencias.model.Usuario u : usuarioRepo.findAll()) {
            if (u.getRol() == com.municipalidad.licencias.model.Enums.Rol.INSPECTOR ||
                u.getRol() == com.municipalidad.licencias.model.Enums.Rol.FISCALIZADOR) {
                if (u.getNombreCompleto() == null || u.getNombreCompleto().isBlank()) continue;
                // Formato: APELLIDO NOMBRE -> nombreapellido
                String[] partes = u.getNombreCompleto().trim().split("[,\s]+");
                String nuevoUsername;
                if (partes.length >= 2) {
                    // Si es "APELLIDO, NOMBRE" o "APELLIDO NOMBRE"
                    String apellido = partes[0].toLowerCase().replaceAll("[^a-z0-9]", "");
                    String nombre = partes[partes.length - 1].toLowerCase().replaceAll("[^a-z0-9]", "");
                    String rol = u.getRol() == com.municipalidad.licencias.model.Enums.Rol.INSPECTOR
                        ? "inspector" : "fiscalizador";
                    nuevoUsername = nombre + apellido + rol;
                } else {
                    continue;
                }
                // Solo actualizar si el username actual es el formato viejo (f1.trujillo, i01.trujillo, etc)
                if (u.getUsername().matches("(f|i)\\d+\\..*") || u.getUsername().equals("inspector1")) {
                    u.setUsername(nuevoUsername);
                    usuarioRepo.save(u);
                    log.info("Username actualizado: {} -> {}", u.getNombreCompleto(), nuevoUsername);
                }
            }
        }
        // Eliminar todo lo que no sea distrito TRUJILLO
        for (com.municipalidad.licencias.model.Enums.Distrito d :
             com.municipalidad.licencias.model.Enums.Distrito.values()) {
            if (d != com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO) {
                // Eliminar solicitudes del distrito
                solicitudRepo.findByDistrito(d).forEach(solicitudRepo::delete);
                // Eliminar usuarios del distrito
                usuarioRepo.findByDistrito(d).forEach(usuarioRepo::delete);
            }
        }
        log.info("Datos de distritos distintos a TRUJILLO eliminados.");

        // Eliminar usuarios de distritos que ya no existen
        usuarioRepo.findAll().stream()
            .filter(u -> u.getDistrito() != null && u.getDistrito() != com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO)
            .forEach(usuarioRepo::delete);
        log.info("Usuarios de distritos eliminados.");
        // Reasignar roles: los i0x.trujillo pasan a FISCALIZADOR, los f0x.trujillo pasan a INSPECTOR
        usuarioRepo.findAll().stream()
            .filter(u -> u.getUsername() != null && u.getUsername().matches("i\\d+\\.trujillo"))
            .forEach(u -> { u.setRol(Enums.Rol.FISCALIZADOR); usuarioRepo.save(u); });
        usuarioRepo.findAll().stream()
            .filter(u -> u.getUsername() != null && u.getUsername().matches("f\\d+\\.trujillo"))
            .forEach(u -> { u.setRol(Enums.Rol.INSPECTOR); usuarioRepo.save(u); });
        log.info("Roles de funcionarios reasignados.");
        crearUsuarioSiNoExiste("admin",      "admin123",     "admin@municipalidad.gob.pe",
            "Administrador",    Enums.Rol.ADMIN);
        crearUsuarioSiNoExiste("inspector1", "inspector123", "inspector1@municipalidad.gob.pe",
            "Inspector García", Enums.Rol.INSPECTOR);

        crearUsuarioSiNoExiste("publico", java.util.UUID.randomUUID().toString(),
            "publico@licencias.gob.pe", "Ciudadano Público", Enums.Rol.NEGOCIO);

        log.info("=======================================================");
        log.info("  Usuarios demo: admin/admin123  inspector1/inspector123  negocio1/negocio123");
        log.info("=======================================================");
    }

    private void crearUsuarioSiNoExiste(String username, String password,
                                         String email, String nombre, Enums.Rol rol) {
        if (usuarioRepo.findByUsername(username).isEmpty()) {
            usuarioRepo.save(Usuario.builder()
                .username(username)
                .password(encoder.encode(password))
                .email(email)
                .nombreCompleto(nombre)
                .rol(rol)
                .activo(true)
                .build());
        }
    }
}
