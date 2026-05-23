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
import java.util.List;

@Service
public class InspeccionService {

    private final InspeccionRepository  inspeccionRepo;
    private final SolicitudRepository   solicitudRepo;
    private final ObservacionRepository observacionRepo;
    private final UsuarioRepository     usuarioRepo;
    private final DiasHabilesUtil       diasHabiles;

    @Value("${app.inspeccion.dias-habiles}")
    private int diasHabilesSegundaInspeccion;

    public InspeccionService(InspeccionRepository inspeccionRepo,
                             SolicitudRepository solicitudRepo,
                             ObservacionRepository observacionRepo,
                             UsuarioRepository usuarioRepo,
                             DiasHabilesUtil diasHabiles) {
        this.inspeccionRepo  = inspeccionRepo;
        this.solicitudRepo   = solicitudRepo;
        this.observacionRepo = observacionRepo;
        this.usuarioRepo     = usuarioRepo;
        this.diasHabiles     = diasHabiles;
    }

    @Transactional
    public Inspeccion programarPrimeraInspeccion(Solicitud solicitud) {
        LocalDate fecha = diasHabiles.siguienteDiaHabil(LocalDate.now().plusDays(1));
        Usuario inspector = usuarioRepo.findByRol(Enums.Rol.INSPECTOR)
            .stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No hay inspectores registrados."));
        Inspeccion inspeccion = Inspeccion.builder()
            .solicitud(solicitud).inspector(inspector)
            .tipo(Enums.TipoInspeccion.PRIMERA).fechaProgramada(fecha)
            .resultado(Enums.ResultadoInspeccion.PENDIENTE).build();
        solicitud.setEstado(Enums.EstadoTramite.INSPECCION_PROGRAMADA);
        solicitudRepo.save(solicitud);
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
        } else {
            if (dto.getObservaciones() == null || dto.getObservaciones().isEmpty())
                throw new IllegalArgumentException("Debe registrar al menos una observación.");
            for (ObservacionDto obsDto : dto.getObservaciones()) {
                observacionRepo.save(Observacion.builder()
                    .inspeccion(inspeccion)
                    .tipo(obsDto.getTipo())
                    .descripcion(obsDto.getDescripcion())
                    .build());
            }
            if (inspeccion.getTipo() == Enums.TipoInspeccion.PRIMERA) {
                solicitud.setEstado(Enums.EstadoTramite.OBSERVADO);
                solicitudRepo.save(solicitud);
                programarSegundaInspeccion(solicitud, inspeccion.getInspector());
            } else if (inspeccion.getTipo() == Enums.TipoInspeccion.SEGUNDA) {
                solicitud.setEstado(Enums.EstadoTramite.DENEGADO);
                solicitudRepo.save(solicitud);
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
