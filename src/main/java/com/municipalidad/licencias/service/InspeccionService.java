package com.municipalidad.licencias.service;

import com.municipalidad.licencias.dto.ObservacionDto;
import com.municipalidad.licencias.dto.ResultadoInspeccionDto;
import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.*;
import com.municipalidad.licencias.util.DiasHabilesUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InspeccionService {

    private final InspeccionRepository  inspeccionRepo;
    private final SolicitudRepository   solicitudRepo;
    private final ObservacionRepository observacionRepo;
    private final UsuarioRepository     usuarioRepo;
    private final DiasHabilesUtil       diasHabiles;
    private final NotificacionService   notificacionService;
    private final EmailService          emailService;

    @Value("${app.inspeccion.dias-habiles}")
    private int diasHabilesSegundaInspeccion;

    @Value("${app.inspeccion.capacidad-diaria:4}")
    private int capacidadDiariaInspector;

    // Días de anticipación mínima para la primera inspección (1 = "mañana como muy pronto").
    // En 0 para la demo, para que las inspecciones que se creen de ahora en adelante caigan hoy.
    @Value("${app.inspeccion.dias-anticipacion:1}")
    private int diasAnticipacionInspeccion;

    public InspeccionService(InspeccionRepository inspeccionRepo,
                             SolicitudRepository solicitudRepo,
                             ObservacionRepository observacionRepo,
                             UsuarioRepository usuarioRepo,
                             DiasHabilesUtil diasHabiles,
                             NotificacionService notificacionService,
                             EmailService emailService) {
        this.inspeccionRepo      = inspeccionRepo;
        this.solicitudRepo       = solicitudRepo;
        this.observacionRepo     = observacionRepo;
        this.usuarioRepo         = usuarioRepo;
        this.diasHabiles         = diasHabiles;
        this.notificacionService = notificacionService;
        this.emailService        = emailService;
    }

    public Inspeccion programarPrimeraInspeccion(Solicitud solicitud) {
        Usuario inspector = obtenerInspectorUnico();
        LocalDate fecha = buscarFechaDisponible(inspector, LocalDate.now().plusDays(diasAnticipacionInspeccion));

        Inspeccion inspeccion = Inspeccion.builder()
            .solicitud(solicitud).inspector(inspector)
            .tipo(Enums.TipoInspeccion.PRIMERA).fechaProgramada(fecha)
            .resultado(Enums.ResultadoInspeccion.PENDIENTE).build();

        solicitud.setEstado(Enums.EstadoTramite.INSPECCION_PROGRAMADA);
        solicitudRepo.save(solicitud);

        // Notificar al negocio (la notificación del día de la visita se envía
        // adicionalmente el mismo día mediante el job notificarInspeccionesDeHoy)
        notificacionService.crear(solicitud.getUsuario(),
            "Inspección técnica programada",
            "Tu solicitud fue admitida. Se programó una inspección técnica para el " +
            fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
            " con el inspector " + inspector.getNombreCompleto() + ".");

        return inspeccionRepo.save(inspeccion);
    }

    /**
     * En la municipalidad solo existe un inspector encargado de todas las
     * inspecciones técnicas (ITSE). Los fiscalizadores atienden fiscalización
     * de oficio / multas, no la agenda de inspecciones de trámites.
     */
    private Usuario obtenerInspectorUnico() {
        return usuarioRepo.findByRol(Enums.Rol.INSPECTOR).stream()
            .filter(Usuario::isActivo)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No hay un inspector activo configurado en el sistema."));
    }

    /**
     * Busca el día hábil más cercano (a partir de {@code desde}) en el que el
     * inspector todavía tiene cupo disponible, según su capacidad diaria
     * configurada en app.inspeccion.capacidad-diaria.
     */
    private LocalDate buscarFechaDisponible(Usuario inspector, LocalDate desde) {
        LocalDate fecha = diasHabiles.siguienteDiaHabil(desde);
        int intentos = 0;
        while (inspeccionRepo.contarProgramadasEnFecha(inspector, fecha) >= capacidadDiariaInspector
               && intentos < 60) {
            fecha = diasHabiles.siguienteDiaHabil(fecha.plusDays(1));
            intentos++;
        }
        return fecha;
    }

    @Transactional
    public Inspeccion registrarResultado(Long inspeccionId, ResultadoInspeccionDto dto) {
        Inspeccion inspeccion = obtenerPorId(inspeccionId);
        if (inspeccion.getResultado() != Enums.ResultadoInspeccion.PENDIENTE)
            throw new IllegalStateException("Esta inspección ya fue registrada.");

        inspeccion.setResultado(dto.getResultado());
        inspeccion.setFechaRealizada(LocalDateTime.now());
        inspeccion.setNotasInspector(dto.getNotas());
        Solicitud solicitud = inspeccion.getSolicitud();

        if (dto.getResultado() == Enums.ResultadoInspeccion.CONFORME) {
            solicitud.setEstado(Enums.EstadoTramite.APROBADO);
            solicitudRepo.save(solicitud);
            notificacionService.crear(solicitud.getUsuario(),
                "¡Inspección aprobada!",
                "Tu local fue inspeccionado y todo está conforme. " +
                "Ya puedes descargar tu licencia de funcionamiento.");
        } else {
            if (dto.getObservaciones() == null || dto.getObservaciones().isEmpty())
                throw new IllegalArgumentException("Debe registrar al menos una observación.");

            for (ObservacionDto obsDto : dto.getObservaciones()) {
                observacionRepo.save(Observacion.builder()
                    .inspeccion(inspeccion).tipo(obsDto.getTipo())
                    .descripcion(obsDto.getDescripcion()).build());
            }

            if (inspeccion.getTipo() == Enums.TipoInspeccion.PRIMERA) {
                solicitud.setEstado(Enums.EstadoTramite.OBSERVADO);
                solicitudRepo.save(solicitud);
                notificacionService.crear(solicitud.getUsuario(),
                    "Inspección con observaciones",
                    "Tu local fue inspeccionado y se encontraron " +
                    dto.getObservaciones().size() + " observación(es). " +
                    "Debes subsanarlas antes de la segunda inspección. " +
                    "Revisa el detalle de tu solicitud.");
                programarSegundaInspeccion(solicitud, inspeccion.getInspector());
            } else if (inspeccion.getTipo() == Enums.TipoInspeccion.SEGUNDA) {
                solicitud.setEstado(Enums.EstadoTramite.DENEGADO);
                solicitudRepo.save(solicitud);
                notificacionService.crear(solicitud.getUsuario(),
                    "Licencia denegada",
                    "Las observaciones de tu solicitud no fueron subsanadas. " +
                    "La licencia de funcionamiento ha sido denegada. " +
                    "Puedes iniciar un nuevo trámite.");
            }
        }
        return inspeccionRepo.save(inspeccion);
    }

    @Transactional
    public Inspeccion programarSegundaInspeccion(Solicitud solicitud, Usuario inspector) {
        LocalDate fechaBase = diasHabiles.sumarDiasHabiles(LocalDate.now(), diasHabilesSegundaInspeccion);
        LocalDate fecha = buscarFechaDisponible(inspector, fechaBase);
        Inspeccion segunda = Inspeccion.builder()
            .solicitud(solicitud).inspector(inspector)
            .tipo(Enums.TipoInspeccion.SEGUNDA).fechaProgramada(fecha)
            .resultado(Enums.ResultadoInspeccion.PENDIENTE).build();
        solicitud.setEstado(Enums.EstadoTramite.SEGUNDA_INSPECCION_PROGRAMADA);
        solicitudRepo.save(solicitud);

        notificacionService.crear(solicitud.getUsuario(),
            "Segunda inspección programada",
            "Se programó una segunda inspección para el " +
            fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
            ". Asegúrate de haber subsanado todas las observaciones antes de esa fecha.");

        return inspeccionRepo.save(segunda);
    }

    public List<Inspeccion> obtenerPendientesPorInspector(Usuario inspector) {
        return inspeccionRepo.findPendientesByInspector(inspector);
    }

    /**
     * RF: al ingresar, el inspector solo debe ver las inspecciones pendientes
     * del día actual. Conforme las va completando, desaparecen de la lista
     * (dejan de estar PENDIENTE) y las de otros días no se muestran.
     */
    public List<Inspeccion> obtenerPendientesHoyPorInspector(Usuario inspector) {
        return inspeccionRepo.findPendientesByInspectorYFecha(inspector, LocalDate.now());
    }

    /**
     * RF: el día en que corresponde una inspección (p.ej. viernes si fue
     * programada para un viernes), tanto el negocio como el inspector deben
     * recibir una notificación indicando que tienen una inspección ese día.
     * Corre todas las mañanas.
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void notificarInspeccionesDeHoy() {
        LocalDate hoy = LocalDate.now();
        List<Inspeccion> deHoy = inspeccionRepo.findByFechaProgramadaAndResultado(
            hoy, Enums.ResultadoInspeccion.PENDIENTE);
        String fechaTexto = hoy.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        for (Inspeccion i : deHoy) {
            Solicitud s = i.getSolicitud();

            // Notificar al negocio
            notificacionService.crear(s.getUsuario(),
                "Hoy tienes una inspección programada",
                "Recuerda: hoy " + fechaTexto + " se realizará la inspección técnica de tu local \"" +
                s.getNombreComercial() + "\" con el inspector " + i.getInspector().getNombreCompleto() + ".",
                "/solicitud/" + s.getId() + "/detalle");
            if (s.getCorreoElectronico() != null && !s.getCorreoElectronico().isBlank()) {
                emailService.enviarActualizacion(
                    s.getCorreoElectronico(), s.getRazonSocial(),
                    s.getCodigoSeguimiento() != null ? s.getCodigoSeguimiento() : "",
                    "Inspección programada para hoy",
                    "Hoy " + fechaTexto + " se realizará la inspección técnica de tu local.");
            }

            // Notificar al inspector
            notificacionService.crear(i.getInspector(),
                "Tienes una inspección programada para hoy",
                "Hoy " + fechaTexto + " debes visitar \"" + s.getNombreComercial() +
                "\" (" + s.getDireccionEstablecimiento() + ") — trámite " +
                (s.getCodigoSeguimiento() != null ? s.getCodigoSeguimiento() : s.getId()) + ".",
                "/inspector/inspeccion/" + i.getId());
            if (i.getInspector().getEmail() != null && !i.getInspector().getEmail().isBlank()) {
                emailService.enviarInspeccionHoyInspector(
                    i.getInspector().getEmail(), i.getInspector().getNombreCompleto(),
                    s.getNombreComercial(), s.getDireccionEstablecimiento(),
                    s.getCodigoSeguimiento() != null ? s.getCodigoSeguimiento() : ("Solicitud #" + s.getId()));
            }
        }
    }

    public List<Inspeccion> obtenerPorSolicitud(Solicitud solicitud) {
        return inspeccionRepo.findBySolicitud(solicitud);
    }

    public Inspeccion obtenerPorId(Long id) {
        return inspeccionRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Inspección no encontrada: " + id));
    }

    /**
     * Herramienta de prueba (solo admin): adelanta la fecha programada de una
     * inspección pendiente a hoy, para poder verificar sin esperar el flujo de
     * "inspecciones del día" (panel del inspector + notificarInspeccionesDeHoy).
     */
    @Transactional
    public void reprogramarParaHoy(Long inspeccionId) {
        Inspeccion i = obtenerPorId(inspeccionId);
        if (i.getResultado() != Enums.ResultadoInspeccion.PENDIENTE)
            throw new IllegalStateException("Esta inspección ya fue registrada.");
        i.setFechaProgramada(LocalDate.now());
        inspeccionRepo.save(i);

        // La fecha ya comunicada al negocio (correo/notificación de cuando se creó
        // la inspección) queda desactualizada tras este adelanto manual: se avisa
        // la nueva fecha para que no quede una discrepancia visible.
        Solicitud s = i.getSolicitud();
        String fechaTexto = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        notificacionService.crear(s.getUsuario(),
            "Se actualizó la fecha de tu inspección",
            "Tu inspección técnica fue reprogramada para hoy " + fechaTexto + ".",
            "/solicitud/" + s.getId() + "/detalle");
        if (s.getCorreoElectronico() != null && !s.getCorreoElectronico().isBlank()) {
            emailService.enviarActualizacion(
                s.getCorreoElectronico(), s.getRazonSocial(),
                s.getCodigoSeguimiento() != null ? s.getCodigoSeguimiento() : "",
                "Fecha de inspección actualizada",
                "Tu inspección técnica fue reprogramada para hoy " + fechaTexto + ".");
        }
    }
}
