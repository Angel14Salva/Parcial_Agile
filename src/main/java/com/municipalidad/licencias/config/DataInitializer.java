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

    public DataInitializer(UsuarioRepository usuarioRepo, PasswordEncoder encoder,
                           com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo) {
        this.usuarioRepo = usuarioRepo;
        this.encoder = encoder;
        this.solicitudRepo = solicitudRepo;
    }

    @Override
    public void run(String... args) {
        // Limpiar borradores huérfanos al arrancar
        solicitudRepo.findByEstado(com.municipalidad.licencias.model.Enums.EstadoTramite.BORRADOR)
            .forEach(solicitudRepo::delete);
        log.info("Borradores huérfanos eliminados al arrancar.");
        crearUsuarioSiNoExiste("admin",      "admin123",     "admin@municipalidad.gob.pe",
            "Administrador",    Enums.Rol.ADMIN);
        crearUsuarioSiNoExiste("inspector1", "inspector123", "inspector1@municipalidad.gob.pe",
            "Inspector García", Enums.Rol.INSPECTOR);
        crearUsuarioSiNoExiste("negocio1",   "negocio123",   "negocio1@empresa.com",
            "Empresa Demo SAC", Enums.Rol.NEGOCIO);
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
