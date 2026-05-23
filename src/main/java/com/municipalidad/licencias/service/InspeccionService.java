package com.municipalidad.licencias.service;

import com.municipalidad.licencias.dto.ResultadoInspeccionDto;
import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.Repositories.*;
import com.municipalidad.licencias.util.DiasHabilesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspeccionService {

    private final InspeccionRepository inspeccionRepo;
    private final SolicitudRepository solicitudRepo;
    private final ObservacionRepository observacionRepo;
    private final UsuarioRepository usuarioRepo;
    private final DiasHabilesUtil diasHabiles;

    @Value("${app.inspeccion.dias-habiles}")
    private int diasHabilesSegundaInspeccion;

    // ── E2-US01: Programar 1.ª inspección automáticamente ────────────────────
    @Transactional
    public Inspeccion programarPrimeraInspeccion(Solicitud solicitud) {
        // Fecha más próxima = siguiente día hábil
        LocalDate fecha = diasHabiles.siguienteDiaHabil(LocalDate.now().plusDays(1));

        // Asignar inspector disponible (primer inspector activo del sistema)
        Usuario inspector = usuarioRepo.findByRol(Enums.Rol.INSPECTOR)
            .stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No hay inspectores registrados."));

        Inspeccion inspeccion = Inspeccion.builder()
            .solicitud(solicitud)
            .inspector(inspector)
            .tipo(Enums.TipoInspeccion.PRIMERA)
            .fechaProgramada(fecha)
            .resultado(Enums.ResultadoInspeccion.PENDIENTE)
            .build();

        solicitud.setEstado(Enums.EstadoTramite.INSPECCION_PROGRAMADA);
        solicitudRepo.save(solicitud);

        return inspeccionRepo.save(inspeccion);
    }

    // ── E2-US02: Registrar resultado de inspección ────────────────────────────
    @Transactional
    public Inspeccion registrarResultado(Long inspeccionId, ResultadoInspeccionDto dto) {
        Inspeccion inspeccion = obtenerPorId(inspeccionId);

        if (inspeccion.getResultado() != Enums.ResultadoInspeccion.PENDIENTE) {
            throw new IllegalStateException("Esta inspección ya fue registrada.");
        }

        inspeccion.setResultado(dto.getResultado());
        inspeccion.setFechaRealizada(LocalDateTime.now());
        inspeccion.setNotasInspector(dto.getNotas());

        Solicitud solicitud = inspeccion.getSolicitud();

        if (dto.getResultado() == Enums.ResultadoInspeccion.CONFORME) {
            // Aprobado → LicenciaService emitirá la licencia
            solicitud.setEstado(Enums.EstadoTramite.APROBADO);
            solicitudRepo.save(solicitud);

        } else {
            // Con observaciones
            if (dto.getObservaciones() == null || dto.getObservaciones().isEmpty()) {
                throw new IllegalArgumentException("Debe registrar al menos una observación.");
            }

            // Guardar observaciones
            for (var obsDto : dto.getObservaciones()) {
                Observacion obs = Observacion.builder()
                    .inspeccion(inspeccion)
                    .tipo(obsDto.getTipo())
                    .descripcion(obsDto.getDescripcion())
                    .build();
                observacionRepo.save(obs);
            }

            // Determinar si es 1.ª o 2.ª inspección
            if (inspeccion.getTipo() == Enums.TipoInspeccion.PRIMERA) {
                solicitud.setEstado(Enums.EstadoTramite.OBSERVADO);
                solicitudRepo.save(solicitud);
                // Programar 2.ª inspección (E3-US03)
                programarSegundaInspeccion(solicitud, inspeccion.getInspector());

            } else if (inspeccion.getTipo() == Enums.TipoInspeccion.SEGUNDA) {
                // 2.ª inspección con obs → licencia denegada (E3-US04)
                solicitud.setEstado(Enums.EstadoTramite.DENEGADO);
                solicitudRepo.save(solicitud);
            }
        }

        return inspeccionRepo.save(inspeccion);
    }

    // ── E3-US03: Programar 2.ª inspección (30 días hábiles) ──────────────────
    @Transactional
    public Inspeccion programarSegundaInspeccion(Solicitud solicitud, Usuario inspector) {
        LocalDate fecha = diasHabiles.sumarDiasHabiles(LocalDate.now(), diasHabilesSegundaInspeccion);

        Inspeccion segunda = Inspeccion.builder()
            .solicitud(solicitud)
            .inspector(inspector)
            .tipo(Enums.TipoInspeccion.SEGUNDA)
            .fechaProgramada(fecha)
            .resultado(Enums.ResultadoInspeccion.PENDIENTE)
            .build();

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
