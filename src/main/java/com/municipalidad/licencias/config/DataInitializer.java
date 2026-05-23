package com.municipalidad.licencias.config;

import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.Usuario;
import com.municipalidad.licencias.repository.Repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        crearUsuarioSiNoExiste("admin",     "admin123",     "admin@municipalidad.gob.pe",
            "Administrador",  Enums.Rol.ADMIN);
        crearUsuarioSiNoExiste("inspector1","inspector123", "inspector1@municipalidad.gob.pe",
            "Inspector García", Enums.Rol.INSPECTOR);
        crearUsuarioSiNoExiste("negocio1",  "negocio123",   "negocio1@empresa.com",
            "Empresa Demo SAC", Enums.Rol.NEGOCIO);

        log.info("=======================================================");
        log.info("  Usuarios de demostración creados:");
        log.info("  admin      / admin123     (ADMIN)");
        log.info("  inspector1 / inspector123 (INSPECTOR)");
        log.info("  negocio1   / negocio123   (NEGOCIO)");
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
