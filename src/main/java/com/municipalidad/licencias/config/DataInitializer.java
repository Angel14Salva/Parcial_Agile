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



            // Reasignar registros de inspectores antiguos al inspector único antes de limpiarlos
            jdbc.execute("UPDATE inspeccion SET inspector_id = (SELECT id FROM usuario WHERE username = 'inspector.garcia') " +
                "WHERE inspector_id IN (SELECT id FROM usuario WHERE rol = 'INSPECTOR' AND username != 'inspector.garcia')");
            jdbc.execute("UPDATE solicitud SET inspector_id = (SELECT id FROM usuario WHERE username = 'inspector.garcia') " +
                "WHERE inspector_id IN (SELECT id FROM usuario WHERE rol = 'INSPECTOR' AND username != 'inspector.garcia')");
            // Eliminar usuarios viejos con formato nombreapellidorol
            jdbc.execute("DELETE FROM usuario WHERE rol IN ('INSPECTOR','FISCALIZADOR') AND username NOT IN ('inspector.garcia','fiscal.rios','fiscal.vargas','fiscal.herrera')");
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

                // Inspector (uno solo, encargado de todas las inspecciones ITSE)
        crearUsuarioSiNoExiste("inspector.garcia",  "insp1234", "inspector.garcia@municipalidad.gob.pe",  "GARCIA LOPEZ, CARLOS",   Enums.Rol.INSPECTOR,  com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
        // Fiscalizadores
        crearUsuarioSiNoExiste("fiscal.rios",    "fisc1234", "fiscal.rios@municipalidad.gob.pe",    "RIOS CASTILLO, MARIA",   Enums.Rol.FISCALIZADOR, com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
        crearUsuarioSiNoExiste("fiscal.vargas",  "fisc1234", "fiscal.vargas@municipalidad.gob.pe",  "VARGAS NUNEZ, LUIS",     Enums.Rol.FISCALIZADOR, com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
        crearUsuarioSiNoExiste("fiscal.herrera", "fisc1234", "fiscal.herrera@municipalidad.gob.pe", "HERRERA CAMPOS, ANA",    Enums.Rol.FISCALIZADOR, com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
        // Cajeros (atención presencial en ventanilla)
        crearUsuarioSiNoExiste("cajero1", "caja1234", "cajero1@municipalidad.gob.pe",
            "MEDINA ROJAS, PATRICIA", Enums.Rol.CAJERO, com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
        // Gerente municipal
        crearUsuarioSiNoExiste("gerente.municipal", "ger1234", "gerente@municipalidad.gob.pe",
            "RODRIGUEZ CASTILLO, CARLOS", Enums.Rol.GERENTE_MUNICIPAL, null);
        // Gerente distrital Trujillo
        crearUsuarioSiNoExiste("gerente.trujillo", "ger1234", "gerente.trujillo@municipalidad.gob.pe",
            "FLORES MEDINA, JORGE", Enums.Rol.GERENTE_DISTRITAL, com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
        // Subgerente Trujillo
        crearUsuarioSiNoExiste("sg.trujillo", "sub1234", "subgerente.trujillo@municipalidad.gob.pe",
            "PAREDES LOZANO, ANA MARIA", Enums.Rol.SUBGERENTE, com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
        crearUsuarioSiNoExiste("admin",      "admin123",     "admin@municipalidad.gob.pe",
            "Administrador",    Enums.Rol.ADMIN);

        crearUsuarioSiNoExiste("publico", java.util.UUID.randomUUID().toString(),
            "publico@licencias.gob.pe", "Ciudadano Público", Enums.Rol.NEGOCIO);

        log.info("=======================================================");
        log.info("  Usuarios demo: admin/admin123  inspector.garcia/insp1234  cajero1/caja1234");
        log.info("=======================================================");
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
