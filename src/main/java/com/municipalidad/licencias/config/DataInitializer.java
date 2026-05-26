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
        // Limpieza via SQL directo para evitar problemas de cascada
        try {
            jdbc.execute("DELETE FROM notificacion WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1')");
            jdbc.execute("DELETE FROM multa WHERE licencia_id IN (SELECT id FROM licencia WHERE solicitud_id IN (SELECT id FROM solicitud WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1')))");
            jdbc.execute("DELETE FROM observacion WHERE solicitud_id IN (SELECT id FROM solicitud WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1'))");
            jdbc.execute("DELETE FROM inspeccion WHERE solicitud_id IN (SELECT id FROM solicitud WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1'))");
            jdbc.execute("DELETE FROM licencia WHERE solicitud_id IN (SELECT id FROM solicitud WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1'))");
            jdbc.execute("DELETE FROM solicitud WHERE usuario_id IN (SELECT id FROM usuario WHERE username = 'negocio1')");
            jdbc.execute("DELETE FROM usuario WHERE username = 'negocio1'");
            // Limpiar borradores
            jdbc.execute("DELETE FROM inspeccion WHERE solicitud_id IN (SELECT id FROM solicitud WHERE estado = 'BORRADOR')");
            jdbc.execute("DELETE FROM solicitud WHERE estado = 'BORRADOR'");
            // Eliminar distritos distintos a TRUJILLO
            jdbc.execute("DELETE FROM inspeccion WHERE solicitud_id IN (SELECT id FROM solicitud WHERE distrito != 'TRUJILLO')");
            jdbc.execute("DELETE FROM licencia WHERE solicitud_id IN (SELECT id FROM solicitud WHERE distrito != 'TRUJILLO')");
            jdbc.execute("DELETE FROM solicitud WHERE distrito != 'TRUJILLO'");
            jdbc.execute("DELETE FROM usuario WHERE distrito IS NOT NULL AND distrito != 'TRUJILLO'");
            log.info("Limpieza inicial completada.");
        } catch (Exception ex) { log.warn("Limpieza inicial: {}", ex.getMessage()); }

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
