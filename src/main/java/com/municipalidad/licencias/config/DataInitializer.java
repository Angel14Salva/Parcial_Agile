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
        // Limpieza via SQL directo para evitar problemas de cascada.
        // Cada sentencia va en su propio try/catch para que un fallo puntual
        // no cancele el resto de la limpieza.
        ejecutarSeguro("UPDATE inspecciones SET inspector_id = (SELECT id FROM usuarios WHERE username = 'inspector.garcia') " +
            "WHERE inspector_id IN (SELECT id FROM usuarios WHERE rol = 'INSPECTOR' AND username != 'inspector.garcia')");
        ejecutarSeguro("UPDATE solicitudes SET inspector_id = (SELECT id FROM usuarios WHERE username = 'inspector.garcia') " +
            "WHERE inspector_id IN (SELECT id FROM usuarios WHERE rol = 'INSPECTOR' AND username != 'inspector.garcia')");
        // Eliminar usuarios viejos con formato nombreapellidorol
        ejecutarSeguro("DELETE FROM usuarios WHERE rol = 'INSPECTOR' AND username != 'inspector.garcia'");
        // Solo quedan 3 cuentas de staff: admin, cajero1, inspector.garcia (+ 'publico', cuenta interna
        // del ciudadano virtual). Se eliminan fiscalizadores, subgerente y gerentes por completo.
        ejecutarSeguro("DELETE FROM multas WHERE inspector_id IN (SELECT id FROM usuarios WHERE rol = 'FISCALIZADOR')");
        ejecutarSeguro("DELETE FROM notificaciones WHERE usuario_id IN (SELECT id FROM usuarios WHERE rol IN ('FISCALIZADOR','SUBGERENTE','GERENTE_DISTRITAL','GERENTE_MUNICIPAL'))");
        ejecutarSeguro("DELETE FROM usuarios WHERE rol IN ('FISCALIZADOR','SUBGERENTE','GERENTE_DISTRITAL','GERENTE_MUNICIPAL')");
        ejecutarSeguro("DELETE FROM notificaciones WHERE usuario_id IN (SELECT id FROM usuarios WHERE username = 'negocio1')");
        ejecutarSeguro("DELETE FROM multas WHERE licencia_id IN (SELECT id FROM licencias WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE usuario_id IN (SELECT id FROM usuarios WHERE username = 'negocio1')))");
        ejecutarSeguro("DELETE FROM observaciones WHERE inspeccion_id IN (SELECT id FROM inspecciones WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE usuario_id IN (SELECT id FROM usuarios WHERE username = 'negocio1')))");
        ejecutarSeguro("DELETE FROM inspecciones WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE usuario_id IN (SELECT id FROM usuarios WHERE username = 'negocio1'))");
        ejecutarSeguro("DELETE FROM licencias WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE usuario_id IN (SELECT id FROM usuarios WHERE username = 'negocio1'))");
        ejecutarSeguro("DELETE FROM solicitudes WHERE usuario_id IN (SELECT id FROM usuarios WHERE username = 'negocio1')");
        ejecutarSeguro("DELETE FROM usuarios WHERE username = 'negocio1'");
        // Limpiar borradores
        ejecutarSeguro("DELETE FROM inspecciones WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE estado = 'BORRADOR')");
        ejecutarSeguro("DELETE FROM solicitudes WHERE estado = 'BORRADOR'");
        // Eliminar distritos distintos a TRUJILLO
        ejecutarSeguro("DELETE FROM inspecciones WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE distrito != 'TRUJILLO')");
        ejecutarSeguro("DELETE FROM licencias WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE distrito != 'TRUJILLO')");
        ejecutarSeguro("DELETE FROM solicitudes WHERE distrito != 'TRUJILLO'");
        ejecutarSeguro("DELETE FROM usuarios WHERE distrito IS NOT NULL AND distrito != 'TRUJILLO'");
        log.info("Limpieza inicial completada.");

                // Inspector (uno solo, encargado de todas las inspecciones ITSE)
        crearUsuarioSiNoExiste("inspector.garcia",  "insp1234", "inspector.garcia@municipalidad.gob.pe",  "GARCIA LOPEZ, CARLOS",   Enums.Rol.INSPECTOR,  com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
        // Cajeros (atención presencial en ventanilla)
        crearUsuarioSiNoExiste("cajero1", "caja1234", "cajero1@municipalidad.gob.pe",
            "MEDINA ROJAS, PATRICIA", Enums.Rol.CAJERO, com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
        crearUsuarioSiNoExiste("admin",      "admin123",     "admin@municipalidad.gob.pe",
            "Administrador",    Enums.Rol.ADMIN);

        crearUsuarioSiNoExiste("publico", java.util.UUID.randomUUID().toString(),
            "publico@licencias.gob.pe", "Ciudadano Público", Enums.Rol.NEGOCIO);

        log.info("=======================================================");
        log.info("  Usuarios demo: admin/admin123  inspector.garcia/insp1234  cajero1/caja1234");
        log.info("=======================================================");
    }

    private void ejecutarSeguro(String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception ex) {
            log.warn("Limpieza inicial - sentencia omitida: {}", ex.getMessage());
        }
    }

    private void crearUsuarioSiNoExiste(String username, String password,
                                         String email, String nombre, Enums.Rol rol) {
        crearUsuarioSiNoExiste(username, password, email, nombre, rol, null);
    }

    private void crearUsuarioSiNoExiste(String username, String password,
                                         String email, String nombre, Enums.Rol rol,
                                         com.municipalidad.licencias.model.Enums.Distrito distrito) {
        if (usuarioRepo.findByUsername(username).isEmpty()) {
            Usuario u = Usuario.builder()
                .username(username)
                .password(encoder.encode(password))
                .email(email)
                .nombreCompleto(nombre)
                .rol(rol)
                .activo(true)
                .build();
            if (distrito != null) u.setDistrito(distrito);
            usuarioRepo.save(u);
        }
    }
}
