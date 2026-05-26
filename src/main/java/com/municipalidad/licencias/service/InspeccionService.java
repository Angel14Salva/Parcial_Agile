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

    @Value("${app.inspeccion.dias-habiles}")
    private int diasHabilesSegundaInspeccion;

    public InspeccionService(InspeccionRepository inspeccionRepo,
                             SolicitudRepository solicitudRepo,
                             ObservacionRepository observacionRepo,
                             UsuarioRepository usuarioRepo,
                             DiasHabilesUtil diasHabiles,
                             NotificacionService notificacionService) {
        this.inspeccionRepo      = inspeccionRepo;
        this.solicitudRepo       = solicitudRepo;
        this.observacionRepo     = observacionRepo;
        this.usuarioRepo         = usuarioRepo;
        this.diasHabiles         = diasHabiles;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public Inspeccion programarPrimeraInspeccion(Solicitud solicitud) {
        LocalDate fecha = diasHabiles.siguienteDiaHabil(LocalDate.now().plusDays(1));
        // Asignar inspector del mismo distrito, con menos inspecciones pendientes
        java.util.List<Usuario> candidatos = solicitud.getDistrito() != null
            ? usuarioRepo.findByRolAndDistrito(Enums.Rol.INSPECTOR, solicitud.getDistrito())
            : usuarioRepo.findByRol(Enums.Rol.INSPECTOR);
        if (candidatos.isEmpty()) candidatos = usuarioRepo.findByRol(Enums.Rol.INSPECTOR);
        Usuario inspector = candidatos.stream()
            .filter(Usuario::isActivo)
            .min(java.util.Comparator.comparingInt(u ->
                inspeccionRepo.findPendientesByInspector(u).size()))
            .orElseThrow(() -> new IllegalStateException("No hay inspectores disponibles."));

        Inspeccion inspeccion = Inspeccion.builder()
            .solicitud(solicitud).inspector(inspector)
            .tipo(Enums.TipoInspeccion.PRIMERA).fechaProgramada(fecha)
            .resultado(Enums.ResultadoInspeccion.PENDIENTE).build();

        solicitud.setEstado(Enums.EstadoTramite.INSPECCION_PROGRAMADA);
        solicitudRepo.save(solicitud);

        // Notificar al negocio
        notificacionService.crear(solicitud.getUsuario(),
            "Inspección técnica programada",
            "Tu solicitud fue admitida. Se programó una inspección técnica para el " +
            fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
            " con el inspector " + inspector.getNombreCompleto() + ".");

        return inspeccionRepo.save(inspeccion);
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
        LocalDate fecha = diasHabiles.sumarDiasHabiles(LocalDate.now(), diasHabilesSegundaInspeccion);
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

    public List<Inspeccion> obtenerPorSolicitud(Solicitud solicitud) {
        return inspeccionRepo.findBySolicitud(solicitud);
    }

    public Inspeccion obtenerPorId(Long id) {
        return inspeccionRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Inspección no encontrada: " + id));
    }
}
