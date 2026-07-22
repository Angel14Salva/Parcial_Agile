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

    @org.springframework.beans.factory.annotation.Value("${app.demo.reset-solicitudes:false}")
    private boolean resetSolicitudesDemo;

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

        // Hibernate genera un CHECK constraint con los valores del enum vigentes al crear la
        // columna, pero ddl-auto=update no lo actualiza cuando el enum cambia despues (ya paso
        // antes con usuarios_rol_check). Se elimina para que futuros valores del enum no rompan
        // los inserts; la validacion de valores queda a cargo de @Enumerated en la app.
        ejecutarSeguro("ALTER TABLE caja_sesiones DROP CONSTRAINT IF EXISTS caja_sesiones_estado_check");
        ejecutarSeguro("ALTER TABLE caja_sesiones ALTER COLUMN monto_apertura DROP NOT NULL");
        // Se agrego el valor MIXTO al enum MetodoPago (pagos divididos en varias partes).
        ejecutarSeguro("ALTER TABLE facturas_caja DROP CONSTRAINT IF EXISTS facturas_caja_metodo_pago_check");

        // Reinicio unico de datos para demo (activar con RESET_DEMO=true en Render).
        // Borra TODAS las solicitudes/inspecciones/licencias/observaciones/multas/facturas
        // de caja para empezar el flujo desde cero. No toca usuarios ni sesiones de caja
        // (apertura/cierre); "en caja ahora" volvera a mostrar solo el monto de apertura al
        // no quedar facturas que sumar. Volver a poner RESET_DEMO en false (o quitarla)
        // despues de que corra una vez, para que no se repita en cada redeploy.
        if (resetSolicitudesDemo) {
            log.warn("=== RESET_DEMO=true: eliminando todas las solicitudes/inspecciones/licencias/facturas existentes ===");
            ejecutarSeguro("DELETE FROM multas");
            ejecutarSeguro("DELETE FROM observaciones");
            ejecutarSeguro("DELETE FROM inspecciones");
            ejecutarSeguro("DELETE FROM licencias");
            ejecutarSeguro("DELETE FROM facturas_caja");
            ejecutarSeguro("DELETE FROM solicitudes");
            ejecutarSeguro("DELETE FROM notificaciones WHERE usuario_id IN " +
                "(SELECT id FROM usuarios WHERE username IN ('publico','inspector.garcia'))");
            log.warn("=== RESET_DEMO completado. Recuerda poner RESET_DEMO=false en Render. ===");
        }

        // crearUsuarioSiNoExiste solo crea si no existe; para actualizar el correo del
        // inspector ya creado en deploys anteriores hace falta este UPDATE puntual.
        ejecutarSeguro("UPDATE usuarios SET email = 'angellmauricio123@gmail.com' WHERE username = 'inspector.garcia'");

        // Unicas cuentas de staff permitidas: admin, inspector.garcia (+ 'publico', cuenta interna
        // del ciudadano virtual/presencial), mas CUALQUIER cajero (rol CAJERO) creado desde
        // /admin/cajeros. Cualquier otro usuario (de pruebas, deploys anteriores o creado
        // manualmente desde /admin/inspectores) se elimina en cada arranque para que la base de
        // datos nunca acumule cuentas de mas.
        final String NO_PERMITIDOS =
            "(SELECT id FROM usuarios WHERE username NOT IN ('admin','cajero1','inspector.garcia','publico') AND rol != 'CAJERO')";

        // Reasignar al inspector titular lo que apuntaba a un inspector no permitido, para no
        // perder el tramite/inspeccion en si al borrar la cuenta vieja.
        ejecutarSeguro("UPDATE inspecciones SET inspector_id = (SELECT id FROM usuarios WHERE username = 'inspector.garcia') " +
            "WHERE inspector_id IN " + NO_PERMITIDOS);
        ejecutarSeguro("UPDATE solicitudes SET inspector_id = (SELECT id FROM usuarios WHERE username = 'inspector.garcia') " +
            "WHERE inspector_id IN " + NO_PERMITIDOS);
        ejecutarSeguro("UPDATE solicitudes SET cajero_id = NULL WHERE cajero_id IN " + NO_PERMITIDOS);

        ejecutarSeguro("DELETE FROM multas WHERE inspector_id IN " + NO_PERMITIDOS);
        ejecutarSeguro("DELETE FROM notificaciones WHERE usuario_id IN " + NO_PERMITIDOS);
        ejecutarSeguro("DELETE FROM facturas_caja WHERE cajero_id IN " + NO_PERMITIDOS);
        ejecutarSeguro("DELETE FROM caja_sesiones WHERE cajero_id IN " + NO_PERMITIDOS + " OR revisado_por_id IN " + NO_PERMITIDOS);
        // Tramites que hayan quedado a nombre de una cuenta ciudadana distinta de 'publico'
        ejecutarSeguro("DELETE FROM multas WHERE licencia_id IN (SELECT id FROM licencias WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE usuario_id IN " + NO_PERMITIDOS + "))");
        ejecutarSeguro("DELETE FROM observaciones WHERE inspeccion_id IN (SELECT id FROM inspecciones WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE usuario_id IN " + NO_PERMITIDOS + "))");
        ejecutarSeguro("DELETE FROM inspecciones WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE usuario_id IN " + NO_PERMITIDOS + ")");
        ejecutarSeguro("DELETE FROM licencias WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE usuario_id IN " + NO_PERMITIDOS + ")");
        ejecutarSeguro("DELETE FROM solicitudes WHERE usuario_id IN " + NO_PERMITIDOS);
        ejecutarSeguro("DELETE FROM usuarios WHERE username NOT IN ('admin','cajero1','inspector.garcia','publico') AND rol != 'CAJERO'");
        // Limpiar borradores
        ejecutarSeguro("DELETE FROM inspecciones WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE estado = 'BORRADOR')");
        ejecutarSeguro("DELETE FROM solicitudes WHERE estado = 'BORRADOR'");
        // Eliminar distritos distintos a TRUJILLO
        ejecutarSeguro("DELETE FROM inspecciones WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE distrito != 'TRUJILLO')");
        ejecutarSeguro("DELETE FROM licencias WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE distrito != 'TRUJILLO')");
        ejecutarSeguro("DELETE FROM solicitudes WHERE distrito != 'TRUJILLO'");
        log.info("Limpieza inicial completada.");

                // Inspector (uno solo, encargado de todas las inspecciones ITSE)
        crearUsuarioSiNoExiste("inspector.garcia",  "insp1234", "angellmauricio123@gmail.com",  "GARCIA LOPEZ, CARLOS",   Enums.Rol.INSPECTOR,  com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO);
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
